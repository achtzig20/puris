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

package org.eclipse.tractusx.puris.backend.delivery.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventTypeEnumeration {
    ESTIMATED_DEPARTURE("estimated-departure"),
    ACTUAL_DEPARTURE("actual-departure"),
    ESTIMATED_ARRIVAL("estimated-arrival"),
    ACTUAL_ARRIVAL("actual-arrival");

    private String value;

    EventTypeEnumeration(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static EventTypeEnumeration fromValue(String value) {
        for (EventTypeEnumeration event : EventTypeEnumeration.values()) {
            if (event.getValue().equals(value)) {
                return event;
            }
        }
        throw new IllegalArgumentException("Unknown event: " + value);
    }
}
