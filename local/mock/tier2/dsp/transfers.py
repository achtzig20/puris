import asyncio
import logging
import uuid
from typing import Optional

import httpx

from dsp.common import CONTEXT_DSPACE, DSPACE_NS, get_iatp_token

logger = logging.getLogger("tier2-mock")

_store: dict[str, dict] = {}

FIXED_TOKEN = "tier2-mock-token"


def create(body: dict, bpnl: str, base_url: str) -> tuple[dict, str, str]:
    """Returns (response_body, provider_pid, callback_address)."""
    provider_pid = f"urn:uuid:{uuid.uuid4()}"
    consumer_pid = body.get("dspace:consumerPid", f"urn:uuid:{uuid.uuid4()}")
    callback = body.get("dspace:callbackAddress", "")

    _store[provider_pid] = {
        "state": "STARTED",
        "consumerPid": consumer_pid,
        "callbackAddress": callback,
        "endpoint": f"{base_url}/api/public",
        "token": FIXED_TOKEN,
    }

    response = {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:TransferProcess",
        "@id": provider_pid,
        "dspace:providerPid": provider_pid,
        "dspace:consumerPid": consumer_pid,
        "dspace:state": "dspace:REQUESTED",
    }
    return response, provider_pid, callback


def get_state(transfer_id: str) -> Optional[dict]:
    entry = _store.get(transfer_id)
    if not entry:
        return None
    return {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:TransferProcess",
        "@id": transfer_id,
        "dspace:providerPid": transfer_id,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:state": "dspace:STARTED",
        "dspace:dataAddress": _data_address(entry["endpoint"], entry["token"]),
    }


def _data_address(endpoint: str, token: str) -> dict:
    bearer = f"Bearer {token}"
    return {
        "@context": {
            "dspace": DSPACE_NS,
            "edc": "https://w3id.org/edc/v0.0.1/ns/",
            "tx-auth": "https://w3id.org/tractusx/auth/",
        },
        "@type": "dspace:DataAddress",
        "dspace:endpointType": "https://w3id.org/idsa/v4.1/HTTP",
        "dspace:endpoint": endpoint,
        "dspace:endpointProperties": [
            {
                "@type": "dspace:EndpointProperty",
                "dspace:name": "https://w3id.org/edc/v0.0.1/ns/endpoint",
                "dspace:value": endpoint,
            },
            {
                "@type": "dspace:EndpointProperty",
                "dspace:name": "https://w3id.org/tractusx/auth/token",
                "dspace:value": bearer,
            },
            {
                "@type": "dspace:EndpointProperty",
                "dspace:name": "authType",
                "dspace:value": "bearer",
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

    iatp_token = await get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)

    message = {
        "@context": CONTEXT_DSPACE,
        "@type": "dspace:TransferStartMessage",
        "dspace:providerPid": provider_pid,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:dataAddress": _data_address(entry["endpoint"], entry["token"]),
    }

    callback_url = f"{callback_address.rstrip('/')}/transfers/{entry['consumerPid']}/start"
    headers = {"Content-Type": "application/json"}
    if iatp_token:
        headers["Authorization"] = f"Bearer {iatp_token}"

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(callback_url, json=message, headers=headers)
            if resp.is_success:
                logger.info("TransferStartMessage sent to %s — %s", callback_url, resp.status_code)
            else:
                logger.warning(
                    "TransferStartMessage to %s returned %s: %s",
                    callback_url, resp.status_code, resp.text[:200],
                )
    except Exception as exc:
        logger.warning("Could not push TransferStartMessage to %s: %s", callback_url, exc)
