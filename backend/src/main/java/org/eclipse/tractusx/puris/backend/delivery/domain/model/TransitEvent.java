package org.eclipse.tractusx.puris.backend.delivery.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public class TransitEvent {
    @Id
    @GeneratedValue
    protected UUID uuid;

    private Date dateTimeOfEvent;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String eventType;
}
