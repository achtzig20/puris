/*
 * Copyright (c) 2026 Volkswagen AG
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.masterdata.controller;

import org.eclipse.tractusx.puris.backend.common.TestConfig;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.security.SecurityConfig;
import org.eclipse.tractusx.puris.backend.common.security.annotation.WithMockApiKey;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationProvider;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.SingleLevelBomAsPlannedSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlannedSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SingleLevelBomAsPlannedController.class)
@Import({SecurityConfig.class, ApiKeyAuthenticationProvider.class, DtrSecurityConfiguration.class, VariablesService.class, TestConfig.class})
class SingleLevelBomAsPlannedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VariablesService variablesService;

    @MockitoBean
    private MaterialService materialService;

    @MockitoBean
    private SingleLevelBomAsPlannedSammMapper sammMapper;

    private static final String OWN_BPNL = "BPNL4444444444XX"; // Default test BPNL from application.properties
    private static final String OTHER_BPNL = "BPNL1234567890ZZ";
    private static final String MATERIAL_NUMBER = "MNR-7307-AU340474.001";

    private String base64Encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Material createTestMaterial(boolean isProduct) {
        return Material.builder()
            .ownMaterialNumber(MATERIAL_NUMBER)
            .materialFlag(true)
            .materialNumberCx(UUID.randomUUID().toString())
            .name("Test Product")
            .productFlag(isProduct)
            .build();
    }

    @Test
    @WithMockApiKey
    void getMapping_ValidSelfAccess_Returns200() throws Exception {
        // given
        Material material = createTestMaterial(true);
        SingleLevelBomAsPlannedSAMM samm = new SingleLevelBomAsPlannedSAMM();

        when(materialService.findByOwnMaterialNumber(MATERIAL_NUMBER)).thenReturn(material);
        when(sammMapper.materialToSamm(material)).thenReturn(samm);

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                base64Encode(MATERIAL_NUMBER), "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isOk());

        verify(materialService).findByOwnMaterialNumber(MATERIAL_NUMBER);
        verify(sammMapper).materialToSamm(material);
    }

    @Test
    @WithMockApiKey
    void getMapping_OtherPartnerAccess_Returns403() throws Exception {
        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                base64Encode(MATERIAL_NUMBER), "$value")
                .header("edc-bpn", OTHER_BPNL)
        ).andExpect(status().isForbidden());

        // Verify that service methods are never called due to early auth check
        verify(materialService, never()).findByOwnMaterialNumber(any());
        verify(sammMapper, never()).materialToSamm(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_InvalidBpnlPattern_Returns400() throws Exception {
        // given
        String invalidBpnl = "INVALID-BPNL";

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                base64Encode(MATERIAL_NUMBER), "$value")
                .header("edc-bpn", invalidBpnl)
        ).andExpect(status().isBadRequest());

        verify(materialService, never()).findByOwnMaterialNumber(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_InvalidMaterialNumberPattern_Returns400() throws Exception {
        // given - base64 encoded vertical whitespace character (should fail pattern check)
        String invalidMaterialNumber = base64Encode("\u000B"); // vertical tab

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                invalidMaterialNumber, "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isBadRequest());

        verify(materialService, never()).findByOwnMaterialNumber(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_EmptyMaterialNumber_Returns400() throws Exception {
        // given
        String emptyMaterialNumber = base64Encode("");

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                emptyMaterialNumber, "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isBadRequest());

        verify(materialService, never()).findByOwnMaterialNumber(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_NonExistentMaterial_Returns404() throws Exception {
        // given
        when(materialService.findByOwnMaterialNumber(MATERIAL_NUMBER)).thenReturn(null);

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                base64Encode(MATERIAL_NUMBER), "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isNotFound());

        verify(materialService).findByOwnMaterialNumber(MATERIAL_NUMBER);
        verify(sammMapper, never()).materialToSamm(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_MaterialNotAProduct_ReturnsEmptyBom() throws Exception {
        // given
        Material material = createTestMaterial(false); // productFlag = false
        SingleLevelBomAsPlannedSAMM emptyBom = new SingleLevelBomAsPlannedSAMM();
        emptyBom.setCatenaXId(material.getMaterialNumberCx());
        emptyBom.setChildItems(new HashSet<>());

        when(materialService.findByOwnMaterialNumber(MATERIAL_NUMBER)).thenReturn(material);
        when(sammMapper.materialToSamm(material)).thenReturn(emptyBom);

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                base64Encode(MATERIAL_NUMBER), "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isOk())
         .andExpect(jsonPath("$.catenaXId").value(material.getMaterialNumberCx()))
         .andExpect(jsonPath("$.childItems").isEmpty());

        verify(materialService).findByOwnMaterialNumber(MATERIAL_NUMBER);
        verify(sammMapper).materialToSamm(material);
    }

    @Test
    @WithMockApiKey
    void getMapping_UnsupportedRepresentation_Returns501() throws Exception {
        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                base64Encode(MATERIAL_NUMBER), "unsupported")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isNotImplemented());

        verify(materialService, never()).findByOwnMaterialNumber(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_EmptyRepresentation_Returns501() throws Exception {
        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/{materialnumber}/submodel/{representation}",
                base64Encode(MATERIAL_NUMBER), "")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isNotImplemented());

        verify(materialService, never()).findByOwnMaterialNumber(any());
    }

    @Test
    @WithMockApiKey
    void handleNotImplemented_GivenUnmappedPath_Returns501() throws Exception {
        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/some/unmapped/path")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isNotImplemented());
    }
}
