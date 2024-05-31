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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.tractusx.puris.backend.delivery.logic.dto.DeliveryQuantityDto;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.ReportedDeliveryService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialServiceImpl;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.model.ReportedCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.repository.ReportedCustomerSupplyRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomerSupplyService {
    private final ReportedCustomerSupplyRepository repository;
    private final OwnDeliveryService ownDeliveryService;
    private final ReportedDeliveryService reportedDeliveryService;
    private final OwnDemandService demandService;
    private final MaterialItemStockService stockService;
    private final MaterialServiceImpl materialService;

    protected final Function<ReportedCustomerSupply, Boolean> validator;

    public CustomerSupplyService(
        ReportedCustomerSupplyRepository customerRepository,
        OwnDeliveryService ownDeliveryService,
        ReportedDeliveryService reportedDeliveryService,
        OwnDemandService demandService,
        MaterialItemStockService stockService,
        MaterialServiceImpl materialService) {
        this.repository = customerRepository;
        this.ownDeliveryService = ownDeliveryService;
        this.reportedDeliveryService = reportedDeliveryService;
        this.demandService = demandService;
        this.stockService = stockService;
        this.materialService = materialService;
        this.validator = this::validate;
    }

    public final List<OwnCustomerSupply> calculateCustomerDaysOfSupply(String material, String partnerBpnl, String siteBpns, int numberOfDays) {
        List<OwnCustomerSupply> customerSupply = new ArrayList<>();
        LocalDate localDate = LocalDate.now();

        List<Double> demands = demandService.getQuantityForDays(material, partnerBpnl, siteBpns, numberOfDays);
        log.info("demands: " + demands);

        List<DeliveryQuantityDto> ownDeliveries = ownDeliveryService.getQuantityForDays(material, partnerBpnl, siteBpns, true, numberOfDays);
        List<DeliveryQuantityDto> reportedDeliveries = reportedDeliveryService.getQuantityForDays(material, partnerBpnl, siteBpns, true, numberOfDays);
        List<Double> deliveries = mergeDeliveries(ownDeliveries, reportedDeliveries);
        log.info("deliveries: " + deliveries);

        double stockQuantity = stockService.getInitialStockQuantity(material, partnerBpnl, siteBpns);
        log.info("stock qty: " + stockQuantity);

        for (int i = 0; i < numberOfDays; i++) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (i == numberOfDays - 1) {
                stockQuantity += deliveries.get(i);
            }

            double daysOfSupply = getDaysOfSupply(
                stockQuantity,
                demands.subList(i, demands.size()));

            OwnCustomerSupply supply = new OwnCustomerSupply();
            supply.setMaterial(materialService.findByOwnMaterialNumber(material));
            supply.setDate(date);
            supply.setDaysOfSupply(daysOfSupply);
            customerSupply.add(supply);

            stockQuantity = stockQuantity - demands.get(i) + deliveries.get(i);

            localDate = localDate.plusDays(1);
        }

        return customerSupply;
    }
    
    public final List<ReportedCustomerSupply> findAll() {
        return repository.findAll();
    }

    public final ReportedCustomerSupply findById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public final List<ReportedCustomerSupply> findAllByFilters(Optional<String> ownMaterialNumber, Optional<String> bpnl) {
        Stream<ReportedCustomerSupply> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(dayOfSupply -> dayOfSupply.getPartner().getBpnl().equals(bpnl.get()));
        }
        return stream.toList();
    }

    public boolean validate(ReportedCustomerSupply daysOfSupply) {
        return 
            daysOfSupply.getPartner() != null &&
            daysOfSupply.getMaterial() != null &&
            daysOfSupply.getDate() != null &&
            daysOfSupply.getStockLocationBPNA() != null &&
            daysOfSupply.getStockLocationBPNS() != null;
    }

    private final double getDaysOfSupply(double stockQuantity, List<Double> demands) {
        double daysOfSupply = 0;

        for (int i = 0; i < demands.size(); i++) {
            double demand = demands.get(i);

            if (stockQuantity >= demand) {
                daysOfSupply += 1;
                stockQuantity = stockQuantity - demand;
            } else if (stockQuantity < demand && stockQuantity > 0) {
                double fractional = stockQuantity / demand;
                daysOfSupply = daysOfSupply + fractional;
                break;
            } else {
                break;
            }
        }
        return daysOfSupply;
    }

    private List<Double> mergeDeliveries(List<DeliveryQuantityDto> list1, List<DeliveryQuantityDto> list2) {
        TreeMap<LocalDate, Double> dateQuantityMap = new TreeMap<>();

        // Helper method to convert Date to LocalDate (only the day part)
        Function<Date, LocalDate> dateToLocalDate = date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        for (DeliveryQuantityDto dq : list1) {
            LocalDate localDate = dateToLocalDate.apply(dq.getDate());
            dateQuantityMap.merge(localDate, dq.getQuantity(), Double::sum);
        }

        for (DeliveryQuantityDto dq : list2) {
            LocalDate localDate = dateToLocalDate.apply(dq.getDate());
            dateQuantityMap.merge(localDate, dq.getQuantity(), Double::sum);
        }

        return new ArrayList<>(dateQuantityMap.values());
    }
}
