/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.demand.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DemandCategoryEnumeration {
    DEMAND_DEFAULT("0001"),
    DEMAND_AFTER_SALES("A1S1"),
    DEMAND_SERIES("SR99"),
    DEMAND_PHASE_IN_PERIOD("PI01"),
    DEMAND_PHASE_OUT_PERIOD("PO01"),
    DEMAND_SINGLE_ORDER("OS01"),
    DEMAND_SMALL_SERIES("OI01"),
    DEMAND_EXTRAORDINARY_DEMAND("ED01");

    private String value;

    DemandCategoryEnumeration(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static DemandCategoryEnumeration fromValue(String value) {
        for (DemandCategoryEnumeration category : DemandCategoryEnumeration.values()) {
            if (category.getValue().equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
