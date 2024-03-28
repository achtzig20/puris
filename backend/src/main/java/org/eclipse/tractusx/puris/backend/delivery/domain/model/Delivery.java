package org.eclipse.tractusx.puris.backend.delivery.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public class Delivery {
    @Id
    @GeneratedValue
    protected UUID uuid;

    @ManyToOne()
    @JoinColumn(name = "partner_uuid")
    @ToString.Exclude
    @NotNull
    protected Partner partner;

    @ManyToOne()
    @JoinColumn(name = "material_ownMaterialNumber")
    @ToString.Exclude
    @NotNull
    protected Material material;

    // Order Position Reference
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String supplierOrderId;
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String customerOrderId;
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String customerOrderPositionId;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String trackingNumber;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
    private String incoterm;

    // measurementUnit class created -> wait for Rene's PR to be merged
    private double quantity;
//  private ItemUnitEnumeration measurementUnit;

    @NotNull
    private List<TransitEvent> transitEvents;

    // Transit Location
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String destinationBpns;
    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String destinationBpna;
    @Pattern(regexp = PatternStore.BPNS_STRING)
    private String originBpns;
    @Pattern(regexp = PatternStore.BPNA_STRING)
    private String originBpna;

    @ToString.Include
    private String material_ownMaterialNumber() {
        return material.getOwnMaterialNumber();
    }

    // endpoint /reported -> deliveries that partners have reported
}
