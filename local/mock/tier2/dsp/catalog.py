import uuid

CONTEXT = {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "cx-policy": "https://w3id.org/catenax/2025/9/policy",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "odrl": "http://www.w3.org/ns/odrl/2/",
    "dspace": "https://w3id.org/dspace/v0.8/",
    "cx-common": "https://w3id.org/catenax/ontology/common#",
    "cx-taxo": "https://w3id.org/catenax/taxonomy#",
    "aas-semantics": "https://admin-shell.io/aas/3/0/HasSemantics/",
}

FRAMEWORK_AGREEMENT = "DataExchangeGovernance:1.0"
USAGE_PURPOSE = "cx.puris.base:1"

SUBMODELS = [
    {
        "asset_id_prefix": "itemstockanonymizedsubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.item_stock_anonymized:1.0.0#ItemStockAnonymized",
        "version": "1.0",
    },
    {
        "asset_id_prefix": "productionanonymizedsubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.planned_production_output_anonymized:1.0.0#PlannedProductionOutputAnonymized",
        "version": "1.0",
    },
    {
        "asset_id_prefix": "deliveryanonymizedsubmodel-api-asset",
        "semantic_id": "urn:samm:io.catenax.delivery_information_anonymized:1.0.0#DeliveryInformationAnonymized",
        "version": "1.0",
    },
]

PART_TYPE_ASSET_PREFIX = "PartTypeInformationSubmodelApi"
DTR_ASSET_PREFIX = "DigitalTwinRegistryId"


def _offer_policy(offer_id: str) -> dict:
    return {
        "@id": offer_id,
        "@type": "odrl:Offer",
        "odrl:permission": {
            "odrl:action": {"@id": "odrl:use"},
            "odrl:constraint": {
                "odrl:and": [
                    {
                        "odrl:leftOperand": {"@id": "cx-policy:FrameworkAgreement"},
                        "odrl:operator": {"@id": "odrl:eq"},
                        "odrl:rightOperand": FRAMEWORK_AGREEMENT,
                    },
                    {
                        "odrl:leftOperand": {"@id": "cx-policy:UsagePurpose"},
                        "odrl:operator": {"@id": "odrl:isAnyOf"},
                        "odrl:rightOperand": USAGE_PURPOSE,
                    },
                ]
            },
        },
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
