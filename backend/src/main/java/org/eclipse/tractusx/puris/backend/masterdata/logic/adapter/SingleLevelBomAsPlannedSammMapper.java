/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ChildData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ItemQuantity;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlannedSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for mapping Material BOM structure to SingleLevelBomAsPlanned SAMM model.
 */
@Service
@Slf4j
public class SingleLevelBomAsPlannedSammMapper {

    @Autowired
    private MaterialRelationService materialRelationService;

    @Autowired
    private MaterialPartnerRelationService materialPartnerRelationService;

    @Autowired
    private MaterialService materialService;

    /**
     * Convert a Material's BOM structure to a SingleLevelBomAsPlanned SAMM for a specific partner.
     *
     * @param partner  the partner for whom to generate the BOM
     * @param material the parent material whose BOM structure to convert
     * @return         the SAMM representation of the single-level BOM
     */
    public SingleLevelBomAsPlannedSAMM materialToSamm(Material material) {

        SingleLevelBomAsPlannedSAMM samm = new SingleLevelBomAsPlannedSAMM();
        samm.setCatenaXId(material.getMaterialNumberCx());
        samm.setChildItems(new HashSet<>());

        Set<ChildData> childItems = new HashSet<>();
        
        // Find all MaterialRelations where this material is the parent
        List<MaterialRelation> childRelations = materialRelationService.findAll().stream()
            .filter(rel -> rel.getParentMaterialNumber().equals(material.getOwnMaterialNumber()))
            .collect(Collectors.toList());
        
        // For each child material, find its supplier(s) and create ChildData
        for (MaterialRelation materialRelation : childRelations) {
            String childMaterialNumber = materialRelation.getChildMaterialNumber();
            Material childMaterial = materialService.findByOwnMaterialNumber(childMaterialNumber);
            
            if (childMaterial == null) {
                log.warn("Child material {} not found in database, skipping", childMaterialNumber);
                continue;
            }
            
            // Find all suppliers of this child material
            List<Partner> suppliers = materialPartnerRelationService.findAllSuppliersForMaterial(childMaterial);
            
            if (suppliers.isEmpty()) {
                log.warn("No supplier found for child material {}, skipping", childMaterialNumber);
                continue;
            }
            
            // Create ChildData for each supplier of this component
            for (Partner supplier : suppliers) {
                MaterialPartnerRelation mpr = materialPartnerRelationService.find(childMaterial, supplier);
                
                if (mpr == null || !mpr.isPartnerSuppliesMaterial()) {
                    log.warn("MaterialPartnerRelation not found or partner not marked as supplier for {} and {}", 
                        childMaterialNumber, supplier.getBpnl());
                    continue;
                }
                
                ChildData childData = createChildData(mpr, materialRelation);
                if (childData != null) {
                    childItems.add(childData);
                }
            }
        }
        
        samm.setChildItems(childItems);
        return samm;
    }

    /**
     * Create a ChildData entry from a MaterialPartnerRelation and MaterialRelation.
     *
     * @param mpr              the MaterialPartnerRelation containing partner identifiers
     * @param materialRelation the MaterialRelation containing quantity and validity info
     * @return                 the ChildData representation
     */
    private ChildData createChildData(MaterialPartnerRelation mpr, MaterialRelation materialRelation) {
        if (mpr == null) {
            return null;
        }

        ChildData childData = new ChildData();
        
        // Set the catena-X ID from partner's CX number
        String childCxId = mpr.getPartnerCXNumber();
        
        if (childCxId == null || childCxId.isEmpty()) {
            throw new IllegalStateException(
                String.format("No valid Catena-X ID found for child material %s with partner %s",
                    mpr.getMaterial().getOwnMaterialNumber(), mpr.getPartner().getBpnl()));
        }
        
        childData.setCatenaXId(childCxId);
        
        // Set business partner BPNL (the supplier of this component)
        childData.setBusinessPartner(mpr.getPartner().getBpnl());
        
        // Set timestamps from MaterialRelation if available, otherwise use current time
        String createdOn = materialRelation.getCreatedOn() != null ? 
            materialRelation.getCreatedOn().toInstant().toString() : Instant.now().toString();
        String lastModifiedOn = materialRelation.getLastModifiedOn() != null ? 
            materialRelation.getLastModifiedOn().toInstant().toString() : Instant.now().toString();
        childData.setCreatedOn(createdOn);
        childData.setLastModifiedOn(lastModifiedOn);
        
        // Set quantity from MaterialRelation
        double quantityValue = materialRelation.getQuantity();
        String unit = materialRelation.getMeasurementUnit() != null ? 
            materialRelation.getMeasurementUnit().getValue() : "unit:piece";
        ItemQuantity quantity = new ItemQuantity(quantityValue, unit);
        childData.setQuantity(quantity);
        
        // Set validity period from MaterialRelation if available
        // TODO: Map validFrom and validTo to ValidityPeriod object once DTO is defined
        childData.setValidityPeriod(null);
        
        return childData;
    }
}
