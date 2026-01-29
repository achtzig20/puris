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
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ChildData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ItemQuantity;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlannedSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ValidityPeriodEntity;
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

        // Only products (items we manufacture) have a BOM structure
        if (!material.isProductFlag()) {
            log.debug("Material {} is not marked as product, returning empty BOM", 
                material.getOwnMaterialNumber());
            samm.setChildItems(new HashSet<>());
            return samm;
        }

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
            
            // Find all supplier MaterialPartnerRelations for this child material
            List<MaterialPartnerRelation> supplierRelations = materialPartnerRelationService
                .findAllByOwnMaterialNumber(childMaterial.getOwnMaterialNumber()).stream()
                .filter(MaterialPartnerRelation::isPartnerSuppliesMaterial)
                .collect(Collectors.toList());

            // Create ChildData for each supplier of this component
            for (MaterialPartnerRelation mpr : supplierRelations) {
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
        
        // Set timestamps from MaterialRelation if available, otherwise use current time
        String createdOn = materialRelation.getCreatedOn() != null ? 
            materialRelation.getCreatedOn().toInstant().toString() : Instant.now().toString();
        childData.setCreatedOn(createdOn);
        
        // Set quantity from MaterialRelation
        double quantityValue = materialRelation.getQuantity();
        String unit = materialRelation.getMeasurementUnit().getValue();
        ItemQuantity quantity = new ItemQuantity(quantityValue, unit);
        childData.setQuantity(quantity);
        
        // Set last modified timestamp
        String lastModifiedOn = materialRelation.getLastModifiedOn() != null ? 
            materialRelation.getLastModifiedOn().toInstant().toString() : Instant.now().toString(); //fallback to createdon 
        childData.setLastModifiedOn(lastModifiedOn);
        
        // Set validity period from MaterialRelation if available
        ValidityPeriodEntity validityPeriod = null;
        if (materialRelation.getValidFrom() != null || materialRelation.getValidTo() != null) {
            String validFromStr = materialRelation.getValidFrom() != null ? 
                materialRelation.getValidFrom().toInstant().toString() : null;
            String validToStr = materialRelation.getValidTo() != null ? 
                materialRelation.getValidTo().toInstant().toString() : null;
            validityPeriod = new ValidityPeriodEntity(validFromStr, validToStr);
        }
        childData.setValidityPeriod(validityPeriod);
        
        // Set business partner BPNL (the supplier of this component)
        childData.setBusinessPartner(mpr.getPartner().getBpnl());
        
        // Set the catena-X ID from child material's CX number
        String childCxId = mpr.getPartnerCXNumber();
        if (childCxId == null || childCxId.isEmpty()) {
            throw new IllegalStateException(
                String.format("No valid Catena-X ID found for child material %s with partner %s",
                    mpr.getMaterial().getOwnMaterialNumber(), mpr.getPartner().getBpnl()));
        }
        childData.setCatenaXId(childCxId);
        
        return childData;
    }
}
