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

package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Generated class for Part Type Information. A Part Type Information represents
 * an item in the Catena-X Bill of Material (BOM) on a type level in a specific
 * version.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PartTypeInformationSAMM {

	@NotNull
	@Pattern(regexp = PatternStore.URN_STRING)
	private String catenaXId;

	@NotNull
	private PartTypeInformationBody partTypeInformation = new PartTypeInformationBody();
    @Valid
	private Set<PartSitesInformationAsPlanned> partSitesInformationAsPlanned = new HashSet<>();

	@JsonCreator
	public PartTypeInformationSAMM(@JsonProperty(value = "catenaXId") String catenaXId,
                                   @JsonProperty(value = "partTypeInformation") PartTypeInformationBody partTypeInformation,
                                   @JsonProperty(value = "partSitesInformationAsPlanned") Set<PartSitesInformationAsPlanned> partSitesInformationAsPlanned) {
		this.catenaXId = catenaXId;
		this.partTypeInformation = partTypeInformation;
		this.partSitesInformationAsPlanned = partSitesInformationAsPlanned;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final PartTypeInformationSAMM that = (PartTypeInformationSAMM) o;
		return Objects.equals(catenaXId, that.catenaXId)
				&& Objects.equals(partTypeInformation, that.partTypeInformation)
				&& Objects.equals(partSitesInformationAsPlanned, that.partSitesInformationAsPlanned);
	}

	@Override
	public int hashCode() {
		return Objects.hash(catenaXId, partTypeInformation, partSitesInformationAsPlanned);
	}
}
