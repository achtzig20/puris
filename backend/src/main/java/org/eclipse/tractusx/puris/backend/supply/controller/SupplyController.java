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

package org.eclipse.tractusx.puris.backend.supply.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.supply.domain.model.Supply;
import org.eclipse.tractusx.puris.backend.supply.logic.dto.SupplyDto;
import org.eclipse.tractusx.puris.backend.supply.logic.service.CustomerSupplyService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@RestController
@RequestMapping("supply")
@Slf4j
public class SupplyController {
    @Autowired
    private CustomerSupplyService daysOfSupplyService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping()
    @ResponseBody
    @Operation(summary = "Get all planned days of supply for the given Material",
        description = "Get all planned days of supply for the given material number. Optionally the days of supply can be filtered by its partner bpnl.")
    public List<SupplyDto> getAllDaysOfSupply(String materialNumber, Optional<String> bpnl) {
        return daysOfSupplyService.findAllByFilters(Optional.of(materialNumber), bpnl)
            .stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    private SupplyDto convertToDto(Supply entity) {
        SupplyDto dto = modelMapper.map(entity, SupplyDto.class);

        dto.setOwnMaterialNumber(entity.getMaterial().getOwnMaterialNumber());
        
        return dto;
    }
}
