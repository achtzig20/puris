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

CONTEXT = [
    "https://w3id.org/dspace/2025/1/context.jsonld",
    {
        "cx-policy": "https://w3id.org/catenax/2025/9/policy/",
        "cx-common": "https://w3id.org/catenax/ontology/common#",
        "cx-taxo": "https://w3id.org/catenax/taxonomy#",
        "aas-semantics": "https://admin-shell.io/aas/3/0/HasSemantics/",
    },
]


def _offer_policy(offer_id: str, bpnl: str) -> dict:
    return {
        "@id": offer_id,
        "@type": "Offer",
        # Must match the "assigner" the eventual ContractAgreementMessage carries (see
        # negotiations.py's push_agreement_message) — the receiving EDC's PolicyEquality
        # compares the agreement's policy against this stored offer policy field-for-field
        # (only "@type"/"assignee"/"target" are excluded from that comparison), so a mismatched
        # or missing "assigner" here fails with "Policy in the contract agreement is not equal
        # to the one in the contract offer" once identity validation passes.
        "assigner": f"did:web:wallet:{bpnl}",
        "permission": [build_permission()],
        "prohibition": [],
        "obligation": [],
    }


def _distribution(service: dict) -> dict:
    return {
        "@type": "Distribution",
        "format": "HttpData-PULL",
        "accessService": service,
    }


def build(bpnl: str, base_url: str) -> dict:
    service = {
        "@id": f"urn:uuid:{uuid.uuid4()}",
        "@type": "DataService",
        "endpointDescription": "dspace:connector",
        "endpointURL": f"{base_url}/api/v1/dsp/2025-1",
    }
    datasets = []

    # DTR asset
    dtr_asset_id = f"DigitalTwinRegistryId@{bpnl}"
    datasets.append({
        "@id": dtr_asset_id,
        "@type": "Dataset",
        "dct:type": {"@id": "cx-taxo:DigitalTwinRegistry"},
        "cx-common:version": "3.0",
        "hasPolicy": [_offer_policy(f"offer-dtr-{uuid.uuid4()}", bpnl)],
        "distribution": [_distribution(service)],
    })

    for prefix, semantic_id, version in SUBMODELS:
        asset_id = f"{prefix}@{bpnl}"
        datasets.append({
            "@id": asset_id,
            "@type": "Dataset",
            "dct:type": {"@id": "cx-taxo:Submodel"},
            "cx-common:version": version,
            "aas-semantics:semanticId": {"@id": semantic_id},
            "hasPolicy": [_offer_policy(f"offer-{prefix}-{uuid.uuid4()}", bpnl)],
            "distribution": [_distribution(service)],
        })


    notification_asset_id = f"notification-api-asset@{bpnl}"
    datasets.append({
        "@id": notification_asset_id,
        "@type": "Dataset",
        "dct:type": {"@id": "cx-taxo:DemandAndCapacityNotificationApi"},
        "cx-common:version": "1.0",
        "aas-semantics:semanticId": {"@id": "urn:samm:io.catenax.demand_and_capacity_notification:3.0.0#DemandAndCapacityNotification"},
        "hasPolicy": [_offer_policy(f"offer-notification-{uuid.uuid4()}", bpnl)],
        "distribution": [_distribution(service)],
    })

    return {
        "@context": CONTEXT,
        "@type": "Catalog",
        "@id": f"urn:uuid:{uuid.uuid4()}",
        "participantId": bpnl,
        "dataset": datasets,
        "catalog": [],
        "distribution": [],
        "service": [service],
    }
