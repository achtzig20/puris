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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private static final String MATERIAL_CX_NUMBER = "860fb504-b884-4009-9313-c6fb6cdc776b";

    private Material createTestMaterial(boolean isProduct) {
        return Material.builder()
            .ownMaterialNumber(MATERIAL_NUMBER)
            .materialFlag(true)
            .materialNumberCx(MATERIAL_CX_NUMBER)
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

        when(materialService.findByMaterialNumberCx(MATERIAL_CX_NUMBER)).thenReturn(material);
        when(sammMapper.materialToSamm(material)).thenReturn(samm);

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/request/{materialnumber}/submodel/{representation}",
                MATERIAL_CX_NUMBER, "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isOk());

        verify(materialService).findByMaterialNumberCx(MATERIAL_CX_NUMBER);
        verify(sammMapper).materialToSamm(material);
    }

    @Test
    @WithMockApiKey
    void getMapping_OtherPartnerAccess_Returns403() throws Exception {
        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/request/{materialnumber}/submodel/{representation}",
                MATERIAL_CX_NUMBER, "$value")
                .header("edc-bpn", OTHER_BPNL)
        ).andExpect(status().isForbidden());

        verify(materialService, never()).findByMaterialNumberCx(any());
        verify(sammMapper, never()).materialToSamm(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_InvalidBpnlPattern_Returns400() throws Exception {
        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/request/{materialnumber}/submodel/{representation}",
                MATERIAL_CX_NUMBER, "$value")
                .header("edc-bpn", "INVALID-BPNL")
        ).andExpect(status().isBadRequest());

        verify(materialService, never()).findByMaterialNumberCx(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_NonExistentMaterial_Returns404() throws Exception {
        // given
        when(materialService.findByMaterialNumberCx(MATERIAL_CX_NUMBER)).thenReturn(null);

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/request/{materialnumber}/submodel/{representation}",
                MATERIAL_CX_NUMBER, "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isNotFound());

        verify(materialService).findByMaterialNumberCx(MATERIAL_CX_NUMBER);
        verify(sammMapper, never()).materialToSamm(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_MaterialNotAProduct_Returns404() throws Exception {
        // given
        Material material = createTestMaterial(false); // productFlag = false

        when(materialService.findByMaterialNumberCx(MATERIAL_CX_NUMBER)).thenReturn(material);

        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/request/{materialnumber}/submodel/{representation}",
                MATERIAL_CX_NUMBER, "$value")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isNotFound());

        verify(materialService).findByMaterialNumberCx(MATERIAL_CX_NUMBER);
        verify(sammMapper, never()).materialToSamm(any());
    }

    @Test
    @WithMockApiKey
    void getMapping_UnsupportedRepresentation_Returns501() throws Exception {
        // when/then
        mockMvc.perform(
            get("/single-level-bom-as-planned/request/{materialnumber}/submodel/{representation}",
                MATERIAL_CX_NUMBER, "unsupported")
                .header("edc-bpn", OWN_BPNL)
        ).andExpect(status().isNotImplemented());

        verify(materialService, never()).findByMaterialNumberCx(any());
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
