package org.eclipse.tractusx.puris.backend.stock.logic.dto.deliveryinformationsamm;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class Delivery {
    @NotNull
    private DeliveryQuantity deliveryQuantity;

    @NotNull
    private List<TransitEvent> transitEvents;

    @NotNull
    private String trackingNumber;

    @NotNull
    private String incoterm;

    @NotNull
    private List<TransitLocation> transitLocations;
}
