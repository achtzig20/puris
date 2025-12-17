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

package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Objects;

/**
 * Generated class for Child Data. Catena-X ID and meta data of the assembled 
 * child item.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ChildData {

    @NotNull
    @Pattern(regexp = "^-?([1-9][0-9]{3,}|0[0-9]{3})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])(T(([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?|(24:00:00(\\.0+)?))(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?)?$")
    private String createdOn;

    @NotNull
    @Valid
    private ItemQuantity quantity;

    @Nullable
    @Pattern(regexp = "^-?([1-9][0-9]{3,}|0[0-9]{3})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])(T(([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?|(24:00:00(\\.0+)?))(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?)?$")
    private String lastModifiedOn;

    @Nullable
    @Valid
    private ValidityPeriodEntity validityPeriod;

    @NotNull
    @Pattern(regexp = PatternStore.BPNL_STRING)
    private String businessPartner;

    @NotNull
    @Pattern(regexp = PatternStore.URN_OR_UUID_STRING)
    private String catenaXId;

    @JsonCreator
    public ChildData(@JsonProperty(value = "createdOn") String createdOn,
                     @JsonProperty(value = "quantity") ItemQuantity quantity,
                     @JsonProperty(value = "lastModifiedOn") String lastModifiedOn,
                     @JsonProperty(value = "validityPeriod") ValidityPeriodEntity validityPeriod,
                     @JsonProperty(value = "businessPartner") String businessPartner,
                     @JsonProperty(value = "catenaXId") String catenaXId) {
        this.createdOn = createdOn;
        this.quantity = quantity;
        this.lastModifiedOn = lastModifiedOn;
        this.validityPeriod = validityPeriod;
        this.businessPartner = businessPartner;
        this.catenaXId = catenaXId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ChildData that = (ChildData) o;
        return Objects.equals(createdOn, that.createdOn)
                && Objects.equals(quantity, that.quantity)
                && Objects.equals(lastModifiedOn, that.lastModifiedOn)
                && Objects.equals(validityPeriod, that.validityPeriod)
                && Objects.equals(businessPartner, that.businessPartner)
                && Objects.equals(catenaXId, that.catenaXId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdOn, quantity, lastModifiedOn, validityPeriod, businessPartner, catenaXId);
    }
}
