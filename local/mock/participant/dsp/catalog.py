#
# Copyright (c) 2026 Volkswagen AG
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
#
"""Builds the DSP catalog (DCAT dataset list) that tier2 offers as a provider."""

import uuid

from data import SUBMODELS
from dsp.common import build_permission

CONTEXT = {
    "cx-policy": "https://w3id.org/catenax/2025/9/policy/",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "dspace": "https://w3id.org/dspace/v0.8/",
    "cx-common": "https://w3id.org/catenax/ontology/common#",
    "cx-taxo": "https://w3id.org/catenax/taxonomy#",
    "aas-semantics": "https://admin-shell.io/aas/3/0/HasSemantics/",
}


def _offer_policy(offer_id: str) -> dict:
    return {
        "@id": offer_id,
        "@type": "odrl:Offer",
        "odrl:permission": build_permission(),
        "odrl:prohibition": [],
        "odrl:obligation": [],
    }


def _distribution(service: dict) -> dict:
    return {
        "@type": "dcat:Distribution",
        "dct:format": {"@id": "HttpData-PULL"},
        "dcat:accessService": service,
    }


def build(bpnl: str, base_url: str) -> dict:
    service = {
        "@id": f"urn:uuid:{uuid.uuid4()}",
        "@type": "dcat:DataService",
        "dcat:endpointDescription": "dspace:connector",
        "dcat:endpointURL": f"{base_url}/api/v1/dsp",
    }
    datasets = []

    # DTR asset
    dtr_asset_id = f"DigitalTwinRegistryId@{bpnl}"
    datasets.append({
        "@id": dtr_asset_id,
        "@type": "dcat:Dataset",
        "dct:type": {"@id": "cx-taxo:DigitalTwinRegistry"},
        "cx-common:version": "3.0",
        "odrl:hasPolicy": _offer_policy(f"offer-dtr-{uuid.uuid4()}"),
        "dcat:distribution": [_distribution(service)],
    })

    for prefix, semantic_id, version in SUBMODELS:
        asset_id = f"{prefix}@{bpnl}"
        datasets.append({
            "@id": asset_id,
            "@type": "dcat:Dataset",
            "dct:type": {"@id": "cx-taxo:Submodel"},
            "cx-common:version": version,
            "aas-semantics:semanticId": {"@id": semantic_id},
            "odrl:hasPolicy": _offer_policy(f"offer-{prefix}-{uuid.uuid4()}"),
            "dcat:distribution": [_distribution(service)],
        })


    notification_asset_id = f"notification-api-asset@{bpnl}"
    datasets.append({
        "@id": notification_asset_id,
        "@type": "dcat:Dataset",
        "dct:type": {"@id": "cx-taxo:DemandAndCapacityNotificationApi"},
        "cx-common:version": "1.0",
        "odrl:hasPolicy": _offer_policy(f"offer-notification-{uuid.uuid4()}"),
        "dcat:distribution": [_distribution(service)],
    })

    return {
        "@context": CONTEXT,
        "@type": "dcat:Catalog",
        "@id": f"urn:uuid:{uuid.uuid4()}",
        "dspace:participantId": bpnl,
        "dcat:dataset": datasets,
        "dcat:catalog": [],
        "dcat:distribution": [],
        "dcat:service": service,
    }
