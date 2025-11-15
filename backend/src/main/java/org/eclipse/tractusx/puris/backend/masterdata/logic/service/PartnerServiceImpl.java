/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcContractMappingService;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import javax.management.openmbean.KeyAlreadyExistsException;


@Service
@Slf4j
public class PartnerServiceImpl implements PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Autowired
    private EdcContractMappingService edcContractMappingService;

    private final Pattern bpnlPattern = PatternStore.BPNL_PATTERN;
    private final Pattern bpnsPattern = PatternStore.BPNS_PATTERN;
    private final Pattern bpnaPattern = PatternStore.BPNA_PATTERN;

    @Override
    public Partner create(Partner partner) {
        List<String> constraintErrors = testConstraints(partner);
        if (!constraintErrors.isEmpty()) {
            log.error("Could not create Partner " + partner.getBpnl() + " because of constraint violations: " + constraintErrors);
            throw new IllegalArgumentException("Partner violates constraints.");
        }
        if (partner.getUuid() != null) {
            log.error("Could not create Partner {} because UUID was provided for a new Partner", partner.getBpnl());
            throw new IllegalArgumentException("UUID must not be set when creating a new Partner.");
        }
        if (partnerRepository.findFirstByBpnl(partner.getBpnl()).isPresent()) {
            log.error("Could not create Partner {} because BPNL already exists", partner.getBpnl());
            throw new KeyAlreadyExistsException("Partner with given BPNL already exists.");
        }
        
        prepareApiAssetsForPartner(partner);
        return partnerRepository.save(partner);
    }

    @Override
    public void prepareApiAssetsForPartner(Partner partner) {
        if (!variablesService.getOwnBpnl().equals(partner.getBpnl())) {
            executorService.submit(new RegistrationTask(partner));
        }
    }

    @AllArgsConstructor
    private class RegistrationTask implements Callable<Boolean> {
        private Partner partner;

        @Override
        public Boolean call() throws Exception {
            if (edcAdapterService.createPolicyAndContractDefForPartner(partner)) {
                log.info("Policy / ContractDef Registration successful for partner " + partner.getBpnl());
                return true;
            } else {
                log.warn("Policy / ContractDef Registration failed for partner " + partner.getBpnl());
                log.warn("Retrying in 3 seconds");
                Thread.sleep(3000);
                if (edcAdapterService.createPolicyAndContractDefForPartner(partner)) {
                    log.info("Retry successful");
                    return true;
                }
                log.warn("Retry failed");
                return false;
            }
        }
    }


    private List<String> testConstraints(Partner partner) {
        List<String> errors = new ArrayList<>();
        int addressCount = 0;
        // Each Partner needs a BPNL, a name, an edcUrl and a BPNA or a BPNS (containing a BPNA)
        if (partner.getBpnl() == null || !bpnlPattern.matcher(partner.getBpnl()).matches()) {
            String error = "Invalid BPNL: " + partner.getBpnl();
            log.error(error);
            errors.add(error);
        }
        
        //EDC URL validation
        try {
            new URL(partner.getEdcUrl()).toURI();
        } catch (Exception e) {
            String error = "Invalid BPNL: " + partner.getBpnl();
            log.error(error);
            errors.add(error);
        }

        boolean nameExists = partner.getName() != null && !partner.getName().isEmpty();
        if (!nameExists) {
            String error = "Missing name of partner";
            log.error(error);
            errors.add(error);
        }

        for (var site : partner.getSites()) {
            boolean validBpns = bpnsPattern.matcher(site.getBpns()).matches();
            if (!validBpns) {
                String error = "Invalid BPNS: " + site.getBpns();
                log.error(error);
                errors.add(error);
            }

            if (site.getAddresses().isEmpty()) {
                String error = "Site " + site.getBpns() + " has no addresses";
                log.error(error);
                errors.add(error);
            }
            for (var address : site.getAddresses()) {
                boolean validBpna = bpnaPattern.matcher(address.getBpna()).matches();
                if (!validBpna) {
                    String error = "Invalid BPNA on site " + site.getBpns() + ": " + address.getBpna();
                    log.error(error);
                    errors.add(error);
                }
                addressCount++;
            }
            
        }
        // Partner addresses
        for (var address : partner.getAddresses()) {
            boolean validBpna = bpnaPattern.matcher(address.getBpna()).matches();
            if (!validBpna) {
                String error = "Invalid BPNA on partner: " + address.getBpna();
                log.error(error);
                errors.add(error);
            }
            addressCount++;
        }
        if (addressCount < 1) {
            String error = "No BPNA given for Partner " + partner.getBpnl();
            log.error(error);
            errors.add(error);
        }
        return errors;
    }

    @Override
    public Partner findByUuid(UUID partnerUuid) {
        return partnerRepository.findById(partnerUuid).orElse(null);
    }

    @Override
    public List<Partner> findAllCustomerPartnersForMaterialId(String ownMaterialNumber) {
        return mprService.findAllCustomersForOwnMaterialNumber(ownMaterialNumber);
    }

    @Override
    public List<Partner> findAllSupplierPartnersForMaterialId(String ownMaterialNumber) {
        return mprService.findAllSuppliersForOwnMaterialNumber(ownMaterialNumber);
    }

    @Override
    public List<Partner> findAll() {
        return partnerRepository.findAll();
    }

    @Override
    public Partner update(Partner partner) {
        List<String> constraintErrors = testConstraints(partner);
        if (!constraintErrors.isEmpty()) {
            log.error("Could not update Partner " + partner.getBpnl() + " because of constraint violations: " + constraintErrors);
            throw new IllegalArgumentException("Partner violates constraints.");
        }
        if (partner.getUuid() == null) {
            log.error("Could not update Partner {} because UUID is missing", partner.getBpnl());
            throw new IllegalArgumentException("UUID must be set when updating a Partner.");
        }

        Optional<Partner> existingPartnerOpt = partnerRepository.findById(partner.getUuid());
        if (existingPartnerOpt.isEmpty()) {
            log.error("Could not update Partner {} because it didn't exist before", partner.getBpnl());
            throw new IllegalStateException("Partner does not exist.");
        }

        Partner existingPartner = existingPartnerOpt.get();
        if (!existingPartner.getBpnl().equals(partner.getBpnl())) {
            log.error(
                "Could not update Partner {} because BPNL change is not allowed (existing: {}, new: {})",
                partner.getBpnl(), existingPartner.getBpnl(), partner.getBpnl()
            );
            throw new IllegalArgumentException("BPNL cannot be changed for an existing Partner.");
        }
        return partnerRepository.save(partner);
    }

    @Override
    public Partner findByBpnl(String bpnl) {
        return partnerRepository.findFirstByBpnl(bpnl).orElse(null);
    }

    @Override
    public Partner findByBpns(String bpns) {
        return partnerRepository.findFirstBySites_Bpns(bpns).orElse(null);
    }

    @Override
    public Partner getOwnPartnerEntity() {
        return partnerRepository.findFirstByBpnl(variablesService.getOwnBpnl()).orElse(null);
    }
}
