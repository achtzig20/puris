"""Mock data for DTR and PURIS submodels served by the tier2 mock data plane."""

import base64
import json
from datetime import date, timedelta
from typing import Optional

# Material numbers used in the tier2 mock.
# These must match what the Supplier PURIS registers as partnerMaterialNumber for tier2.
TIER2_MATERIAL_NUMBER = "MNR-8101-ID175283.001"
TIER2_AAS_ID = "urn:uuid:tier2-aas-01"

# -------------------------------------------------------------------
# DTR
# -------------------------------------------------------------------

def lookup_shells(bpnl: str, base_url: str, asset_ids_b64: str) -> dict:
    """Handle GET /lookup/shells?assetIds=... — return matching AAS global IDs."""
    try:
        decoded = base64.b64decode(asset_ids_b64 + "==").decode("utf-8")
        params = json.loads(f"[{decoded}]")
    except Exception:
        params = []

    manufacturer_part_id = None
    for p in params:
        if p.get("name") == "manufacturerPartId":
            manufacturer_part_id = p.get("value")

    if manufacturer_part_id == TIER2_MATERIAL_NUMBER:
        return {"result": [TIER2_AAS_ID]}
    return {"result": []}


def get_shell(aas_id_b64: str, bpnl: str, base_url: str) -> Optional[dict]:
    """Handle GET /shell-descriptors/{base64(aasId)} — return AAS shell descriptor."""
    try:
        aas_id = base64.b64decode(aas_id_b64 + "==").decode("utf-8")
    except Exception:
        return None
    if aas_id != TIER2_AAS_ID:
        return None
    return _build_shell(bpnl, base_url)


def _build_shell(bpnl: str, base_url: str) -> dict:
    dsp_url = f"{base_url}/api/v1/dsp"
    public_url = f"{base_url}/api/public"

    def _submodel_descriptor(semantic_id: str, asset_id: str) -> dict:
        return {
            "id": f"urn:uuid:sm-{asset_id}",
            "semanticId": {
                "type": "ExternalReference",
                "keys": [{"type": "GlobalReference", "value": semantic_id}],
            },
            "endpoints": [
                {
                    "interface": "SUBMODEL-3.0",
                    "protocolInformation": {
                        "href": f"{public_url}/{asset_id}",
                        "endpointProtocol": "HTTP",
                        "endpointProtocolVersion": ["1.1"],
                        "subprotocol": "DSP",
                        "subprotocolBody": f"id={asset_id};dspEndpoint={dsp_url}",
                        "subprotocolBodyEncoding": "plain",
                    },
                }
            ],
        }

    submodel_descriptors = [
        _submodel_descriptor(
            "urn:samm:io.catenax.item_stock:2.0.0#ItemStock",
            f"itemstocksubmodel-api-asset@{bpnl}",
        ),
        _submodel_descriptor(
            "urn:samm:io.catenax.planned_production_output:2.0.0#PlannedProductionOutput",
            f"productionsubmodel-api-asset@{bpnl}",
        ),
        _submodel_descriptor(
            "urn:samm:io.catenax.short_term_material_demand:1.0.0#ShortTermMaterialDemand",
            f"demandsubmodel-api-asset@{bpnl}",
        ),
        _submodel_descriptor(
            "urn:samm:io.catenax.delivery_information:2.0.0#DeliveryInformation",
            f"deliverysubmodel-api-asset@{bpnl}",
        ),
        _submodel_descriptor(
            "urn:samm:io.catenax.days_of_supply:2.0.0#DaysOfSupply",
            f"daysofsupplysubmodel-api-asset@{bpnl}",
        ),
        _submodel_descriptor(
            "urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation",
            f"PartTypeInformationSubmodelApi@{bpnl}",
        ),
    ]

    return {
        "id": TIER2_AAS_ID,
        "globalAssetId": f"urn:uuid:global-{bpnl}-mat-01",
        "idShort": "Tier2Semiconductor",
        "specificAssetIds": [
            {"name": "manufacturerPartId", "value": TIER2_MATERIAL_NUMBER},
            {"name": "manufacturerId", "value": bpnl},
            {"name": "digitalTwinType", "value": "PartType"},
        ],
        "submodelDescriptors": submodel_descriptors,
    }


# -------------------------------------------------------------------
# PURIS Submodels  (minimal valid payloads)
# -------------------------------------------------------------------

def get_submodel(asset_id: str, bpnl: str) -> Optional[dict]:
    prefix = asset_id.split("@")[0]
    dispatch = {
        "itemstocksubmodel-api-asset": _item_stock,
        "productionsubmodel-api-asset": _production,
        "demandsubmodel-api-asset": _demand,
        "deliverysubmodel-api-asset": _delivery,
        "daysofsupplysubmodel-api-asset": _days_of_supply,
        "notification-api-asset": _notification,
        "PartTypeInformationSubmodelApi": _part_type_info,
    }
    fn = dispatch.get(prefix)
    if fn is None:
        return None
    return fn(bpnl)


def _today() -> str:
    return date.today().isoformat()


def _future(days: int) -> str:
    return (date.today() + timedelta(days=days)).isoformat()


def _bpns(bpnl: str) -> str:
    return bpnl.replace("BPNL", "BPNS", 1)


def _item_stock(bpnl: str) -> dict:
    return {
        "positions": [
            {
                "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                "allocatedStocks": [
                    {
                        "quantityOnAllocatedStock": {"value": 200.0, "unit": "unit:piece"},
                        "stockLocationBPNS": _bpns(bpnl),
                        "isBlocked": False,
                    }
                ],
            }
        ],
        "materialNumberSupplier": TIER2_MATERIAL_NUMBER,
        "materialGlobalAssetId": f"urn:uuid:global-{bpnl}-mat-01",
        "direction": "OUTBOUND",
    }


def _production(bpnl: str) -> dict:
    return {
        "positions": [
            {
                "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                "allocatedPlannedProductionOutputs": [
                    {
                        "plannedProductionQuantity": {"value": 500.0, "unit": "unit:piece"},
                        "productionSiteBpns": _bpns(bpnl),
                        "estimatedTimeOfCompletion": f"{_future(7)}T00:00:00Z",
                    }
                ],
            }
        ],
        "materialNumberSupplier": TIER2_MATERIAL_NUMBER,
        "materialGlobalAssetId": f"urn:uuid:global-{bpnl}-mat-01",
        "direction": "OUTBOUND",
    }


def _demand(bpnl: str) -> dict:
    return {
        "demandSeries": [
            {
                "customerLocationBpns": "BPNS1234567890ZZ",
                "expectedSupplierLocationBpns": _bpns(bpnl),
                "demands": [
                    {
                        "demand": {"value": 100.0, "unit": "unit:piece"},
                        "pointInTime": _future(9),
                    }
                ],
                "demandCategoryCode": "0001",
            }
        ],
        "materialNumberSupplier": TIER2_MATERIAL_NUMBER,
        "materialGlobalAssetId": f"urn:uuid:global-{bpnl}-mat-01",
        "materialNumberCustomer": TIER2_MATERIAL_NUMBER,
        "customer": "BPNL1234567890ZZ",
    }


def _delivery(bpnl: str) -> dict:
    return {
        "deliveries": [
            {
                "deliveryQuantity": {"value": 100.0, "unit": "unit:piece"},
                "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                "transitEvents": [
                    {"dateTimeOfEvent": f"{_future(3)}T00:00:00Z", "eventType": "estimated-departure"},
                    {"dateTimeOfEvent": f"{_future(5)}T00:00:00Z", "eventType": "estimated-arrival"},
                ],
                "transitLocations": {
                    "origin": {"bpns": _bpns(bpnl)},
                    "destination": {"bpns": "BPNS1234567890ZZ"},
                },
                "incoterm": "EXW",
            }
        ],
        "materialNumberSupplier": TIER2_MATERIAL_NUMBER,
        "materialGlobalAssetId": f"urn:uuid:global-{bpnl}-mat-01",
        "direction": "OUTBOUND",
    }


def _days_of_supply(bpnl: str) -> dict:
    return {
        "allocated": [
            {
                "stockLocationBpns": _bpns(bpnl),
                "daysOfSupply": [
                    {"date": _today(), "daysOfSupply": 5.0},
                    {"date": _future(1), "daysOfSupply": 4.5},
                ],
            }
        ],
        "materialNumberSupplier": TIER2_MATERIAL_NUMBER,
        "materialGlobalAssetId": f"urn:uuid:global-{bpnl}-mat-01",
        "direction": "OUTBOUND",
    }


def _notification(_bpnl: str) -> dict:
    return {
        "affectedSitesBpnsSender": [],
        "affectedSitesBpnsRecipient": [],
        "leadingRootCause": "STRIKE",
        "effect": "DEMAND-VOLATILITY",
        "startDateOfEffect": _today(),
        "expectedEndDateOfEffect": _future(14),
        "status": "OPEN",
        "contentChangedAt": f"{_today()}T00:00:00Z",
        "relatedNotificationId": None,
        "sourceNotificationId": None,
        "text": "Mock notification",
    }


def _part_type_info(bpnl: str) -> dict:
    return {
        "catenaXId": f"urn:uuid:global-{bpnl}-mat-01",
        "partTypeInformation": {
            "manufacturerPartId": TIER2_MATERIAL_NUMBER,
            "nameAtManufacturer": "Tier2 Semiconductor",
            "classification": [
                {
                    "classificationStandard": "IEC",
                    "classificationID": "IC",
                    "classificationDescription": "Integrated Circuit",
                }
            ],
        },
    }
