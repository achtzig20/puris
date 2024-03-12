package org.eclipse.tractusx.puris.backend.stock.logic.dto.deliveryinformationsamm;

import java.util.Objects;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class OrderPositionReference {
    
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
	private String supplierOrderId;

	@NotNull
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
	private String customerOrderId;

	@NotNull
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
	private String customerOrderPositionId;

	@JsonCreator
	public OrderPositionReference(@JsonProperty(value = "supplierOrderId") String supplierOrderId,
			@JsonProperty(value = "customerOrderId") String customerOrderId,
			@JsonProperty(value = "customerOrderPositionId") String customerOrderPositionId) {
		this.supplierOrderId = supplierOrderId;
		this.customerOrderId = customerOrderId;
		this.customerOrderPositionId = customerOrderPositionId;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final OrderPositionReference that = (OrderPositionReference) o;
		return Objects.equals(supplierOrderId, that.supplierOrderId)
				&& Objects.equals(customerOrderId, that.customerOrderId)
				&& Objects.equals(customerOrderPositionId, that.customerOrderPositionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(supplierOrderId, customerOrderId, customerOrderPositionId);
	}
}
