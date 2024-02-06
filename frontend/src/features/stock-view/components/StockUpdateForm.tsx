/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/

import { Checkbox, Input, SelectList, LoadingButton } from '@catena-x/portal-shared-components';
import { MaterialDescriptor } from '../../../models/types/data/material-descriptor';
import { UNITS_OF_MEASUREMENT } from '../../../models/constants/uom';
import { useSites } from '../hooks/useSites';
import { useEffect, useReducer, useState } from 'react';
import { Site } from '../../../models/types/edc/site';
import { Autocomplete } from '@mui/material';
import { MaterialStock, ProductStock } from '../../../models/types/data/stock';

type StockUpdateFormProps = {
    items: MaterialDescriptor[] | null;
    type: 'material' | 'product';
    selectedItem: MaterialStock | ProductStock | null;
};

function stockReducer(
    state: Partial<MaterialStock> | Partial<ProductStock>,
    action: {
        type: 'replace' | keyof MaterialStock | keyof ProductStock;
        payload: MaterialStock[keyof MaterialStock] | ProductStock[keyof ProductStock] | MaterialStock | ProductStock;
    }
) {
    console.log(action);
    if (action.type === 'replace') {
        return action.payload as MaterialStock | ProductStock;
    }
    return {
        ...state,
        [action.type]: action.payload,
    };
}

export function StockUpdateForm({ items, type, selectedItem }: StockUpdateFormProps) {
    const { sites } = useSites();
    const [selectedSite, setSelectedSite] = useState<Site | null>(null);
    const [saving, setSaving] = useState<boolean>(false);
    const [newStock, dispatch] = useReducer(stockReducer, selectedItem ?? {});
    const handleSave = () => {
        if (saving) return;
        setSaving(true);
        setTimeout(() => {
            setSaving(false);
        }, 2000);
    };

    useEffect(() => {
        dispatch({ type: 'replace', payload: selectedItem });
    }, [selectedItem]);
    /* const handleStockNumberChange = (materialDescriptor: MaterialDescriptor) => {
        const newStock: Partial<MaterialStock> = {
            uuid: null,
            material: {
                uuid: null,
                materialFlag: true,
                productFlag: false,
                materialNumberCustomer: null,
                materialNumberSupplier: materialDescriptor.ownMaterialNumber,
                materialNumberCx: null,
                name: materialDescriptor.description,
            },
        };
    } */

    return (
        <form className="p-5">
            <div className="flex gap-5 justify-center">
                <div className="w-[32rem]">
                    <Autocomplete
                        id="material"
                        value={selectedItem != null || newStock?.material
                            ? {
                                  ownMaterialNumber:
                                      type === 'material'
                                          ? newStock?.material?.materialNumberCustomer
                                          : newStock?.material?.materialNumberSupplier,
                                  description: selectedItem?.material?.name,
                              }
                            : null}
                        options={items ?? []}
                        getOptionLabel={(option) => option.ownMaterialNumber ?? ''}
                        renderInput={(params) => <Input {...params} label={`${type}*`} placeholder={`Select a ${type}`} />}
                        onChange={(_, newValue) =>
                            dispatch({
                                type: 'material',
                                payload: {
                                    ...(type === 'material'
                                        ? { materialFlag: true, productFlag: false }
                                        : { materialFlag: false, productFlag: true }),
                                    uuid: null,
                                    materialNumberCustomer: type === 'material' ? newValue?.ownMaterialNumber ?? null : null,
                                    materialNumberSupplier: type === 'product' ? newValue?.ownMaterialNumber ?? null : null,
                                    materialNumberCx: null,
                                    name: newValue?.description ?? '',
                                },
                            })
                        }
                    />

                    <SelectList
                        id="partner"
                        label="Partner*"
                        items={[]}
                        placeholder="Select a partner"
                        keyTitle="name"
                        key="id"
                        onChangeItem={(item) => null}
                        disabled={selectedItem === null}
                    />
                    <div className="grid grid-cols-3 gap-2">
                        <div className="col-span-2">
                            <Input id="quantity" label="Quantity*" type="number" placeholder="Enter quantity" />
                        </div>
                        <SelectList
                            id="uom"
                            label="UOM*"
                            items={UNITS_OF_MEASUREMENT}
                            placeholder="Select unit"
                            keyTitle="value"
                            key="key"
                            onChangeItem={(item) => console.log(item)}
                        />
                    </div>
                    <div className="flex items-center justify-end pt-7">
                        <Checkbox label="Is Blocked" />
                    </div>
                </div>
                <div className="w-[32rem]">
                    <SelectList
                        id="site"
                        label={`Stock Location BPNS*`}
                        items={sites ?? []}
                        placeholder={`Select a Site`}
                        keyTitle="bpns"
                        onChangeItem={(item) => setSelectedSite(item)}
                    />
                    <SelectList
                        id="address"
                        label="Stock Location BPNA*"
                        items={selectedSite?.addresses ?? []}
                        placeholder="Select a partner"
                        keyTitle="bpna"
                        disabled={selectedSite === null}
                        onChangeItem={(item) => null}
                    />
                    <div className="grid grid-cols-2 gap-2">
                        <Input id="customer-order-number" label="Customer Order Number" type="text" />
                        <Input id="customer-order-position" label="Customer Order Position" type="text" />
                    </div>
                    <Input id="supplier-order-number" label="Supplier Order Number" type="text" />
                </div>
            </div>
            <div className="mt-7 mx-auto w-48">
                <LoadingButton
                    className="w-full"
                    variant="contained"
                    color="primary"
                    loading={saving}
                    loadIndicator="Saving..."
                    onButtonClick={() => handleSave()}
                    label="Add or Update"
                    fullWidth={true}
                ></LoadingButton>
            </div>
        </form>
    );
}
