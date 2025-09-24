/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import org.eclipse.tractusx.puris.backend.demand.domain.model.DemandCategoryEnumeration;
import org.eclipse.tractusx.puris.backend.demand.domain.model.OwnDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.repository.OwnDemandRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OwnDemandServiceTest {

    @InjectMocks
    private OwnDemandService ownDemandService;

    @Mock
    private OwnDemandRepository ownDemandRepository;

    @Mock
    private PartnerService partnerService;

    @Mock
    private MaterialPartnerRelationService mprService;

    private Partner ownPartner;
    private Partner supplierPartner;
    private Site ownSite;
    private Site supplierSite;
    private Material testMaterial;
    private OwnDemand validDemand;

    private static final String OWN_BPNL = "BPNL4444444444XX";
    private static final String OWN_BPNS = "BPNS4444444444XX";
    private static final String SUPPLIER_BPNL = "BPNL1234567890ZZ";
    private static final String SUPPLIER_BPNS = "BPNS1234567890ZZ";
    private static final String MATERIAL_NUMBER = "MAT-001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ownSite = new Site();
        ownSite.setBpns(OWN_BPNS);
        ownSite.setName("Own Site");

        supplierSite = new Site();
        supplierSite.setBpns(SUPPLIER_BPNS);
        supplierSite.setName("Supplier Site");

        SortedSet<Site> ownSites = new TreeSet<>(Comparator.comparing(Site::getBpns));
        ownSites.add(ownSite);

        SortedSet<Site> supplierSites = new TreeSet<>(Comparator.comparing(Site::getBpns));
        supplierSites.add(supplierSite);

        ownPartner = new Partner();
        ownPartner.setBpnl(OWN_BPNL);
        ownPartner.setName("Own Partner");
        ownPartner.setSites(ownSites);
        ownPartner.setUuid(UUID.randomUUID());

        supplierPartner = new Partner();
        supplierPartner.setBpnl(SUPPLIER_BPNL);
        supplierPartner.setName("Supplier Partner");
        supplierPartner.setSites(supplierSites);
        supplierPartner.setUuid(UUID.randomUUID());

        testMaterial = new Material();
        testMaterial.setOwnMaterialNumber(MATERIAL_NUMBER);
        testMaterial.setMaterialFlag(true);
        testMaterial.setProductFlag(false);
        testMaterial.setName("Test Material");

        validDemand = createValidDemand();
    }

    private OwnDemand createValidDemand() {
        OwnDemand demand = new OwnDemand();
        demand.setMaterial(testMaterial);
        demand.setPartner(supplierPartner);
        demand.setQuantity(100.0);
        demand.setMeasurementUnit(ItemUnitEnumeration.UNIT_PIECE);
        demand.setLastUpdatedOnDateTime(new Date());
        demand.setDay(new Date());
        demand.setDemandCategoryCode(DemandCategoryEnumeration.DEMAND_DEFAULT);
        demand.setDemandLocationBpns(OWN_BPNS);
        demand.setSupplierLocationBpns(SUPPLIER_BPNS);
        return demand;
    }

    @Test
    void testGetQuantityForDays_WithValidData_ReturnsCorrectQuantities() {
        LocalDate today = LocalDate.now();
        Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date tomorrowDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        OwnDemand demand1 = new OwnDemand();
        demand1.setDay(todayDate);
        demand1.setQuantity(50.0);

        OwnDemand demand2 = new OwnDemand();
        demand2.setDay(todayDate);
        demand2.setQuantity(25.0);

        OwnDemand demand3 = new OwnDemand();
        demand3.setDay(tomorrowDate);
        demand3.setQuantity(100.0);

        List<OwnDemand> allDemands = List.of(demand1, demand2, demand3);

        // Mock the findAllByFilters method
        ownDemandService = spy(ownDemandService);
        doReturn(allDemands).when(ownDemandService).findAllByFilters(any(), any(), any());

        // Act
        List<Double> quantities = ownDemandService.getQuantityForDays(MATERIAL_NUMBER, Optional.empty(), Optional.empty(), 2);

        // Assert
        assertEquals(2, quantities.size());
        assertEquals(75.0, quantities.get(0)); // 50 + 25 for today
        assertEquals(100.0, quantities.get(1)); // 100 for tomorrow
    }

    @Test
    void testGetQuantityForDays_WithNoDemands_ReturnsZeros() {
        // Arrange
        List<OwnDemand> emptyDemands = new ArrayList<>();
        ownDemandService = spy(ownDemandService);
        doReturn(emptyDemands).when(ownDemandService).findAllByFilters(any(), any(), any());

        // Act
        List<Double> quantities = ownDemandService.getQuantityForDays(MATERIAL_NUMBER, Optional.empty(), Optional.empty(), 3);

        // Assert
        assertEquals(3, quantities.size());
        assertEquals(0.0, quantities.get(0));
        assertEquals(0.0, quantities.get(1));
        assertEquals(0.0, quantities.get(2));
    }

    @Test
    void testValidate_WithValidDemand_ReturnsTrue() {
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);

        boolean result = ownDemandService.validate(validDemand);
        assertTrue(result);
    }

    @Test
    void testValidate_WithInvalidDemand_ReturnsFalse() {
        OwnDemand invalidDemand = new OwnDemand();
        invalidDemand.setMaterial(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);

        boolean result = ownDemandService.validate(invalidDemand);
        assertFalse(result);
    }

    @Test
    void testValidateWithDetails_WithValidDemand_ReturnsEmptyList() {
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);

        List<String> errors = ownDemandService.validateWithDetails(validDemand);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWithDetails_WithMissingMaterial_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setMaterial(null);
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);

        List<String> errors = ownDemandService.validateWithDetails(demand);
        assertTrue(errors.contains("Missing Material."));
    }

    @Test
    void testValidateWithDetails_WithMissingPartner_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setPartner(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Missing Partner."));
    }

    @Test
    void testValidateWithDetails_WithPartnerNotSupplyingMaterial_ReturnsError() {
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(false);
        List<String> errors = ownDemandService.validateWithDetails(validDemand);

        assertTrue(errors.contains("Partner does not supply the specified material."));
    }

    @Test
    void testValidateWithDetails_WithZeroQuantity_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setQuantity(0.0);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Quantity must be greater than 0."));
    }

    @Test
    void testValidateWithDetails_WithNegativeQuantity_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setQuantity(-10.0);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Quantity must be greater than 0."));
    }

    @Test
    void testValidateWithDetails_WithMissingMeasurementUnit_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setMeasurementUnit(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Missing measurement unit."));
    }

    @Test
    void testValidateWithDetails_WithMissingLastUpdatedOnDateTime_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setLastUpdatedOnDateTime(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Missing lastUpdatedOnTime."));
    }

    @Test
    void testValidateWithDetails_WithFutureLastUpdatedOnDateTime_ReturnsError() {
        OwnDemand demand = createValidDemand();
        Date futureDate = new Date(System.currentTimeMillis() + 86400000);
        demand.setLastUpdatedOnDateTime(futureDate);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("lastUpdatedOnDateTime cannot be in the future."));
    }

    @Test
    void testValidateWithDetails_WithMissingDay_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setDay(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Missing day."));
    }

    @Test
    void testValidateWithDetails_WithMissingDemandCategoryCode_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setDemandCategoryCode(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Missing demand category code."));
    }

    @Test
    void testValidateWithDetails_WithMissingDemandLocationBpns_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setDemandLocationBpns(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Missing demand location BPNS."));
    }

    @Test
    void testValidateWithDetails_WithSamePartnerAsOwn_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setPartner(ownPartner);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, ownPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Partner cannot be the same as own partner entity."));
    }

    @Test
    void testValidateWithDetails_WithInvalidDemandLocationBpns_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setDemandLocationBpns("INVALID-BPNS");

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Demand location BPNS must match one of the own partner entity's site BPNS."));
    }

    @Test
    void testValidateWithDetails_WithInvalidSupplierLocationBpns_ReturnsError() {
        OwnDemand demand = createValidDemand();
        demand.setSupplierLocationBpns("INVALID-SUPPLIER-BPNS");

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerSuppliesMaterial(testMaterial, supplierPartner)).thenReturn(true);
        List<String> errors = ownDemandService.validateWithDetails(demand);

        assertTrue(errors.contains("Supplier location BPNS must match one of the partner's site BPNS."));
    }

    @Test
    void testGetQuantityForDays_WithSpecificPartner_FiltersCorrectly() {
        String material = MATERIAL_NUMBER;
        Optional<String> partnerBpnl = Optional.of(SUPPLIER_BPNL);
        Optional<String> siteBpns = Optional.empty();
        int numberOfDays = 1;

        List<OwnDemand> demands = List.of(validDemand);
        ownDemandService = spy(ownDemandService);
        doReturn(demands).when(ownDemandService).findAllByFilters(eq(Optional.of(material)), eq(partnerBpnl), eq(siteBpns));
        List<Double> quantities = ownDemandService.getQuantityForDays(material, partnerBpnl, siteBpns, numberOfDays);

        assertEquals(1, quantities.size());
        verify(ownDemandService).findAllByFilters(eq(Optional.of(material)), eq(partnerBpnl), eq(siteBpns));
    }

    @Test
    void testGetQuantityForDays_WithSpecificSite_FiltersCorrectly() {
        String material = MATERIAL_NUMBER;
        Optional<String> partnerBpnl = Optional.empty();
        Optional<String> siteBpns = Optional.of(OWN_BPNS);
        int numberOfDays = 1;

        List<OwnDemand> demands = List.of(validDemand);
        ownDemandService = spy(ownDemandService);
        doReturn(demands).when(ownDemandService).findAllByFilters(eq(Optional.of(material)), eq(partnerBpnl), eq(siteBpns));
        List<Double> quantities = ownDemandService.getQuantityForDays(material, partnerBpnl, siteBpns, numberOfDays);

        assertEquals(1, quantities.size());
        verify(ownDemandService).findAllByFilters(eq(Optional.of(material)), eq(partnerBpnl), eq(siteBpns));
    }

    @Test
    void testGetQuantityForDays_WithDifferentMonths_FiltersCorrectly() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1);
        
        Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date nextMonthDate = Date.from(nextMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

        OwnDemand demandToday = new OwnDemand();
        demandToday.setDay(todayDate);
        demandToday.setQuantity(50.0);

        OwnDemand demandNextMonth = new OwnDemand();
        demandNextMonth.setDay(nextMonthDate);
        demandNextMonth.setQuantity(100.0);

        List<OwnDemand> allDemands = List.of(demandToday, demandNextMonth);

        ownDemandService = spy(ownDemandService);
        doReturn(allDemands).when(ownDemandService).findAllByFilters(any(), any(), any());
        List<Double> quantities = ownDemandService.getQuantityForDays(MATERIAL_NUMBER, Optional.empty(), Optional.empty(), 2);

        assertEquals(2, quantities.size());
        assertEquals(50.0, quantities.get(0));
        assertEquals(0.0, quantities.get(1));
    }
}