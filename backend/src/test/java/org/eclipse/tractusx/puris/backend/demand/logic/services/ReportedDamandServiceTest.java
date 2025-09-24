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
import static org.mockito.Mockito.*;

import java.util.*;

import org.eclipse.tractusx.puris.backend.demand.domain.model.DemandCategoryEnumeration;
import org.eclipse.tractusx.puris.backend.demand.domain.model.ReportedDemand;
import org.eclipse.tractusx.puris.backend.demand.domain.repository.ReportedDemandRepository;
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

class ReportedDemandServiceTest {

    @InjectMocks
    private ReportedDemandService reportedDemandService;

    @Mock
    private ReportedDemandRepository reportedDemandRepository;

    @Mock
    private PartnerService partnerService;

    @Mock
    private MaterialPartnerRelationService mprService;

    private Partner ownPartner;
    private Partner customerPartner;
    private Site ownSite;
    private Site customerSite;
    private Site additionalOwnSite;
    private Site additionalCustomerSite;
    private Material testMaterial;
    private ReportedDemand validDemand;

    private static final String OWN_BPNL = "BPNL4444444444XX";
    private static final String OWN_BPNS = "BPNS4444444444XX";
    private static final String OWN_ADDITIONAL_BPNS = "BPNS4444444455XX";
    private static final String CUSTOMER_BPNL = "BPNL1234567890ZZ";
    private static final String CUSTOMER_BPNS = "BPNS1234567890ZZ";
    private static final String CUSTOMER_ADDITIONAL_BPNS = "BPNS1234567855ZZ";
    private static final String MATERIAL_NUMBER = "MAT-001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ownSite = new Site();
        ownSite.setBpns(OWN_BPNS);
        ownSite.setName("Own Site");

        additionalOwnSite = new Site();
        additionalOwnSite.setBpns(OWN_ADDITIONAL_BPNS);
        additionalOwnSite.setName("Own Additional Site");

        customerSite = new Site();
        customerSite.setBpns(CUSTOMER_BPNS);
        customerSite.setName("Customer Site");

        additionalCustomerSite = new Site();
        additionalCustomerSite.setBpns(CUSTOMER_ADDITIONAL_BPNS);
        additionalCustomerSite.setName("Customer Additional Site");

        SortedSet<Site> ownSites = new TreeSet<>(Comparator.comparing(Site::getBpns));
        ownSites.add(ownSite);
        ownSites.add(additionalOwnSite);

        SortedSet<Site> customerSites = new TreeSet<>(Comparator.comparing(Site::getBpns));
        customerSites.add(customerSite);
        customerSites.add(additionalCustomerSite);

        ownPartner = new Partner();
        ownPartner.setBpnl(OWN_BPNL);
        ownPartner.setName("Own Partner");
        ownPartner.setSites(ownSites);
        ownPartner.setUuid(UUID.randomUUID());

        customerPartner = new Partner();
        customerPartner.setBpnl(CUSTOMER_BPNL);
        customerPartner.setName("Customer Partner");
        customerPartner.setSites(customerSites);
        customerPartner.setUuid(UUID.randomUUID());

        testMaterial = new Material();
        testMaterial.setOwnMaterialNumber(MATERIAL_NUMBER);
        testMaterial.setMaterialFlag(false);
        testMaterial.setProductFlag(true);
        testMaterial.setName("Test Product");

        validDemand = createValidDemand();
    }

    private ReportedDemand createValidDemand() {
        ReportedDemand demand = new ReportedDemand();
        demand.setMaterial(testMaterial);
        demand.setPartner(customerPartner);
        demand.setQuantity(100.0);
        demand.setMeasurementUnit(ItemUnitEnumeration.UNIT_PIECE);
        demand.setDay(new Date());
        demand.setDemandCategoryCode(DemandCategoryEnumeration.DEMAND_DEFAULT);
        demand.setDemandLocationBpns(CUSTOMER_BPNS);
        demand.setSupplierLocationBpns(OWN_BPNS);
        return demand;
    }

    @Test
    void testValidate_WithValidDemand_ReturnsTrue() {
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(validDemand);

        assertTrue(result);
        verify(partnerService).getOwnPartnerEntity();
        verify(mprService).partnerOrdersProduct(testMaterial, customerPartner);
    }

    @Test
    void testValidate_WithValidDemandAndNullSupplierLocation_ReturnsTrue() {
        ReportedDemand demand = createValidDemand();
        demand.setSupplierLocationBpns(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertTrue(result);
    }

    @Test
    void testValidate_WithValidDemandAndAlternativeOwnSite_ReturnsTrue() {
        ReportedDemand demand = createValidDemand();
        demand.setSupplierLocationBpns(OWN_ADDITIONAL_BPNS);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertTrue(result);
    }

    @Test
    void testValidate_WithValidDemandAndAlternativeCustomerSite_ReturnsTrue() {
        ReportedDemand demand = createValidDemand();
        demand.setDemandLocationBpns(CUSTOMER_ADDITIONAL_BPNS);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertTrue(result);
    }

    @Test
    void testValidate_WithNullMaterial_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setMaterial(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithNullPartner_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setPartner(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithPartnerNotOrderingProduct_ReturnsFalse() {
        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(false);

        boolean result = reportedDemandService.validate(validDemand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithZeroQuantity_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setQuantity(0.0);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithNegativeQuantity_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setQuantity(-10.0);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithNullMeasurementUnit_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setMeasurementUnit(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithNullDay_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setDay(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithNullDemandCategoryCode_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setDemandCategoryCode(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithNullDemandLocationBpns_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setDemandLocationBpns(null);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithPartnerEqualsOwnPartner_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setPartner(ownPartner);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, ownPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithInvalidSupplierLocationBpns_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setSupplierLocationBpns("INVALID-SUPPLIER-BPNS");

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithInvalidDemandLocationBpns_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setDemandLocationBpns("INVALID-DEMAND-BPNS");

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartner)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithEmptySitesOnCustomerPartner_ReturnsFalse() {
        ReportedDemand demand = createValidDemand();
        demand.setDemandLocationBpns(CUSTOMER_BPNS);

        Partner customerPartnerWithoutSites = new Partner();
        customerPartnerWithoutSites.setBpnl(CUSTOMER_BPNL);
        customerPartnerWithoutSites.setName("Customer Partner");
        customerPartnerWithoutSites.setSites(new TreeSet<>());
        demand.setPartner(customerPartnerWithoutSites);

        when(partnerService.getOwnPartnerEntity()).thenReturn(ownPartner);
        when(mprService.partnerOrdersProduct(testMaterial, customerPartnerWithoutSites)).thenReturn(true);

        boolean result = reportedDemandService.validate(demand);

        assertFalse(result);
    }

    @Test
    void testValidate_WithPartnerServiceReturningNull_ReturnsFalse() {
        when(partnerService.getOwnPartnerEntity()).thenReturn(null);
        boolean result = reportedDemandService.validate(validDemand);

        assertFalse(result);
        verify(partnerService).getOwnPartnerEntity();
        verify(mprService, never()).partnerOrdersProduct(any(), any());
    }
}