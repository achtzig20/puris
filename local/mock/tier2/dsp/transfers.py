import asyncio
import logging
import uuid
from typing import Optional

import httpx

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
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
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
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
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
            "dspace": "https://w3id.org/dspace/v0.8/",
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

    iatp_token = await _get_iatp_token(bpnl, supplier_bpnl, wallet_url, wallet_secret)

    message = {
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
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
