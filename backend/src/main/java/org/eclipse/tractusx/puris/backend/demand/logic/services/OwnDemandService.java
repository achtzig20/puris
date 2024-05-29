/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.demand.logic.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.repository.OwnDemandRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class OwnDemandService extends DemandService<OwnDemand, OwnDemandRepository> {
    public OwnDemandService(OwnDemandRepository repository, PartnerService partnerService, MaterialPartnerRelationService mprService) {
        super(repository, partnerService, mprService);
    }

    public final List<Double> getQuantityForDays(String material, String partnerBpnl, String siteBpns, int numberOfDays) {
        List<Double> quantities = new ArrayList<>();
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        for (int i = 0; i < numberOfDays; i++) {
            List<OwnDemand> demands = findAllByFilters(Optional.of(material), Optional.of(partnerBpnl), Optional.of(siteBpns), Optional.of(date));
            double demandQuantity = getSumOfQuantities(demands);
            quantities.add(demandQuantity);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
            date = calendar.getTime();
        }
        return quantities;
    }

    @Override
    public boolean validate(OwnDemand demand) {
        Partner ownPartnerEntity = partnerService.getOwnPartnerEntity();
        return 
            demand.getMaterial() != null &&
            demand.getPartner() != null &&
            mprService.partnerSuppliesMaterial(demand.getMaterial(), demand.getPartner()) &&
            demand.getQuantity() > 0 && 
            demand.getMeasurementUnit() != null && 
            demand.getDay() != null && 
            demand.getDemandCategoryCode() != null &&
            demand.getDemandLocationBpns() != null &&
            !demand.getPartner().equals(ownPartnerEntity) &&
            ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(demand.getDemandLocationBpns())) &&
            (demand.getSupplierLocationBpns() == null || demand.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(demand.getSupplierLocationBpns())));
    }
}
