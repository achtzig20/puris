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
            "urn:samm:io.catenax.item_stock_anonymized:1.0.0#ItemStockAnonymized",
            f"itemstockanonymizedsubmodel-api-asset@{bpnl}",
        ),
        _submodel_descriptor(
            "urn:samm:io.catenax.delivery_information_anonymized:1.0.0#DeliveryInformationAnonymized",
            f"deliveryanonymizedsubmodel-api-asset@{bpnl}",
        ),
        _submodel_descriptor(
            "urn:samm:io.catenax.planned_production_output_anonymized:1.0.0#PlannedProductionOutputAnonymized",
            f"productionanonymizedsubmodel-api-asset@{bpnl}",
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
        "itemstockanonymizedsubmodel-api-asset": _item_stock_anonymized,
        "deliveryanonymizedsubmodel-api-asset": _delivery_anonymized,
        "productionanonymizedsubmodel-api-asset": _production_anonymized,
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


def _item_stock_anonymized(_bpnl: str) -> dict:
    return {
        "allocatedStocksAnonymized": [
            {
                "quantityOnAllocatedStock": {"value": 200.0, "unit": "unit:piece"},
                "stockLocationBPNSAnonymized": "enc:tier2-site-a1b2c3",
                "isBlocked": False,
                "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
            }
        ],
        "materialGlobalAssetIdAnonymized": "enc:tier2-mat-d4e5f6",
        "direction": "OUTBOUND",
    }


def _delivery_anonymized(_bpnl: str) -> dict:
    return {
        "materialGlobalAssetIdAnonymized": "enc:tier2-mat-d4e5f6",
        "deliveries": [
            {
                "deliveryQuantity": {"value": 100.0, "unit": "unit:piece"},
                "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
                "transitEvents": [
                    {"dateTimeOfEvent": f"{_future(3)}T00:00:00Z", "eventType": "estimated-departure"},
                    {"dateTimeOfEvent": f"{_future(5)}T00:00:00Z", "eventType": "estimated-arrival"},
                ],
                "originBpnsAnonymized": "enc:tier2-origin-g7h8i9",
                "destinationBpnsAnonymized": "enc:supplier-dest-j0k1l2",
            }
        ],
    }


def _production_anonymized(_bpnl: str) -> dict:
    return {
        "materialGlobalAssetIdAnonymized": "enc:tier2-mat-d4e5f6",
        "allocatedPlannedProductionOutputs": [
            {
                "plannedProductionQuantity": {"value": 500.0, "unit": "unit:piece"},
                "productionSiteBpnsAnonymized": "enc:tier2-site-a1b2c3",
                "estimatedTimeOfCompletion": f"{_future(7)}T00:00:00Z",
                "lastUpdatedOnDateTime": f"{_today()}T00:00:00Z",
            }
        ],
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
