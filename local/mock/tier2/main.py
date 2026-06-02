#
# Copyright (c) 2026 Contributors to the Eclipse Foundation
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
"""
Tier2 mock — single-container mock for n-tier supply-chain simulation.

Implements the provider side of:
  - DSP v0.8 catalog / negotiation / transfer endpoints
  - A data plane (HttpData-PULL) serving DTR lookups and PURIS submodels
"""

import asyncio
import logging
import os

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

import data as mock_data
from dsp import catalog, negotiations, transfers

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("tier2-mock")

# --------------------------------------------------------------------------
# Config (from environment)xBusiness Partner Data Registry
# --------------------------------------------------------------------------
BPNL = os.getenv("TIER2_BPNL", "BPNL3333333333T3")
BPNS = os.getenv("TIER2_BPNS", "BPNS3333333333T3")
BPNA = os.getenv("TIER2_BPNA", "BPNA3333333333T3")
BASE_URL = os.getenv("TIER2_MOCK_BASE_URL", "http://puris-tier2-mock:8083")
SUPPLIER_BPNL = os.getenv("SUPPLIER_BPNL", "BPNL1234567890ZZ")
WALLET_URL = os.getenv("WALLET_URL", "http://wallet:80")
WALLET_OAUTH_SECRET = os.getenv("WALLET_OAUTH_SECRET", "miw_private_client")
SUPPLIER_DSP_URL = os.getenv("SUPPLIER_DSP_URL", "http://supplier-control-plane:9184/api/v1/dsp")

app = FastAPI(title="PURIS Tier2 Mock", version="1.0.0")


# --------------------------------------------------------------------------
# DSP: Catalog
# --------------------------------------------------------------------------

@app.post("/api/v1/dsp/catalog/request")
async def handle_catalog(request: Request):
    auth = request.headers.get("Authorization", "NONE")
    logger.info("Catalog Authorization header (first 80): %s", auth[:80])
    body = await request.json()
    logger.info("Catalog request from %s", request.client.host)
    return catalog.build(BPNL, BASE_URL)


# --------------------------------------------------------------------------
# DSP: Contract Negotiations
# --------------------------------------------------------------------------

@app.post("/api/v1/dsp/negotiations/request")
async def start_negotiation(request: Request):
    auth_header = request.headers.get("Authorization", "NONE")
    logger.info("Negotiation request Authorization header: %s", auth_header[:120] if auth_header != "NONE" else "NONE")
    body = await request.json()
    logger.info("Negotiation request: %s", body)
    response, neg_id, callback = negotiations.create(body, BPNL)
    callback = callback or SUPPLIER_DSP_URL
    logger.info("Negotiation created: %s, callback: %s", neg_id, callback)
    asyncio.create_task(
        negotiations.push_agreement_message(
            neg_id=neg_id,
            callback_address=callback,
            bpnl=BPNL,
            supplier_bpnl=SUPPLIER_BPNL,
            wallet_url=WALLET_URL,
            wallet_secret=WALLET_OAUTH_SECRET,
        )
    )
    return response


@app.get("/api/v1/dsp/negotiations/{neg_id}")
async def get_negotiation(neg_id: str):
    state = negotiations.get_state(neg_id, BPNL)
    if state is None:
        return JSONResponse(status_code=404, content={"error": f"Negotiation {neg_id} not found"})
    return state


@app.get("/api/mock/negotiations/{neg_id}/state")
async def get_negotiation_state_mock(neg_id: str):
    """Mock endpoint: Returns the mock's internal finalized negotiation state.
    This bypasses DSP callback mechanisms and directly returns what the mock has created.
    Useful for testing when EDC's callback validation is preventing DSP callbacks from succeeding."""
    state = negotiations.get_state(neg_id, BPNL)
    if state is None:
        return JSONResponse(status_code=404, content={"error": f"Negotiation {neg_id} not found"})
    return state


@app.post("/api/v1/dsp/negotiations/{neg_id}/agreement/verification")
async def verify_agreement(neg_id: str, request: Request):
    logger.info("Agreement verification for negotiation %s", neg_id)
    entry = negotiations._store.get(neg_id, {})
    callback = entry.get("callbackAddress", "") or SUPPLIER_DSP_URL
    asyncio.create_task(
        negotiations.push_finalized_message(
            neg_id=neg_id,
            callback_address=callback,
            bpnl=BPNL,
            supplier_bpnl=SUPPLIER_BPNL,
            wallet_url=WALLET_URL,
            wallet_secret=WALLET_OAUTH_SECRET,
        )
    )
    return {
        "@context": {"dspace": "https://w3id.org/dspace/v0.8/"},
        "@type": "dspace:ContractNegotiation",
        "dspace:providerPid": neg_id,
        "dspace:state": "dspace:FINALIZED",
    }


# --------------------------------------------------------------------------
# DSP: Transfers
# --------------------------------------------------------------------------

@app.post("/api/v1/dsp/transfers/request")
async def start_transfer(request: Request):
    body = await request.json()
    logger.info("Transfer request: %s", body)
    response, provider_pid, callback = transfers.create(body, BPNL, BASE_URL)
    logger.info("Transfer created: %s, callback: %s", provider_pid, callback)

    # Send TransferStartMessage asynchronously to the consumer's callback address
    asyncio.create_task(
        transfers.push_start_message(
            provider_pid=provider_pid,
            callback_address=callback,
            bpnl=BPNL,
            supplier_bpnl=SUPPLIER_BPNL,
            wallet_url=WALLET_URL,
            wallet_secret=WALLET_OAUTH_SECRET,
        )
    )
    return response


@app.get("/api/v1/dsp/transfers/{transfer_id}")
async def get_transfer(transfer_id: str):
    state = transfers.get_state(transfer_id)
    if state is None:
        return JSONResponse(status_code=404, content={"error": f"Transfer {transfer_id} not found"})
    return state


@app.post("/api/v1/dsp/transfers/{transfer_id}/completion")
async def complete_transfer(transfer_id: str, request: Request):
    logger.info("Transfer completion for %s", transfer_id)
    return JSONResponse(status_code=200, content={})


@app.post("/api/v1/dsp/transfers/{transfer_id}/termination")
async def terminate_transfer(transfer_id: str, request: Request):
    logger.info("Transfer termination for %s", transfer_id)
    return JSONResponse(status_code=200, content={})


# --------------------------------------------------------------------------
# Data Plane  (HttpData-PULL)
#
# The EDR endpoint returned by the transfer process points here.
# PURIS backend calls this directly with the token from the EDR.
#
# Two types of paths are handled:
#   1. DTR:   /api/public/lookup/shells?assetIds=...
#             /api/public/shell-descriptors/{base64-aasId}
#   2. PURIS: /api/public/{asset-id}
# --------------------------------------------------------------------------

@app.get("/api/public/lookup/shells")
async def dtr_lookup(request: Request):
    asset_ids_b64 = request.query_params.get("assetIds", "")
    logger.info("DTR lookup, assetIds=%s", asset_ids_b64)
    result = mock_data.lookup_shells(BPNL, BASE_URL, asset_ids_b64)
    return result


@app.get("/api/public/shell-descriptors/{aas_id_b64}")
async def dtr_shell(aas_id_b64: str):
    logger.info("DTR shell-descriptors: %s", aas_id_b64)
    shell = mock_data.get_shell(aas_id_b64, BPNL, BASE_URL)
    if shell is None:
        return JSONResponse(status_code=404, content={"error": "Shell not found"})
    return shell


@app.get("/api/public/{asset_id:path}")
async def data_plane(asset_id: str, request: Request):
    # Strip trailing $value representation suffix if present
    clean_id = asset_id.split("/$value")[0].split("/$metadata")[0]
    logger.info("Data plane request: asset=%s", clean_id)
    result = mock_data.get_submodel(clean_id, BPNL)
    if result is None:
        logger.warning("Unknown asset requested: %s", clean_id)
        return JSONResponse(status_code=404, content={"error": f"Unknown asset: {clean_id}"})
    return result


# --------------------------------------------------------------------------
# Health
# --------------------------------------------------------------------------

@app.get("/health")
async def health():
    return {"status": "UP", "bpnl": BPNL}
