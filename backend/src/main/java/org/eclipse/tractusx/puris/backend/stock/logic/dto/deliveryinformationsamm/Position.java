package org.eclipse.tractusx.puris.backend.stock.logic.dto.deliveryinformationsamm;

import java.util.Date;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class Position {
    @Valid
	private OrderPositionReference orderPositionReference;

    @NotNull
	private Date lastUpdatedOnDateTime;

    @Valid
    @NotNull
    private List<Delivery> deliveries;
}
