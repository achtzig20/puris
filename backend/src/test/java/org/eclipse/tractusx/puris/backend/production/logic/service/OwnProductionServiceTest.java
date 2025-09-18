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

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.eclipse.tractusx.puris.backend.production.domain.repository.OwnProductionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OwnProductionServiceTest {

    @Mock
    private OwnProductionRepository repository;

    @Mock
    private PartnerService partnerService;

    @InjectMocks
    private OwnProductionService ownProductionService;

    private static final Material testMaterial;
    private static final Partner testPartner;
    private static final Partner ownPartner;
    private static final Site ownSite;
    private static final Site anotherOwnSite;
    private static final Date testDate = Date.from(Instant.parse("2025-01-15T10:00:00Z"));
    private static final Date futureDate = Date.from(Instant.parse("2025-12-31T23:59:59Z"));
    private static final Date pastDate = Date.from(Instant.parse("2024-01-15T10:00:00Z"));
    
    private static final String OWN_BPNS = "BPNS1234567890AB";
    private static final String ANOTHER_OWN_BPNS = "BPNS1234567890CD";
    private static final String INVALID_BPNS = "BPNS9999999999ZZ";
    private static final String PARTNER_BPNL = "BPNL0987654321BA";
    private static final String OWN_BPNL = "BPNL1234567890AB";

    static {
        testMaterial = new Material();
        testMaterial.setOwnMaterialNumber("TEST-MATERIAL-001");
        testMaterial.setMaterialFlag(true);
        testMaterial.setName("Test Material");

        testPartner = new Partner();
        testPartner.setUuid(UUID.randomUUID());
        testPartner.setBpnl(PARTNER_BPNL);
        testPartner.setName("Test Partner");

        ownPartner = new Partner();
        ownPartner.setUuid(UUID.randomUUID());
        ownPartner.setBpnl(OWN_BPNL);
        ownPartner.setName("Own Partner");

        ownSite = new Site();
        ownSite.setBpns(OWN_BPNS);
        ownSite.setName("Own Site");

        anotherOwnSite = new Site();
        anotherOwnSite.setBpns(ANOTHER_OWN_BPNS);
        anotherOwnSite.setName("Another Own Site");

        SortedSet<Site> sites = new TreeSet<>(Comparator.comparing(Site::getBpns));
        sites.add(ownSite);
        sites.add(anotherOwnSite);
        ownPartner.setSites(sites);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
    }

    // Create Production Tests
    @Test
    void testCreate_ValidProduction_ReturnsCreatedProduction() {
        OwnProduction validProduction = createValidProduction();
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(validProduction)).thenReturn(validProduction);

        OwnProduction result = ownProductionService.create(validProduction);

        assertNotNull(result);
        assertEquals(validProduction, result);
        verify(repository).save(validProduction);
    }

    @Test
    void testCreate_InvalidProduction_ThrowsIllegalArgumentException() {
        OwnProduction invalidProduction = createInvalidProduction();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> ownProductionService.create(invalidProduction));
        
        assertEquals("Invalid production", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void testCreate_DuplicateProduction_ThrowsKeyAlreadyExistsException() {
        OwnProduction validProduction = createValidProduction();
        OwnProduction existingProduction = createValidProduction();
        
        when(repository.findAll()).thenReturn(List.of(existingProduction));

        KeyAlreadyExistsException exception = assertThrows(KeyAlreadyExistsException.class, 
            () -> ownProductionService.create(validProduction));
        
        assertEquals("Production already exists", exception.getMessage());
        verify(repository, never()).save(any());
    }

    // CreateAll Production Tests
    @Test
    void testCreateAll_ValidProductionList_ReturnsCreatedProductions() {
        OwnProduction production1 = createValidProduction();
        OwnProduction production2 = createValidProductionWithDifferentData();
        List<OwnProduction> productions = List.of(production1, production2);
        
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.saveAll(productions)).thenReturn(productions);

        List<OwnProduction> result = ownProductionService.createAll(productions);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(productions, result);
        verify(repository).saveAll(productions);
    }

    @Test
    void testCreateAll_ContainsInvalidProduction_ThrowsIllegalArgumentException() {
        OwnProduction validProduction = createValidProduction();
        OwnProduction invalidProduction = createInvalidProduction();
        List<OwnProduction> productions = List.of(validProduction, invalidProduction);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> ownProductionService.createAll(productions));
        
        assertEquals("Invalid production", exception.getMessage());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testCreateAll_DuplicateWithExistingData_ThrowsKeyAlreadyExistsException() {
        OwnProduction newProduction = createValidProduction();
        List<OwnProduction> newProductions = List.of(newProduction);

        when(repository.findAll()).thenReturn(List.of(newProduction));

        KeyAlreadyExistsException exception = assertThrows(KeyAlreadyExistsException.class, 
            () -> ownProductionService.createAll(newProductions));
        
        assertEquals("Production already exists", exception.getMessage());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testCreateAll_EmptyList_ReturnsEmptyList() {
        List<OwnProduction> emptyList = Collections.emptyList();
        when(repository.saveAll(emptyList)).thenReturn(emptyList);

        List<OwnProduction> result = ownProductionService.createAll(emptyList);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).saveAll(emptyList);
    }

    // Validate Tests
    @Test
    void testValidate_ValidProduction_ReturnsTrue() {
        OwnProduction validProduction = createValidProduction();

        boolean result = ownProductionService.validate(validProduction);

        assertTrue(result);
    }

    @Test
    void testValidate_InvalidProduction_ReturnsFalse() {
        OwnProduction invalidProduction = createInvalidProduction();

        boolean result = ownProductionService.validate(invalidProduction);

        assertFalse(result);
    }

    // Validate details Tests
    @Test
    void testValidateWithDetails_ValidProduction_ReturnsEmptyList() {
        OwnProduction validProduction = createValidProduction();

        List<String> errors = ownProductionService.validateWithDetails(validProduction);

        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWithDetails_NegativeQuantity_ReturnsQuantityError() {
        OwnProduction production = createValidProduction();
        production.setQuantity(-10.0);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Quantity must be greater than 0."));
    }

    @Test
    void testValidateWithDetails_ZeroQuantity_ReturnsQuantityError() {
        OwnProduction production = createValidProduction();
        production.setQuantity(0.0);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Quantity must be greater than 0."));
    }

    @Test
    void testValidateWithDetails_NullMeasurementUnit_ReturnsMeasurementUnitError() {
        OwnProduction production = createValidProduction();
        production.setMeasurementUnit(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Missing measurement unit."));
    }

    @Test
    void testValidateWithDetails_NullLastUpdatedOnDateTime_ReturnsLastUpdatedError() {
        OwnProduction production = createValidProduction();
        production.setLastUpdatedOnDateTime(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Missing lastUpdatedOnTime."));
    }

    @Test
    void testValidateWithDetails_FutureLastUpdatedOnDateTime_ReturnsDateError() {
        OwnProduction production = createValidProduction();
        production.setLastUpdatedOnDateTime(futureDate);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("lastUpdatedOnDateTime cannot be in the future."));
    }

    @Test
    void testValidateWithDetails_NullEstimatedTimeOfCompletion_ReturnsCompletionTimeError() {
        OwnProduction production = createValidProduction();
        production.setEstimatedTimeOfCompletion(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Missing estimated time of completion."));
    }

    @Test
    void testValidateWithDetails_NullMaterial_ReturnsMaterialError() {
        OwnProduction production = createValidProduction();
        production.setMaterial(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Missing material."));
    }

    @Test
    void testValidateWithDetails_NullPartner_ReturnsPartnerError() {
        OwnProduction production = createValidProduction();
        production.setPartner(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Missing partner."));
    }

    @Test
    void testValidateWithDetails_PartnerSameAsOwnPartner_ReturnsPartnerError() {
        OwnProduction production = createValidProduction();
        production.setPartner(ownPartner);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Partner cannot be the same as own partner entity."));
    }

    @Test
    void testValidateWithDetails_NullProductionSiteBpns_ReturnsBpnsError() {
        OwnProduction production = createValidProduction();
        production.setProductionSiteBpns(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Missing production site BPNS."));
    }

    @Test
    void testValidateWithDetails_InvalidProductionSiteBpns_ReturnsBpnsMatchError() {
        OwnProduction production = createValidProduction();
        production.setProductionSiteBpns(INVALID_BPNS);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Production site BPNS must match one of the own partner entity's site BPNS."));
    }

    @Test
    void testValidateWithDetails_ValidAlternativeProductionSiteBpns_ReturnsEmptyList() {
        OwnProduction production = createValidProduction();
        production.setProductionSiteBpns(ANOTHER_OWN_BPNS);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWithDetails_CustomerOrderNumberWithoutPosition_ReturnsOrderError() {
        OwnProduction production = createValidProduction();
        production.setCustomerOrderNumber("ORDER-001");
        production.setCustomerOrderPositionNumber(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("If an order position reference is given, customer order number and customer order position number must be set."));
    }

    @Test
    void testValidateWithDetails_CustomerOrderPositionWithoutNumber_ReturnsOrderError() {
        OwnProduction production = createValidProduction();
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber("POS-001");

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("If an order position reference is given, customer order number and customer order position number must be set."));
    }

    @Test
    void testValidateWithDetails_BothCustomerOrderFieldsSet_ReturnsEmptyList() {
        OwnProduction production = createValidProduction();
        production.setCustomerOrderNumber("ORDER-001");
        production.setCustomerOrderPositionNumber("POS-001");

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWithDetails_AllOrderFieldsNull_ReturnsEmptyList() {
        OwnProduction production = createValidProduction();
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber(null);
        production.setSupplierOrderNumber(null);

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWithDetails_OnlySupplierOrderNumber_ReturnsOrderError() {
        OwnProduction production = createValidProduction();
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber(null);
        production.setSupplierOrderNumber("SUPPLY-001");

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("If an order position reference is given, customer order number and customer order position number must be set."));
    }

    @Test
    void testValidateWithDetails_OwnPartnerWithNoSites_ReturnsBpnsMatchError() {
        Partner ownPartnerWithNoSites = new Partner();
        ownPartnerWithNoSites.setUuid(UUID.randomUUID());
        ownPartnerWithNoSites.setBpnl(OWN_BPNL);
        SortedSet<Site> emptySites = new TreeSet<>();
        ownPartnerWithNoSites.setSites(emptySites);
        
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartnerWithNoSites);
        
        OwnProduction production = createValidProduction();

        List<String> errors = ownProductionService.validateWithDetails(production);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Production site BPNS must match one of the own partner entity's site BPNS."));
    }

    @Test
    void testValidateWithDetails_OwnPartnerWithNullSites_ReturnsBpnsMatchError() {
        Partner ownPartnerWithNullSites = new Partner();
        ownPartnerWithNullSites.setUuid(UUID.randomUUID());
        ownPartnerWithNullSites.setBpnl(OWN_BPNL);
        ownPartnerWithNullSites.setSites(null);
        
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartnerWithNullSites);
        
        OwnProduction production = createValidProduction();

        // throws a NullPointerException because stream() operation on null
        assertThrows(NullPointerException.class, 
            () -> ownProductionService.validateWithDetails(production));
    }

    // Helpers
    private OwnProduction createValidProduction() {
        OwnProduction production = new OwnProduction();
        production.setQuantity(100.0);
        production.setMeasurementUnit(ItemUnitEnumeration.UNIT_PIECE);
        production.setLastUpdatedOnDateTime(testDate);
        production.setEstimatedTimeOfCompletion(futureDate);
        production.setMaterial(testMaterial);
        production.setPartner(testPartner);
        production.setProductionSiteBpns(OWN_BPNS);
        production.setCustomerOrderNumber(null);
        production.setCustomerOrderPositionNumber(null);
        production.setSupplierOrderNumber(null);
        return production;
    }

    private OwnProduction createValidProductionWithDifferentData() {
        OwnProduction production = new OwnProduction();
        production.setQuantity(200.0);
        production.setMeasurementUnit(ItemUnitEnumeration.UNIT_KILOGRAM);
        production.setLastUpdatedOnDateTime(testDate);
        production.setEstimatedTimeOfCompletion(futureDate);
        production.setMaterial(testMaterial);
        production.setPartner(testPartner);
        production.setProductionSiteBpns(ANOTHER_OWN_BPNS);
        production.setCustomerOrderNumber("ORDER-002");
        production.setCustomerOrderPositionNumber("POS-002");
        production.setSupplierOrderNumber("SUPPLY-002");
        return production;
    }

    private OwnProduction createInvalidProduction() {
        OwnProduction production = new OwnProduction();
        production.setQuantity(-1.0);
        production.setMeasurementUnit(null);
        production.setLastUpdatedOnDateTime(null);
        production.setEstimatedTimeOfCompletion(null);
        production.setMaterial(null);
        production.setPartner(testPartner);
        production.setProductionSiteBpns(null);
        return production;
    }
}