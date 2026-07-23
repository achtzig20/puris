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
"""Provider-side transfer process state machine (tier2 acting as DSP provider, HttpData-PULL)."""

import asyncio
import uuid
from typing import Optional

from dsp.common import CONTEXT_DSPACE, push_dsp_message, versioned_callback_base

_store: dict[str, dict] = {}

FIXED_TOKEN = "tier2-mock-token"


def create(body: dict, base_url: str) -> tuple[dict, str, str]:
    """Returns (response_body, provider_pid, callback_address)."""
    provider_pid = f"urn:uuid:{uuid.uuid4()}"
    consumer_pid = body.get("consumerPid", f"urn:uuid:{uuid.uuid4()}")
    callback = body.get("callbackAddress", "")

    _store[provider_pid] = {
        "state": "STARTED",
        "consumerPid": consumer_pid,
        "callbackAddress": callback,
        "endpoint": f"{base_url}/api/public",
        "token": FIXED_TOKEN,
    }

    response = {
        "@context": CONTEXT_DSPACE,
        "@type": "TransferProcess",
        "@id": provider_pid,
        "providerPid": provider_pid,
        "consumerPid": consumer_pid,
        "state": "REQUESTED",
    }
    return response, provider_pid, callback


def get_state(transfer_id: str) -> Optional[dict]:
    entry = _store.get(transfer_id)
    if not entry:
        return None
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "TransferProcess",
        "@id": transfer_id,
        "providerPid": transfer_id,
        "consumerPid": entry["consumerPid"],
        "state": "STARTED",
        "dataAddress": _data_address(entry["endpoint"], entry["token"]),
    }


def _data_address(endpoint: str, token: str) -> dict:
    bearer = f"Bearer {token}"
    return {
        "@context": [
            "https://w3id.org/dspace/2025/1/context.jsonld",
            {
                "edc": "https://w3id.org/edc/v0.0.1/ns/",
                "tx-auth": "https://w3id.org/tractusx/auth/",
            },
        ],
        "@type": "DataAddress",
        "endpointType": "https://w3id.org/idsa/v4.1/HTTP",
        "endpoint": endpoint,
        "endpointProperties": [
            {
                "@type": "EndpointProperty",
                "name": "https://w3id.org/edc/v0.0.1/ns/endpoint",
                "value": endpoint,
            },
            {
                "@type": "EndpointProperty",
                "name": "https://w3id.org/edc/v0.0.1/ns/authorization",
                "value": bearer,
            },
            {
                "@type": "EndpointProperty",
                "name": "authType",
                "value": "bearer",
            },
        ],
    }


async def push_start_message(
    provider_pid: str,
    callback_address: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
):
    """Send TransferStartMessage to consumer's callback address."""
    entry = _store.get(provider_pid)
    if not entry or not callback_address:
        return

    await asyncio.sleep(0.5)

    message = {
        "@context": CONTEXT_DSPACE,
        "@type": "TransferStartMessage",
        "providerPid": provider_pid,
        "consumerPid": entry["consumerPid"],
        "dataAddress": _data_address(entry["endpoint"], entry["token"]),
    }

    callback_url = f"{versioned_callback_base(callback_address)}/transfers/{entry['consumerPid']}/start"
    await push_dsp_message("TransferStartMessage", callback_url, message, bpnl, supplier_bpnl, wallet_url, wallet_secret)
