import asyncio
import logging
import uuid
from datetime import datetime, timezone
from typing import Optional

import httpx

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
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
        "@type": "dspace:ContractNegotiation",
        "@id": neg_id,
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": consumer_pid,
        "dspace:state": "dspace:REQUESTED",
    }


def _finalized(neg_id: str, bpnl: str, agreement_id: str, consumer_pid: str) -> dict:
    return {
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
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
        "@context": {
            "dspace": "https://w3id.org/dspace/v0.8/",
            "odrl": "http://www.w3.org/ns/odrl/2/",
            "cx-policy": "https://w3id.org/catenax/2025/9/policy/",
        },
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
            "odrl:permission": {
                "odrl:action": {"@id": "odrl:use"},
                "odrl:constraint": {
                    "odrl:and": [
                        {
                            "odrl:leftOperand": {"@id": "cx-policy:FrameworkAgreement"},
                            "odrl:operator": {"@id": "odrl:eq"},
                            "odrl:rightOperand": "DataExchangeGovernance:1.0",
                        },
                        {
                            "odrl:leftOperand": {"@id": "cx-policy:UsagePurpose"},
                            "odrl:operator": {"@id": "odrl:isAnyOf"},
                            "odrl:rightOperand": "cx.puris.base:1",
                        },
                    ]
                },
            },
            "odrl:prohibition": [],
            "odrl:obligation": [],
        },
    }

    consumer_pid = entry["consumerPid"]
    callback_url = f"{callback_address.rstrip('/')}/negotiations/{consumer_pid}/agreement"
    headers = {"Content-Type": "application/json"}
    iatp_token = await _get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
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
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
        "@type": "dspace:ContractNegotiationEventMessage",
        "dspace:providerPid": neg_id,
        "dspace:consumerPid": entry["consumerPid"],
        "dspace:eventType": {"@id": "dspace:FINALIZED"},
    }

    consumer_pid = entry["consumerPid"]
    callback_url = f"{callback_address.rstrip('/')}/negotiations/{consumer_pid}/events"
    headers = {"Content-Type": "application/json"}
    iatp_token = await _get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)
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


async def _get_iatp_token(bpnl: str, supplier_bpnl: str, wallet_url: str, wallet_secret: str) -> Optional[str]:
    """Get a scoped IATP token from the wallet targeting the supplier's DID."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            # Step 1: OAuth2 bearer token
            resp = await client.post(
                f"{wallet_url}/oauth/token",
                data={
                    "grant_type": "client_credentials",
                    "client_id": bpnl,
                    "client_secret": wallet_secret,
                },
                headers={"Content-Type": "application/x-www-form-urlencoded"},
            )
            if not resp.is_success:
                logger.warning("Wallet OAuth failed: %s", resp.status_code)
                return None
            bearer = resp.json().get("access_token")
            if not bearer:
                logger.warning("Wallet OAuth returned no access_token")
                return None

            # Step 2: STS grantAccess — produces a token with aud=supplier DID
            resp2 = await client.post(
                f"{wallet_url}/api/sts",
                json={
                    "grantAccess": {
                        "scope": "read",
                        "credentialTypes": ["MembershipCredential"],
                        "consumerDid": f"did:web:wallet:{bpnl}",
                        "providerDid": f"did:web:wallet:{supplier_bpnl}",
                    }
                },
                headers={"Authorization": f"Bearer {bearer}", "Content-Type": "application/json"},
            )
            if not resp2.is_success:
                logger.warning("STS grantAccess failed: %s %s", resp2.status_code, resp2.text[:200])
                return None
            jwt = resp2.json().get("jwt")
            if not jwt:
                logger.warning("STS grantAccess returned no jwt: %s", resp2.json())
                return None
            return jwt
    except Exception as exc:
        logger.warning("IATP token acquisition failed: %s", exc)
        return None
