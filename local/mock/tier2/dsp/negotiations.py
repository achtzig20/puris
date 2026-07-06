import asyncio
import logging
import uuid
from datetime import datetime, timezone
from typing import Optional

import httpx

from dsp.common import CONTEXT_DSPACE, CONTEXT_DSPACE_ODRL_POLICY, build_permission, get_iatp_token

logger = logging.getLogger("tier2-mock")

_store: dict[str, dict] = {}


def create(body: dict, bpnl: str) -> tuple[dict, str, str]:
    """Returns (response_body, neg_id, callback_address)."""
    neg_id = f"urn:uuid:{uuid.uuid4()}"
    agreement_id = f"urn:uuid:{uuid.uuid4()}"
    consumer_pid = body.get("dspace:consumerPid", f"urn:uuid:{uuid.uuid4()}")
    callback = body.get("dspace:callbackAddress", "")
    asset_id = _extract_asset_id(body)

    entry = {
        "agreementId": agreement_id,
        "assetId": asset_id,
        "consumerPid": consumer_pid,
        "callbackAddress": callback,
    }
    # Store by both provider and consumer PIDs for lookup flexibility
    _store[neg_id] = entry
    _store[consumer_pid] = entry
    return _requested(neg_id, consumer_pid), neg_id, callback


def get_state(neg_id: str, bpnl: str) -> Optional[dict]:
    entry = _store.get(neg_id)
    if not entry:
        return None
    return _finalized(neg_id, bpnl, entry["agreementId"], entry["consumerPid"])


def _extract_asset_id(body: dict) -> str:
    offer = body.get("dspace:offer", body.get("offer", {}))
    target = offer.get("odrl:target", offer.get("target", {}))
    if isinstance(target, dict):
        return target.get("@id", "")
    return str(target)


def _requested(neg_id: str, consumer_pid: str) -> dict:
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:ContractNegotiation",
        "@id": neg_id,
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": consumer_pid,
        "dspace:state": "dspace:REQUESTED",
    }


def _finalized(neg_id: str, bpnl: str, agreement_id: str, consumer_pid: str) -> dict:
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:ContractNegotiation",
        "@id": neg_id,
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": consumer_pid,
        "dspace:state": "dspace:FINALIZED",
        "dspace:contractAgreementId": agreement_id,
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
        "@type": "dspace:ContractAgreementMessage",
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:agreement": {
            "@id": entry["agreementId"],
            "@type": "odrl:Agreement",
            "odrl:target": {"@id": entry["assetId"]},
            "dspace:timestamp": now,
            "odrl:assigner": bpnl,
            "odrl:assignee": supplier_bpnl,
            "odrl:permission": build_permission(),
            "odrl:prohibition": [],
            "odrl:obligation": [],
        },
    }

    consumer_pid = entry["consumerPid"]
    callback_url = f"{callback_address.rstrip('/')}/negotiations/{consumer_pid}/agreement"
    headers = {"Content-Type": "application/json"}
    iatp_token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
    if iatp_token:
        headers["Authorization"] = f"Bearer {iatp_token}"

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(callback_url, json=message, headers=headers)
            if resp.is_success:
                logger.info("ContractAgreementMessage sent to %s — %s", callback_url, resp.status_code)
            else:
                logger.warning("ContractAgreementMessage to %s returned %s: %s", callback_url, resp.status_code, resp.text[:500])
    except Exception as exc:
        logger.warning("Could not push ContractAgreementMessage to %s: %s", callback_url, exc)


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
        "@type": "dspace:ContractNegotiationEventMessage",
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:eventType": {"@id": "dspace:FINALIZED"},
    }

    consumer_pid = entry["consumerPid"]
    callback_url = f"{callback_address.rstrip('/')}/negotiations/{consumer_pid}/events"
    headers = {"Content-Type": "application/json"}
    iatp_token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
    if iatp_token:
        headers["Authorization"] = f"Bearer {iatp_token}"

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(callback_url, json=message, headers=headers)
            if resp.is_success:
                logger.info("ContractNegotiationEventMessage sent to %s — %s", callback_url, resp.status_code)
            else:
                logger.warning("ContractNegotiationEventMessage to %s returned %s: %s", callback_url, resp.status_code, resp.text[:200])
    except Exception as exc:
        logger.warning("Could not push ContractNegotiationEventMessage to %s: %s", callback_url, exc)
