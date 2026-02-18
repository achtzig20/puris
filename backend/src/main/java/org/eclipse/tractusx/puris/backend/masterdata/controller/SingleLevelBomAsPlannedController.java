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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.SingleLevelBomAsPlannedSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlannedSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * This class offers the endpoint for requesting the SingleLevelBomAsPlanned
 * Submodel 1.0.0
 */
@RestController
@RequestMapping("single-level-bom-as-planned")
@Slf4j
public class SingleLevelBomAsPlannedController {
    static Pattern bpnlPattern = PatternStore.BPNL_PATTERN;
    static Pattern materialNumberPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @Autowired
    private MaterialService materialService;
    @Autowired
    private SingleLevelBomAsPlannedSammMapper sammMapper;
    @Autowired
    private org.eclipse.tractusx.puris.backend.common.util.VariablesService variablesService;

    @RequestMapping(value = "/**")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ResponseEntity<String> handleNotImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(description = "Endpoint that delivers SingleLevelBomAsPlanned of own products. " +
            "'materialnumber' must be set to the ownMaterialNumber. " +
            "This endpoint is meant for self-access only via EDC. ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters. ", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access forbidden - self-access only. ", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found for given parameters. ", content = @Content),
            @ApiResponse(responseCode = "501", description = "Unsupported representation requested. ", content = @Content)
    })
    @GetMapping("/{materialnumber}/submodel/{representation}")
    public ResponseEntity<SingleLevelBomAsPlannedSAMM> getMapping(@RequestHeader("edc-bpn") String bpnl,
            @Parameter(description = "The material number that the request receiving party uses for the material in question") @PathVariable String materialnumber,
            @Parameter(description = "Must be set to '$value'") @PathVariable String representation) {
        materialnumber = new String(Base64.getDecoder().decode(materialnumber.getBytes(StandardCharsets.UTF_8)));
        if (!bpnlPattern.matcher(bpnl).matches() || !materialNumberPattern.matcher(materialnumber).matches()) {
            return ResponseEntity.badRequest().build();
        }

        if (!"$value".equals(representation)) {
            return ResponseEntity.status(501).build();
        }

        // Self-access only - verify requesting BPNL is our own
        if (!bpnl.equals(variablesService.getOwnBpnl())) {
            log.warn("Access denied: {} attempted to access SingleLevelBomAsPlanned (self-access only)", bpnl);
            return ResponseEntity.status(403).build();
        }

        log.info("Self-request for single level bom as planned on {}", materialnumber);
        Material material = materialService.findByOwnMaterialNumber(materialnumber);
        if (material == null || !material.isProductFlag()) {
            return ResponseEntity.status(404).build();
        }

        var samm = sammMapper.materialToSamm(material);
        return ResponseEntity.ok(samm);
    }
}
