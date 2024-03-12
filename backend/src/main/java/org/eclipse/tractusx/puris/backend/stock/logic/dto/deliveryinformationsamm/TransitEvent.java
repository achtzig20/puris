package org.eclipse.tractusx.puris.backend.stock.logic.dto.deliveryinformationsamm;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransitEvent {
    private Date dateTimeOfEvent;

    private String eventType;
}
