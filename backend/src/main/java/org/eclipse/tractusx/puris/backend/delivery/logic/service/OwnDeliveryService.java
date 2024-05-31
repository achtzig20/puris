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

package org.eclipse.tractusx.puris.backend.delivery.logic.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.OwnDelivery;
import org.eclipse.tractusx.puris.backend.delivery.domain.repository.OwnDeliveryRepository;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.DeliveryQuantityDto;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OwnDeliveryService {
    private final OwnDeliveryRepository repository;

    private final PartnerService partnerService;

    protected final Function<OwnDelivery, Boolean> validator;

    private Partner ownPartnerEntity;

    public OwnDeliveryService(OwnDeliveryRepository repository, PartnerService partnerService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.validator = this::validate;
    }

    public final List<OwnDelivery> findAll() {
        return repository.findAll();
    }

    public final List<OwnDelivery> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(delivery -> delivery.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final List<OwnDelivery> findAllByOwnMaterialNumber(String ownMaterialNumber) {
        return repository.findAll().stream().filter(delivery -> delivery.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber))
                .toList();
    }

    public final List<OwnDelivery> findAllByFilters(
        Optional<String> ownMaterialNumber,
        Optional<String> bpns,
        Optional<String> bpnl,
        Optional<Date> day,
        Optional<Boolean> incoming) {
        Stream<OwnDelivery> stream = repository.findAll().stream();
        if (ownMaterialNumber.isPresent()) {
            stream = stream.filter(delivery -> delivery.getMaterial().getOwnMaterialNumber().equals(ownMaterialNumber.get()));
        }
        if (bpns.isPresent()) {
            if (incoming.isPresent()) {
                if (incoming.get() == true) {
                    stream = stream.filter(delivery -> delivery.getDestinationBpns().equals(bpns.get()));
                } else {
                    stream = stream.filter(delivery -> delivery.getOriginBpns().equals(bpns.get()));
                }
            } else {
                stream = stream.filter(delivery -> delivery.getDestinationBpns().equals(bpns.get()) || delivery.getOriginBpns().equals(bpns.get()));
            }
        }
        if (bpnl.isPresent()) {
            stream = stream.filter(delivery -> delivery.getPartner().getBpnl().equals(bpnl.get()));
        }
        if (day.isPresent()) {
            LocalDate localDayDate = Instant.ofEpochMilli(day.get().getTime())
                .atOffset(ZoneOffset.UTC)
                .toLocalDate();
            stream = stream.filter(delivery -> {
                long time = incoming.get() ? delivery.getDateOfArrival().getTime() : delivery.getDateOfDeparture().getTime();
                LocalDate deliveryDayDate = Instant.ofEpochMilli(time)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate();
                return deliveryDayDate.getDayOfMonth() == localDayDate.getDayOfMonth();
            });
        }
        return stream.toList();
    }

    public final OwnDelivery findById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public final double getSumOfQuantities(List<OwnDelivery> deliveries) {
        double sum = 0;
        for (OwnDelivery delivery : deliveries) {
            sum += delivery.getQuantity();
        }
        return sum;
    }

    public final List<DeliveryQuantityDto> getQuantityForDays(String material, String partnerBpnl, String siteBpns, boolean incoming, int numberOfDays) {
        List<DeliveryQuantityDto> deliveryQtys = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        for (int i = 0; i < numberOfDays; i++) {
            Date date = calendar.getTime();
            List<OwnDelivery> deliveries = findAllByFilters(Optional.of(material), Optional.of(partnerBpnl), Optional.of(siteBpns), Optional.of(date), Optional.of(incoming));
            double deliveryQuantity = getSumOfQuantities(deliveries);
            deliveryQtys.add(new DeliveryQuantityDto(date, deliveryQuantity));

            calendar.add(Calendar.DAY_OF_MONTH, 1);
            date = calendar.getTime();
        }
        return deliveryQtys;
    }

    public final OwnDelivery create(OwnDelivery delivery) {
        if (!validator.apply(delivery)) {
            throw new IllegalArgumentException("Invalid delivery");
        }
        if (delivery.getUuid() != null && repository.findById(delivery.getUuid()).isPresent()) {
            throw new KeyAlreadyExistsException("Delivery already exists");
        }
        return repository.save(delivery);
    }

    public final List<OwnDelivery> createAll(List<OwnDelivery> deliveries) {
        if (deliveries.stream().anyMatch(delivery -> !validator.apply(delivery))) {
            throw new IllegalArgumentException("Invalid delivery");
        }
        if (repository.findAll().stream()
                .anyMatch(existing -> deliveries.stream().anyMatch(delivery -> delivery.equals(existing)))) {
            throw new KeyAlreadyExistsException("delivery already exists");
        }
        return repository.saveAll(deliveries);
    }

    public final OwnDelivery update(OwnDelivery delivery) {
        if (delivery.getUuid() == null || repository.findById(delivery.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(delivery);
    }

    public final void delete(UUID id) {
        repository.deleteById(id);
    }

    // public boolean validate(OwnDelivery delivery) {
    //     if (ownPartnerEntity == null) {
    //         ownPartnerEntity = partnerService.getOwnPartnerEntity();
    //     }
    //     return 
    //         delivery.getQuantity() >= 0 && 
    //         delivery.getMeasurementUnit() != null &&
    //         delivery.getMaterial() != null &&
    //         delivery.getPartner() != null &&
    //         validateResponsibility(delivery) &&
    //         validateTransitEvent(delivery) &&
    //         !delivery.getPartner().equals(ownPartnerEntity) &&
    //         ((
    //             delivery.getCustomerOrderNumber() != null && 
    //             delivery.getCustomerOrderPositionNumber() != null
    //         ) || (
    //             delivery.getCustomerOrderNumber() == null && 
    //             delivery.getCustomerOrderPositionNumber() == null &&
    //             delivery.getSupplierOrderNumber() == null
    //         ));
    // }

    public boolean validate(OwnDelivery delivery) {
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }

        boolean quantityValid = delivery.getQuantity() >= 0;
        log.info("Quantity valid: {}", quantityValid);

        boolean measurementUnitValid = delivery.getMeasurementUnit() != null;
        log.info("Measurement unit valid: {}", measurementUnitValid);

        boolean materialValid = delivery.getMaterial() != null;
        log.info("Material valid: {}", materialValid);

        boolean partnerValid = delivery.getPartner() != null;
        log.info("Partner valid: {}", partnerValid);

        boolean responsibilityValid = validateResponsibility(delivery);
        log.info("Responsibility valid: {}", responsibilityValid);

        boolean transitEventValid = validateTransitEvent(delivery);
        log.info("Transit event valid: {}", transitEventValid);

        boolean partnerNotEqual = !delivery.getPartner().equals(ownPartnerEntity);
        log.info("Partner not equal to own entity: {}", partnerNotEqual);

        boolean orderNumberValid = (delivery.getCustomerOrderNumber() != null && delivery.getCustomerOrderPositionNumber() != null)
                || (delivery.getCustomerOrderNumber() == null && delivery.getCustomerOrderPositionNumber() == null && delivery.getSupplierOrderNumber() == null);
        log.info("Order number valid: {}", orderNumberValid);

        return quantityValid && measurementUnitValid && materialValid && partnerValid && responsibilityValid && transitEventValid && partnerNotEqual && orderNumberValid;
    }

    // private boolean validateTransitEvent(OwnDelivery delivery) {
    //     var now = new Date().getTime();
    //     return
    //         delivery.getDepartureType() != null &&
    //         (delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE || delivery.getDepartureType() == EventTypeEnumeration.ACTUAL_DEPARTURE) &&
    //         delivery.getArrivalType() != null &&
    //         (delivery.getArrivalType() == EventTypeEnumeration.ESTIMATED_ARRIVAL || delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL) &&
    //         !(delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE && delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL) &&
    //         delivery.getDateOfDeparture().getTime() < delivery.getDateOfArrival().getTime() && 
    //         (delivery.getArrivalType() != EventTypeEnumeration.ACTUAL_ARRIVAL || delivery.getDateOfArrival().getTime() < now) &&
    //         (delivery.getDepartureType() != EventTypeEnumeration.ACTUAL_DEPARTURE || delivery.getDateOfDeparture().getTime() < now);
    // }
    private boolean validateTransitEvent(OwnDelivery delivery) {
        var now = new Date().getTime();

        boolean departureTypeValid = delivery.getDepartureType() != null &&
                (delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE || delivery.getDepartureType() == EventTypeEnumeration.ACTUAL_DEPARTURE);
        log.info("Departure type valid: {}", departureTypeValid);

        boolean arrivalTypeValid = delivery.getArrivalType() != null &&
                (delivery.getArrivalType() == EventTypeEnumeration.ESTIMATED_ARRIVAL || delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL);
        log.info("Arrival type valid: {}", arrivalTypeValid);

        boolean invalidCombination = !(delivery.getDepartureType() == EventTypeEnumeration.ESTIMATED_DEPARTURE && delivery.getArrivalType() == EventTypeEnumeration.ACTUAL_ARRIVAL);
        log.info("Invalid combination (ESTIMATED_DEPARTURE and ACTUAL_ARRIVAL): {}", invalidCombination);

        boolean departureBeforeArrival = delivery.getDateOfDeparture().getTime() < delivery.getDateOfArrival().getTime();
        log.info("Departure before arrival: {}", departureBeforeArrival);

        boolean actualArrivalBeforeNow = delivery.getArrivalType() != EventTypeEnumeration.ACTUAL_ARRIVAL || delivery.getDateOfArrival().getTime() < now;
        log.info("Actual arrival before now: {}", actualArrivalBeforeNow);

        boolean actualDepartureBeforeNow = delivery.getDepartureType() != EventTypeEnumeration.ACTUAL_DEPARTURE || delivery.getDateOfDeparture().getTime() < now;
        log.info("Actual departure before now: {}", actualDepartureBeforeNow);

        return departureTypeValid && arrivalTypeValid && invalidCombination && departureBeforeArrival && actualArrivalBeforeNow && actualDepartureBeforeNow;
    }

    private boolean validateResponsibility(OwnDelivery delivery) {
        // if (ownPartnerEntity == null) {
        //     ownPartnerEntity = partnerService.getOwnPartnerEntity();
        // }
        // return delivery.getIncoterm() != null && switch (delivery.getIncoterm().getResponsibility()) {
        //     case SUPPLIER ->
        //         delivery.getMaterial().isProductFlag() &&
        //         ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
        //         delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
        //     case CUSTOMER ->
        //         delivery.getMaterial().isMaterialFlag() &&
        //         delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
        //         ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
        //     case PARTIAL ->
        //         (
        //             delivery.getMaterial().isProductFlag() &&
        //             ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
        //             delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))
        //         ) || (
        //             delivery.getMaterial().isMaterialFlag() &&
        //             delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
        //             ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()))
        //         );
        // };
        if (ownPartnerEntity == null) {
            ownPartnerEntity = partnerService.getOwnPartnerEntity();
        }

        boolean incotermValid = delivery.getIncoterm() != null;
        log.info("Incoterm valid: {}", incotermValid);

        boolean responsibilityValid = false;

        if (incotermValid) {
            switch (delivery.getIncoterm().getResponsibility()) {
                case SUPPLIER:
                    responsibilityValid = delivery.getMaterial().isProductFlag() &&
                            ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                            delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
                    break;
                case CUSTOMER:
                    responsibilityValid = delivery.getMaterial().isMaterialFlag() &&
                            delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                            ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns()));
                    break;
                case PARTIAL:
                    responsibilityValid = (delivery.getMaterial().isProductFlag() &&
                            ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                            delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns())))
                            || (delivery.getMaterial().isMaterialFlag() &&
                            delivery.getPartner().getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getOriginBpns())) &&
                            ownPartnerEntity.getSites().stream().anyMatch(site -> site.getBpns().equals(delivery.getDestinationBpns())));
                    break;
            }
            log.info("Responsibility valid: {}", responsibilityValid);
        }

        return incotermValid && responsibilityValid;
    }
}
