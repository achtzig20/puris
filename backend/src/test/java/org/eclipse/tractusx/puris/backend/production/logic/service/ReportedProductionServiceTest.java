/*
 * Copyright (c) 2025 Volkswagen AG
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.production.logic.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedProduction;
import org.eclipse.tractusx.puris.backend.production.domain.repository.ReportedProductionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ReportedProductionServiceTest {

    @Mock
    private ReportedProductionRepository repository;

    @InjectMocks
    private ReportedProductionService reportedProductionService;

    private static final Material testMaterial;
    private static final Partner testPartner;
    private static final Site partnerSite;
    private static final Site anotherPartnerSite;
    private static final Date testDate = Date.from(Instant.parse("2025-01-15T10:00:00Z"));
    private static final Date futureDate = Date.from(Instant.parse("2025-12-31T23:59:59Z"));
    
    private static final String PARTNER_SITE_BPNS = "BPNS1234567890AB";
    private static final String ANOTHER_PARTNER_SITE_BPNS = "BPNS1234567890CD";
    private static final String INVALID_BPNS = "BPNS9999999999ZZ";
    private static final String PARTNER_BPNL = "BPNL0987654321BA";

    static {
        testMaterial = new Material();
        testMaterial.setOwnMaterialNumber("TEST-MATERIAL-001");
        testMaterial.setMaterialFlag(true);
        testMaterial.setName("Test Material");

        testPartner = new Partner();
        testPartner.setUuid(UUID.randomUUID());
        testPartner.setBpnl(PARTNER_BPNL);
        testPartner.setName("Test Partner");

        partnerSite = new Site();
        partnerSite.setBpns(PARTNER_SITE_BPNS);
        partnerSite.setName("Partner Site");

        anotherPartnerSite = new Site();
        anotherPartnerSite.setBpns(ANOTHER_PARTNER_SITE_BPNS);
        anotherPartnerSite.setName("Another Partner Site");

        SortedSet<Site> sites = new TreeSet<>(Comparator.comparing(Site::getBpns));
        sites.add(partnerSite);
        sites.add(anotherPartnerSite);
        testPartner.setSites(sites);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Create Production Tests
    @Test
    void testCreate_ValidReportedProduction_ReturnsCreatedReportedProduction() {
        ReportedProduction validProduction = createValidProduction();
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(validProduction)).thenReturn(validProduction);

        ReportedProduction result = reportedProductionService.create(validProduction);

        assertNotNull(result);
        assertEquals(validProduction, result);
        verify(repository).save(validProduction);
    }

    @Test
    void testCreate_InvalidProduction_ReturnsNull() {
        ReportedProduction invalidProduction = createInvalidProduction();

        ReportedProduction result = reportedProductionService.create(invalidProduction);

        assertNull(result);
        verify(repository, never()).save(any());
    }

    @Test
    void testCreate_DuplicateProduction_ReturnsNull() {
        ReportedProduction validProduction = createValidProduction();
        ReportedProduction existingProduction = createValidProduction();
        
        when(repository.findAll()).thenReturn(List.of(existingProduction));

        ReportedProduction result = reportedProductionService.create(validProduction);

        assertNull(result);
        verify(repository, never()).save(any());
    }

    @Test
    void testCreate_NullProduction_ReturnsNull() {
        ReportedProduction result = reportedProductionService.create(null);

        assertNull(result);
        verify(repository, never()).save(any());
    }

    // CreateAll Production Tests
    @Test
    void testCreateAll_ContainsInvalidProduction_ReturnsNull() {
        ReportedProduction validProduction = createValidProduction();
        ReportedProduction invalidProduction = createInvalidProduction();
        List<ReportedProduction> productions = List.of(validProduction, invalidProduction);

        List<ReportedProduction> result = reportedProductionService.createAll(productions);

        assertNull(result);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testCreateAll_DuplicateWithExistingData_ReturnsNull() {
        ReportedProduction newProduction = createValidProduction();
        List<ReportedProduction> newProductions = List.of(newProduction);

        when(repository.findAll()).thenReturn(List.of(newProduction));

        List<ReportedProduction> result = reportedProductionService.createAll(newProductions);

        assertNull(result);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testCreateAll_EmptyList_ReturnsEmptyList() {
        List<ReportedProduction> emptyList = Collections.emptyList();
        when(repository.saveAll(emptyList)).thenReturn(emptyList);

        List<ReportedProduction> result = reportedProductionService.createAll(emptyList);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).saveAll(emptyList);
    }

    @Test
    void testCreateAll_NullList_ThrowsException() {
        assertThrows(NullPointerException.class, () -> reportedProductionService.createAll(null));
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testCreateAll_ListContainsNull_ReturnsNull() {
        ReportedProduction validProduction = createValidProduction();
        List<ReportedProduction> productions = Arrays.asList(validProduction, null);

        List<ReportedProduction> result = reportedProductionService.createAll(productions);

        assertNull(result);
        verify(repository, never()).saveAll(any());
    }

    // Validate Tests - Basic Validation
    @Test
    void testValidate_ValidProduction_ReturnsTrue() {
        ReportedProduction validProduction = createValidProduction();

        boolean result = reportedProductionService.validate(validProduction);

        assertTrue(result);
    }

    @Test
    void testValidate_InvalidProduction_ReturnsFalse() {
        ReportedProduction invalidProduction = createInvalidProduction();

        boolean result = reportedProductionService.validate(invalidProduction);

        assertFalse(result);
    }

    @Test
    void testValidate_NullProduction_ReturnsFalse() {
        boolean result = reportedProductionService.validate(null);
        assertFalse(result);
    }

    // Validate Tests - Quantity Validation
    @Test
    void testValidate_NegativeQuantity_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setQuantity(-10.0);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_ZeroQuantity_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setQuantity(0.0);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_PositiveQuantity_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setQuantity(50.5);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    // Validate Tests - Measurement Unit Validation
    @Test
    void testValidate_NullMeasurementUnit_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setMeasurementUnit(null);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_ValidMeasurementUnit_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setMeasurementUnit(ItemUnitEnumeration.UNIT_KILOGRAM);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    // Validate Tests - Estimated Time of Completion Validation
    @Test
    void testValidate_NullEstimatedTimeOfCompletion_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setEstimatedTimeOfCompletion(null);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_ValidEstimatedTimeOfCompletion_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setEstimatedTimeOfCompletion(futureDate);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    // Validate Tests - Material Validation
    @Test
    void testValidate_NullMaterial_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setMaterial(null);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_ValidMaterial_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        Material anotherMaterial = new Material();
        anotherMaterial.setOwnMaterialNumber("ANOTHER-MATERIAL");
        production.setMaterial(anotherMaterial);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    // Validate Tests - Partner Validation
    @Test
    void testValidate_NullPartner_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setPartner(null);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_ValidPartner_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        Partner anotherPartner = new Partner();
        anotherPartner.setBpnl("BPNL9999999999ZZ");
        Site site = new Site();
        site.setBpns(PARTNER_SITE_BPNS);
        SortedSet<Site> sites = new TreeSet<>(Comparator.comparing(Site::getBpns));
        sites.add(site);
        anotherPartner.setSites(sites);
        production.setPartner(anotherPartner);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    // Validate Tests - Production Site BPNS Validation
    @Test
    void testValidate_NullProductionSiteBpns_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setProductionSiteBpns(null);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_InvalidProductionSiteBpns_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setProductionSiteBpns(INVALID_BPNS);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_ValidAlternativeProductionSiteBpns_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setProductionSiteBpns(ANOTHER_PARTNER_SITE_BPNS);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    @Test
    void testValidate_PartnerWithNoSites_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        Partner partnerWithNoSites = new Partner();
        partnerWithNoSites.setBpnl("BPNL9999999999ZZ");
        partnerWithNoSites.setSites(new TreeSet<>());
        production.setPartner(partnerWithNoSites);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_PartnerWithNullSites_ThrowsNullPointerException() {
        ReportedProduction production = createValidProduction();
        Partner partnerWithNullSites = new Partner();
        partnerWithNullSites.setBpnl("BPNL9999999999ZZ");
        partnerWithNullSites.setSites(null);
        production.setPartner(partnerWithNullSites);

        assertThrows(NullPointerException.class, () -> reportedProductionService.validate(production));
    }

    // Validate Tests - Order Number Validation
    @Test
    void testValidate_BothCustomerOrderFieldsSet_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber("ORDER-001");
        production.setCustomerOrderPositionNumber("POS-001");

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    @Test
    void testValidate_AllOrderFieldsNull_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber(null);
        production.setSupplierOrderNumber(null);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    @Test
    void testValidate_CustomerOrderNumberWithoutPosition_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber("ORDER-001");
        production.setCustomerOrderPositionNumber(null);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_CustomerOrderPositionWithoutNumber_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber("POS-001");

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_OnlySupplierOrderNumber_ReturnsFalse() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber(null);
        production.setSupplierOrderNumber("SUPPLY-001");

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testValidate_AllOrderFieldsSet_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber("ORDER-001");
        production.setCustomerOrderPositionNumber("POS-001");
        production.setSupplierOrderNumber("SUPPLY-001");

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    @Test
    void testValidate_CustomerOrderFieldsSetSupplierNull_ReturnsTrue() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber("ORDER-001");
        production.setCustomerOrderPositionNumber("POS-001");
        production.setSupplierOrderNumber(null);

        boolean result = reportedProductionService.validate(production);

        assertTrue(result);
    }

    // Edge Cases and Integration Tests
    @Test
    void testValidate_MultipleValidationErrors_ReturnsFalse() {
        ReportedProduction production = new ReportedProduction();
        production.setQuantity(-1.0);
        production.setMeasurementUnit(null);
        production.setEstimatedTimeOfCompletion(null);
        production.setMaterial(null);
        production.setPartner(null);
        production.setProductionSiteBpns(null);

        boolean result = reportedProductionService.validate(production);

        assertFalse(result);
    }

    @Test
    void testCreate_ValidProductionWithComplexOrderScenario_Success() {
        ReportedProduction production = createValidProduction();
        production.setCustomerOrderNumber("COMPLEX-ORDER-001");
        production.setCustomerOrderPositionNumber("COMPLEX-POS-001");
        production.setSupplierOrderNumber("COMPLEX-SUPPLY-001");
        
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(production)).thenReturn(production);

        ReportedProduction result = reportedProductionService.create(production);

        assertNotNull(result);
        assertEquals(production, result);
        verify(repository).save(production);
    }

    @Test
    void testCreateAll_MixedValidAndInvalidProductions_ReturnsNull() {
        ReportedProduction validProduction1 = createValidProduction();
        ReportedProduction validProduction2 = createValidProductionWithDifferentData();
        ReportedProduction invalidProduction = createInvalidProduction();
        
        List<ReportedProduction> productions = List.of(validProduction1, validProduction2, invalidProduction);

        List<ReportedProduction> result = reportedProductionService.createAll(productions);

        assertNull(result);
        verify(repository, never()).saveAll(any());
    }

    // Helpers
    private ReportedProduction createValidProduction() {
        ReportedProduction production = new ReportedProduction();
        production.setQuantity(100.0);
        production.setMeasurementUnit(ItemUnitEnumeration.UNIT_PIECE);
        production.setEstimatedTimeOfCompletion(futureDate);
        production.setMaterial(testMaterial);
        production.setPartner(testPartner);
        production.setProductionSiteBpns(PARTNER_SITE_BPNS);
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber(null);
        production.setSupplierOrderNumber(null);
        return production;
    }

    private ReportedProduction createValidProductionWithDifferentData() {
        ReportedProduction production = new ReportedProduction();
        production.setQuantity(200.0);
        production.setMeasurementUnit(ItemUnitEnumeration.UNIT_KILOGRAM);
        production.setEstimatedTimeOfCompletion(futureDate);
        production.setMaterial(testMaterial);
        production.setPartner(testPartner);
        production.setProductionSiteBpns(ANOTHER_PARTNER_SITE_BPNS);
        production.setCustomerOrderNumber("ORDER-002");
        production.setCustomerOrderPositionNumber("POS-002");
        production.setSupplierOrderNumber("SUPPLY-002");
        return production;
    }

    private ReportedProduction createInvalidProduction() {
        ReportedProduction production = new ReportedProduction();
        production.setQuantity(-1.0);
        production.setMeasurementUnit(null);
        production.setEstimatedTimeOfCompletion(null);
        production.setMaterial(null);
        production.setPartner(testPartner);
        production.setProductionSiteBpns(null);
        return production;
    }
}