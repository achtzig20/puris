import uuid

from dsp.common import DSPACE_NS, ODRL_NS, build_permission

CONTEXT = {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "cx-policy": "https://w3id.org/catenax/2025/9/policy",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "odrl": ODRL_NS,
    "dspace": DSPACE_NS,
    "cx-common": "https://w3id.org/catenax/ontology/common#",
    "cx-taxo": "https://w3id.org/catenax/taxonomy#",
    "aas-semantics": "https://admin-shell.io/aas/3/0/HasSemantics/",
}

SUBMODELS = [
    {
        "asset_id_prefix": "itemstocksubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.item_stock:2.0.0#ItemStock",
        "version": "2.0",
    },
    {
        "asset_id_prefix": "productionsubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.planned_production_output:2.0.0#PlannedProductionOutput",
        "version": "2.0",
    },
    {
        "asset_id_prefix": "demandsubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.short_term_material_demand:1.0.0#ShortTermMaterialDemand",
        "version": "1.0",
    },
    {
        "asset_id_prefix": "deliverysubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.delivery_information:2.0.0#DeliveryInformation",
        "version": "2.0",
    },
    {
        "asset_id_prefix": "daysofsupplysubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.days_of_supply:2.0.0#DaysOfSupply",
        "version": "2.0",
    },
    {
        "asset_id_prefix": "notification-api-asset",
        "semantic_id": "urn:samm:io.catenax.demand_and_capacity_notification:3.0.0#DemandAndCapacityNotification",
        "version": "3.0",
    },
]

PART_TYPE_ASSET_PREFIX = "PartTypeInformationSubmodelApi"
DTR_ASSET_PREFIX = "DigitalTwinRegistryId"


def _offer_policy(offer_id: str) -> dict:
    return {
        "@id": offer_id,
        "@type": "odrl:Offer",
        "odrl:permission": build_permission(),
        "odrl:prohibition": [],
        "odrl:obligation": [],
    }


def _distribution(service_id: str) -> dict:
    return {
        "@type": "dcat:Distribution",
        "dct:format": {"@id": "HttpData-PULL"},
        "dcat:accessService": {"@id": service_id},
    }


def build(bpnl: str, base_url: str) -> dict:
    service_id = f"urn:uuid:{uuid.uuid4()}"
    datasets = []

    # DTR asset
    dtr_asset_id = f"{DTR_ASSET_PREFIX}@{bpnl}"
    datasets.append({
        "@id": dtr_asset_id,
        "@type": "dcat:Dataset",
        "dct:type": {"@id": "cx-taxo:DigitalTwinRegistry"},
        "cx-common:version": "3.0",
        "odrl:hasPolicy": _offer_policy(f"offer-dtr-{uuid.uuid4()}"),
        "dcat:distribution": [_distribution(service_id)],
    })

    # PartTypeInformation asset
    part_type_asset_id = f"{PART_TYPE_ASSET_PREFIX}@{bpnl}"
    datasets.append({
        "@id": part_type_asset_id,
        "@type": "dcat:Dataset",
        "dct:type": {"@id": "cx-taxo:Submodel"},
        "cx-common:version": "3.0",
        "aas-semantics:semanticId": {
            "@id": "urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation"
        },
        "odrl:hasPolicy": _offer_policy(f"offer-pti-{uuid.uuid4()}"),
        "dcat:distribution": [_distribution(service_id)],
    })

    # Submodel assets, list all files 
    for sm in SUBMODELS:
        asset_id = f"{sm['asset_id_prefix']}@{bpnl}"
        dct_type = sm.get("dct_type", "cx-taxo:Submodel")
        entry = {
            "@id": asset_id,
            "@type": "dcat:Dataset",
            "dct:type": {"@id": dct_type},
            "cx-common:version": sm["version"],
            "odrl:hasPolicy": _offer_policy(f"offer-{sm['asset_id_prefix']}-{uuid.uuid4()}"),
            "dcat:distribution": [_distribution(service_id)],
        }
        if dct_type == "cx-taxo:Submodel":
            entry["aas-semantics:semanticId"] = {"@id": sm["semantic_id"]}
        datasets.append(entry)

    return {
        "@context": CONTEXT,
        "@type": "dcat:Catalog",
        "@id": f"urn:uuid:{uuid.uuid4()}",
        "dspace:participantId": bpnl,
        "dcat:dataset": datasets,
        "dcat:service": {
            "@id": service_id,
            "@type": "dcat:DataService",
            "dct:terms": "connector",
            "dcat:endpointURL": f"{base_url}/api/v1/dsp",
        },
    }
