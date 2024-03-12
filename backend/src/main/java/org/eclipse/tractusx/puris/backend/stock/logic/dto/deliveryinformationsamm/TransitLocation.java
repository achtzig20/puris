package org.eclipse.tractusx.puris.backend.stock.logic.dto.deliveryinformationsamm;

import jakarta.validation.constraints.NotNull;

public class TransitLocation {
    @NotNull
    private Destination destination;

    @NotNull
    private Origin origin;
}
