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

package org.eclipse.tractusx.puris.backend.supply.logic.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedSupplierDaysOfSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.repository.ReportedSupplierDaysOfSupplyRepository;
import org.springframework.stereotype.Service;

@Service
public class SupplierDaysOfSupplyService {
    private final ReportedSupplierDaysOfSupplyRepository repository; 

    protected final Function<ReportedSupplierDaysOfSupply, Boolean> validator;

    public SupplierDaysOfSupplyService(ReportedSupplierDaysOfSupplyRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final List<ReportedSupplierDaysOfSupply> findAll() {
        return repository.findAll();
    }

    public final ReportedSupplierDaysOfSupply findById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public final List<ReportedSupplierDaysOfSupply> findAllByFilters(Optional<String> ownMaterialNumber, Optional<String> bpnl) {
        Stream<ReportedSupplierDaysOfSupply> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getPartner().getBpnl().equals(bpnl.get()));
        } 
        return stream.toList();
    }

    public boolean validate(ReportedSupplierDaysOfSupply daysOfSupply) {
        return 
            daysOfSupply.getPartner() != null &&
            daysOfSupply.getMaterial() != null &&
            daysOfSupply.getDate() != null &&
            daysOfSupply.getStockLocationBPNA() != null &&
            daysOfSupply.getStockLocationBPNS() != null;
    }
}
