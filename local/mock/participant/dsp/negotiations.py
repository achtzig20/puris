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
"""Provider-side contract negotiation state machine (mock participant acting as DSP provider)."""

import asyncio
import logging
import uuid
from datetime import datetime, timezone
from typing import Optional

from dsp.common import CONTEXT_DSPACE, CONTEXT_DSPACE_ODRL_POLICY, build_permission, push_dsp_message, versioned_callback_base

logger = logging.getLogger("mock participant dsp negotiations")

_store: dict[str, dict] = {}


def create(body: dict) -> tuple[dict, str, str]:
    """Returns (response_body, neg_id, callback_address)."""
    neg_id = f"urn:uuid:{uuid.uuid4()}"
    agreement_id = f"urn:uuid:{uuid.uuid4()}"
    consumer_pid = body.get("consumerPid", f"urn:uuid:{uuid.uuid4()}")
    callback = body.get("callbackAddress", "")
    asset_id = _extract_asset_id(body)

    entry = {
        "agreementId": agreement_id,
        "assetId": asset_id,
        "consumerPid": consumer_pid,
        "callbackAddress": callback,
    }
    _store[neg_id] = entry
    return _requested(neg_id, consumer_pid), neg_id, callback


def get_state(neg_id: str) -> Optional[dict]:
    entry = _store.get(neg_id)
    if not entry:
        return None
    return _finalized(neg_id, entry["agreementId"], entry["consumerPid"])


def get_callback_address(neg_id: str) -> str:
    return _store.get(neg_id, {}).get("callbackAddress", "")


def _extract_asset_id(body: dict) -> str:
    offer = body.get("offer") or {}
    target = offer.get("target", {})
    if isinstance(target, dict):
        return target.get("@id", "")
    return str(target)


def _requested(neg_id: str, consumer_pid: str) -> dict:
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "ContractNegotiation",
        "@id": neg_id,
        "providerPid": neg_id,
        "consumerPid": consumer_pid,
        "state": "REQUESTED",
    }


def _finalized(neg_id: str, agreement_id: str, consumer_pid: str) -> dict:
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "ContractNegotiation",
        "@id": neg_id,
        "providerPid": neg_id,
        "consumerPid": consumer_pid,
        "state": "FINALIZED",
        "contractAgreementId": agreement_id,
    }


async def push_agreement_message(
    neg_id: str,
    callback_address: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
):
    """Send ContractAgreementMessage to consumer's DSP callback."""
    entry = _store.get(neg_id)
    if not entry or not callback_address:
        return

    await asyncio.sleep(0.5)

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    message = {
        "@context": CONTEXT_DSPACE_ODRL_POLICY,
        "@type": "ContractAgreementMessage",
        "providerPid": neg_id,
        "consumerPid": entry["consumerPid"],
        "agreement": {
            "@id": entry["agreementId"],
            "@type": "Agreement",
            "target": {"@id": entry["assetId"]},
            "timestamp": now,
            "assigner": f"did:web:wallet:{bpnl}",
            "assignee": f"did:web:wallet:{supplier_bpnl}",
            "permission": [build_permission()],
            "prohibition": [],
            "obligation": [],
        },
    }

    callback_url = f"{versioned_callback_base(callback_address)}/negotiations/{entry['consumerPid']}/agreement"
    await push_dsp_message("ContractAgreementMessage", callback_url, message, bpnl, supplier_bpnl, wallet_url, wallet_secret)


async def push_finalized_message(
    neg_id: str,
    callback_address: str,
    bpnl: str,
    supplier_bpnl: str,
    wallet_url: str,
    wallet_secret: str,
):
    """Send ContractNegotiationEventMessage (FINALIZED) to consumer's DSP callback."""
    entry = _store.get(neg_id)
    if not entry or not callback_address:
        return

    await asyncio.sleep(0.3)

    message = {
        "@context": CONTEXT_DSPACE,
        "@type": "ContractNegotiationEventMessage",
        "providerPid": neg_id,
        "consumerPid": entry["consumerPid"],
        "eventType": "FINALIZED",
    }

    callback_url = f"{versioned_callback_base(callback_address)}/negotiations/{entry['consumerPid']}/events"
    await push_dsp_message("ContractNegotiationEventMessage", callback_url, message, bpnl, supplier_bpnl, wallet_url, wallet_secret)
