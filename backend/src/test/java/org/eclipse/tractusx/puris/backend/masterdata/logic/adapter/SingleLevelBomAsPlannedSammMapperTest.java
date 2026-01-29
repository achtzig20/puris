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

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.ChildData;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlannedSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SingleLevelBomAsPlannedSammMapperTest {

    @Mock
    private MaterialRelationService materialRelationService;

    @Mock
    private MaterialPartnerRelationService materialPartnerRelationService;

    @Mock
    private MaterialService materialService;

    @InjectMocks
    private SingleLevelBomAsPlannedSammMapper mapper;

    private static final String PARENT_MATERIAL_NUMBER = "MAT-PARENT-001";
    private static final String PARENT_CX_ID = "urn:uuid:parent-cx-123";
    private static final String CHILD1_MATERIAL_NUMBER = "MAT-CHILD-001";
    private static final String CHILD1_CX_ID = "urn:uuid:child1-cx-456";
    private static final String CHILD1_PARTNER_CX_ID = "urn:uuid:partner-child1-cx";
    private static final String CHILD2_MATERIAL_NUMBER = "MAT-CHILD-002";
    private static final String CHILD2_CX_ID = "urn:uuid:child2-cx-789";
    private static final String CHILD2_PARTNER_CX_ID = "urn:uuid:partner-child2-cx";
    private static final String PARTNER_BPNL = "BPNL1234567890AB";

    private Material parentMaterial;
    private Material childMaterial1;
    private Material childMaterial2;
    private Partner partner;
    private MaterialRelation materialRelation1;
    private MaterialRelation materialRelation2;
    private MaterialPartnerRelation mpr1;
    private MaterialPartnerRelation mpr2;

    @BeforeEach
    void setUp() {
        // Parent material is a product (we manufacture it, so it has a BOM)
        parentMaterial = createMaterial(PARENT_MATERIAL_NUMBER, PARENT_CX_ID, "Parent Product", true, false);
        // Child materials are materials (we buy them from suppliers)
        childMaterial1 = createMaterial(CHILD1_MATERIAL_NUMBER, CHILD1_CX_ID, "Child Component 1", false, true);
        childMaterial2 = createMaterial(CHILD2_MATERIAL_NUMBER, CHILD2_CX_ID, "Child Component 2", false, true);
        partner = createPartner(PARTNER_BPNL, "Test Partner");
        materialRelation1 = createMaterialRelation(PARENT_MATERIAL_NUMBER, CHILD1_MATERIAL_NUMBER, 2.5,
                ItemUnitEnumeration.UNIT_PIECE);
        materialRelation2 = createMaterialRelation(PARENT_MATERIAL_NUMBER, CHILD2_MATERIAL_NUMBER, 1.0,
                ItemUnitEnumeration.UNIT_KILOGRAM);
        mpr1 = createMaterialPartnerRelation(childMaterial1, partner, CHILD1_PARTNER_CX_ID, "PARTNER-MAT-001");
        mpr2 = createMaterialPartnerRelation(childMaterial2, partner, CHILD2_PARTNER_CX_ID, "PARTNER-MAT-002");
    }

    private Material createMaterial(String ownNumber, String cxId, String name, boolean isProduct, boolean isMaterial) {
        Material material = new Material();
        material.setOwnMaterialNumber(ownNumber);
        material.setMaterialNumberCx(cxId);
        material.setName(name);
        material.setProductFlag(isProduct);
        material.setMaterialFlag(isMaterial);
        return material;
    }

    private Partner createPartner(String bpnl, String name) {
        Partner p = new Partner();
        p.setUuid(UUID.randomUUID());
        p.setBpnl(bpnl);
        p.setName(name);
        return p;
    }

    private MaterialRelation createMaterialRelation(String parentNumber, String childNumber, double quantity,
            ItemUnitEnumeration unit) {
        MaterialRelation relation = new MaterialRelation();
        relation.setUuid(UUID.randomUUID());
        relation.setParentMaterialNumber(parentNumber);
        relation.setChildMaterialNumber(childNumber);
        relation.setQuantity(quantity);
        relation.setMeasurementUnit(unit);
        relation.setCreatedOn(new Date());
        relation.setLastModifiedOn(new Date());
        return relation;
    }

    private MaterialPartnerRelation createMaterialPartnerRelation(Material material, Partner partner,
            String partnerCxNumber, String partnerMatNumber) {
        MaterialPartnerRelation mpr = new MaterialPartnerRelation();
        mpr.setMaterial(material);
        mpr.setPartner(partner);
        mpr.setPartnerCXNumber(partnerCxNumber);
        mpr.setPartnerMaterialNumber(partnerMatNumber);
        mpr.setPartnerSuppliesMaterial(true);
        return mpr;
    }

    private ChildData findChildByCxId(SingleLevelBomAsPlannedSAMM result, String cxId) {
        return result.getChildItems().stream()
                .filter(c -> c.getCatenaXId().equals(cxId))
                .findFirst()
                .orElse(null);
    }

    private void assertChildData(ChildData childData, String expectedBpnl, double expectedQuantity,
            String expectedUnit) {
        assertNotNull(childData);
        assertEquals(expectedBpnl, childData.getBusinessPartner());
        assertEquals(expectedQuantity, childData.getQuantity().getQuantityNumber());
        assertEquals(expectedUnit, childData.getQuantity().getMeasurementUnit());
    }

    private void setupSingleChildRelation() {
        when(materialRelationService.findAll()).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER)).thenReturn(childMaterial1);
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER))
                .thenReturn(Collections.singletonList(mpr1));
    }

    @Test
    void testMaterialToSamm_WithValidChildren() {
        when(materialRelationService.findAll()).thenReturn(Arrays.asList(materialRelation1, materialRelation2));
        when(materialService.findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER)).thenReturn(childMaterial1);
        when(materialService.findByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER)).thenReturn(childMaterial2);
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER))
                .thenReturn(Collections.singletonList(mpr1));
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER))
                .thenReturn(Collections.singletonList(mpr2));

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertNotNull(result);
        assertEquals(PARENT_CX_ID, result.getCatenaXId());
        assertEquals(2, result.getChildItems().size());

        verify(materialRelationService).findAll();
        verify(materialService).findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER);
        verify(materialService).findByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER);
        verify(materialPartnerRelationService).findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER);
        verify(materialPartnerRelationService).findAllByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER);

        ChildData child1Data = findChildByCxId(result, CHILD1_PARTNER_CX_ID);
        assertChildData(child1Data, PARTNER_BPNL, 2.5, "unit:piece");

        ChildData child2Data = findChildByCxId(result, CHILD2_PARTNER_CX_ID);
        assertChildData(child2Data, PARTNER_BPNL, 1.0, "unit:kilogram");
    }

    @Test
    void testMaterialToSamm_WithEmptyBOM() {
        when(materialRelationService.findAll()).thenReturn(Collections.emptyList());

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertNotNull(result);
        assertEquals(PARENT_CX_ID, result.getCatenaXId());
        assertTrue(result.getChildItems().isEmpty());
        verify(materialRelationService).findAll();
        verify(materialService, never()).findByOwnMaterialNumber(anyString());
        verify(materialPartnerRelationService, never()).findAllByOwnMaterialNumber(anyString());
    }

    @Test
    void testMaterialToSamm_WithMissingChildMaterial() {
        when(materialRelationService.findAll()).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER)).thenReturn(null);

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertNotNull(result);
        assertEquals(PARENT_CX_ID, result.getCatenaXId());
        assertTrue(result.getChildItems().isEmpty());
        verify(materialRelationService).findAll();
        verify(materialService).findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER);
        verify(materialPartnerRelationService, never()).findAllByOwnMaterialNumber(anyString());
    }

    @Test
    void testMaterialToSamm_WithNoSupplierRelations() {
        mpr1.setPartnerSuppliesMaterial(false);
        when(materialRelationService.findAll()).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER)).thenReturn(childMaterial1);
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER))
                .thenReturn(Collections.singletonList(mpr1));

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertNotNull(result);
        assertEquals(PARENT_CX_ID, result.getCatenaXId());
        assertTrue(result.getChildItems().isEmpty());
        verify(materialRelationService).findAll();
        verify(materialService).findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER);
        verify(materialPartnerRelationService).findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER);
    }

    @Test
    void testMaterialToSamm_WithPartialSupplierRelations() {
        when(materialRelationService.findAll()).thenReturn(Arrays.asList(materialRelation1, materialRelation2));
        when(materialService.findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER)).thenReturn(childMaterial1);
        when(materialService.findByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER)).thenReturn(childMaterial2);
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER))
                .thenReturn(Collections.singletonList(mpr1));
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER))
                .thenReturn(Collections.emptyList());

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertNotNull(result);
        assertEquals(PARENT_CX_ID, result.getCatenaXId());
        assertEquals(1, result.getChildItems().size());

        ChildData childData = result.getChildItems().iterator().next();
        assertEquals(CHILD1_PARTNER_CX_ID, childData.getCatenaXId());
        assertEquals(PARTNER_BPNL, childData.getBusinessPartner());
    }

    @Test
    void testMaterialToSamm_ThrowsExceptionWhenPartnerCxNumberIsNull() {
        mpr1.setPartnerCXNumber(null);
        setupSingleChildRelation();

        assertThrows(IllegalStateException.class, () -> mapper.materialToSamm(parentMaterial));
    }

    @Test
    void testMaterialToSamm_ThrowsExceptionWhenPartnerCxNumberIsEmpty() {
        mpr1.setPartnerCXNumber("");
        setupSingleChildRelation();

        assertThrows(IllegalStateException.class, () -> mapper.materialToSamm(parentMaterial));
    }

    @Test
    void testMaterialToSamm_VerifyQuantityAndUnitMapping() {
        setupSingleChildRelation();

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        ChildData childData = result.getChildItems().iterator().next();
        assertNotNull(childData.getQuantity());
        assertEquals(2.5, childData.getQuantity().getQuantityNumber());
        assertEquals("unit:piece", childData.getQuantity().getMeasurementUnit());
    }

    @Test
    void testMaterialToSamm_VerifyTimestamps() {
        setupSingleChildRelation();

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        ChildData childData = result.getChildItems().iterator().next();
        assertNotNull(childData.getCreatedOn());
        assertNotNull(childData.getLastModifiedOn());
        assertTrue(childData.getCreatedOn().matches(".*T.*"));
        assertTrue(childData.getLastModifiedOn().matches(".*T.*"));
    }

    @Test
    void testMaterialToSamm_OnlyIncludesChildrenForSpecificParent() {
        MaterialRelation otherParentRelation = createMaterialRelation("OTHER-PARENT", "MAT-CHILD-003", 1.0,
                ItemUnitEnumeration.UNIT_PIECE);

        when(materialRelationService.findAll())
                .thenReturn(Arrays.asList(materialRelation1, materialRelation2, otherParentRelation));
        when(materialService.findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER)).thenReturn(childMaterial1);
        when(materialService.findByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER)).thenReturn(childMaterial2);
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER))
                .thenReturn(Collections.singletonList(mpr1));
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD2_MATERIAL_NUMBER))
                .thenReturn(Collections.singletonList(mpr2));

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertEquals(2, result.getChildItems().size());
        verify(materialService, never()).findByOwnMaterialNumber("MAT-CHILD-003");
    }

    @Test
    void testMaterialToSamm_NonProductReturnsEmptyBOM() {
        parentMaterial.setProductFlag(false);

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertNotNull(result);
        assertEquals(PARENT_CX_ID, result.getCatenaXId());
        assertTrue(result.getChildItems().isEmpty());
        verify(materialRelationService, never()).findAll();
    }

    @Test
    void testMaterialToSamm_MultipleSuppliers() {
        Partner partner2 = createPartner("BPNL9876543210XY", "Second Supplier");
        MaterialPartnerRelation mpr1b = createMaterialPartnerRelation(childMaterial1, partner2,
                "urn:uuid:partner2-child1-cx", "PARTNER2-MAT-001");

        when(materialRelationService.findAll()).thenReturn(Collections.singletonList(materialRelation1));
        when(materialService.findByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER)).thenReturn(childMaterial1);
        when(materialPartnerRelationService.findAllByOwnMaterialNumber(CHILD1_MATERIAL_NUMBER))
                .thenReturn(Arrays.asList(mpr1, mpr1b));

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        assertEquals(2, result.getChildItems().size());

        List<String> bpnls = result.getChildItems().stream()
                .map(ChildData::getBusinessPartner)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList("BPNL1234567890AB", "BPNL9876543210XY"), bpnls);
    }

    @Test
    void testMaterialToSamm_ValidityPeriod() {
        materialRelation1.setValidFrom(new Date(System.currentTimeMillis() - 86400000));
        materialRelation1.setValidTo(new Date(System.currentTimeMillis() + 86400000));
        setupSingleChildRelation();

        SingleLevelBomAsPlannedSAMM result = mapper.materialToSamm(parentMaterial);

        ChildData childData = result.getChildItems().iterator().next();
        assertNotNull(childData.getValidityPeriod());
        assertNotNull(childData.getValidityPeriod().getValidFrom());
        assertNotNull(childData.getValidityPeriod().getValidTo());
    }
}
