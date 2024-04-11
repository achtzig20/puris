/*
Copyright (c) 2024 Volkswagen AG
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

import { useEffect, useState } from 'react';
import { Datepicker, Input, PageSnackbar, PageSnackbarStack } from '@catena-x/portal-shared-components';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Demand } from '@models/types/data/demand';
import { Autocomplete, Box, Button, Dialog, DialogTitle, Grid, Stack, Typography } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { Notification } from '@models/types/data/notification';
import { postDemand } from '@services/demands-service';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';

const GridItem = ({ label, value }: { label: string; value: string }) => (
    <Grid item xs={6}>
        <Stack>
            <Typography variant="caption1" fontWeight={500}>
                {label}:
            </Typography>
            <Typography variant="body3" paddingLeft=".5rem">
                {value}
            </Typography>
        </Stack>
    </Grid>
);

type DemandCategoryModalProps = {
    open: boolean;
    onClose: () => void;
    onSave: () => void;
    demand: Partial<Demand> | null;
};

export const DemandCategoryModal = (
    { open, onClose, onSave, demand }: DemandCategoryModalProps) => {
    const [temporaryDemand, setTemporaryDemand] = useState<Partial<Demand>>(demand ?? {});
    const { partners } = usePartners('product', temporaryDemand?.ownMaterialNumber ?? null);
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [formError, setFormError] = useState(false);

    const handleSaveClick = (demand: Partial<Demand>) => {
        //Form Validation
        if (
            !temporaryDemand?.day
            || !temporaryDemand?.quantity
            || !temporaryDemand.demandCategoryCode
            || !temporaryDemand?.measurementUnit
            || !temporaryDemand?.partnerBpnl
            || !temporaryDemand?.supplierSite
            ) {
            setFormError(true);
            return;
        } else {
            setFormError(false);
        }

        postDemand(demand)
            .then(() => {
                onSave;
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: 'Demand Created',
                        description: 'The Demand has been saved successfully',
                        severity: 'success',
                    },
                ]);
            })
            .catch((error) => {
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                        description: error.status === 409 ? 'Date conflicting with another Demand' : error.error,
                        severity: 'error',
                    },
                ]);
            });
        onClose();
    };
    useEffect(() => {
        if (demand) {
            setTemporaryDemand(demand);
        }
    }, [demand]);
    return (
        <>
            <Dialog open={open && demand !== null} onClose={onClose} title="Delivery Information">
                <DialogTitle fontWeight={600} textAlign="center">
                    Demand Information
                </DialogTitle>
                <Stack padding="0 2rem 2rem">
                    <Grid container spacing={2} width="32rem" padding=".25rem">
                        <GridItem label="Material Number" value={temporaryDemand.ownMaterialNumber ?? ''} />
                        <GridItem label="Site" value={temporaryDemand.demandLocationBpns ?? ''} />
                        <Grid item marginTop="1.5rem" xs={6}>
                            <Datepicker
                                label="Day"
                                placeholder="Pick a Day"
                                locale="de"
                                error={formError && !temporaryDemand?.day}
                                readOnly={false}
                                value={temporaryDemand?.day}
                                onChangeItem={(value) => setTemporaryDemand((curr) => ({ ...curr, day: value }))}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <Input
                                label="Quantity*"
                                type="number"
                                value={temporaryDemand.quantity}
                                error={formError && !temporaryDemand?.quantity}
                                onChange={(e) => setTemporaryDemand((curr) => (
                                    parseFloat(e.target.value) >= 0 ?
                                    { ...curr, quantity: parseFloat(e.target.value) } : { ...curr, quantity: 0 }
                                ))}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <Autocomplete
                                id="category"
                                clearIcon={false}
                                value={
                                    temporaryDemand.demandCategoryCode
                                        ? {
                                            key: temporaryDemand.demandCategoryCode,
                                            value: DEMAND_CATEGORY.find((c) => c.key === temporaryDemand.demandCategoryCode)?.value,
                                        }
                                        : DEMAND_CATEGORY[0]
                                }
                                options={DEMAND_CATEGORY}
                                getOptionLabel={(option) => option?.value ?? ''}
                                renderInput={
                                    (params) =>
                                        <Input
                                            {...params}
                                            label="Category*"
                                            placeholder="Select category"
                                            error={formError && !temporaryDemand?.demandCategoryCode}
                                        />
                                }
                                onChange={(_, value) => setTemporaryDemand((curr) => ({ ...curr, demandCategoryCode: value?.key }))}
                                isOptionEqualToValue={(option, value) => option?.key === value?.key}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <Autocomplete
                                id="uom"
                                clearIcon={false}
                                value={
                                    temporaryDemand.measurementUnit
                                        ? {
                                            key: temporaryDemand.measurementUnit,
                                            value: getUnitOfMeasurement(temporaryDemand.measurementUnit),
                                        }
                                        : null
                                }
                                options={UNITS_OF_MEASUREMENT}
                                getOptionLabel={(option) => option?.value ?? ''}
                                renderInput={
                                    (params) =>
                                        <Input
                                            {...params}
                                            label="UOM*"
                                            placeholder="Select unit"
                                            error={formError && !temporaryDemand?.measurementUnit}
                                        />
                                }
                                onChange={(_, value) => setTemporaryDemand((curr) => ({ ...curr, measurementUnit: value?.key }))}
                                isOptionEqualToValue={(option, value) => option?.key === value?.key}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <Autocomplete
                                id="partner"
                                options={partners ?? []}
                                getOptionLabel={(option) => option?.name ?? ''}
                                renderInput={
                                    (params) =>
                                        <Input
                                            {...params}
                                            label="Partner*"
                                            placeholder="Select a Partner"
                                            error={formError && !temporaryDemand?.partnerBpnl}
                                        />
                                }
                                onChange={(event, value) => setTemporaryDemand({ ...temporaryDemand, partnerBpnl: value ?? undefined })}
                                value={temporaryDemand.partnerBpnl}
                                isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <Autocomplete
                                id="supplierSite"
                                options={[]}
                                //options={suppliers ?? []}
                                getOptionLabel={(option) => option ?? ''}
                                renderInput={
                                    (params) =>
                                        <Input
                                            {...params}
                                            label="Supplier Site*"
                                            placeholder="Select a Site"
                                            error={formError && !temporaryDemand?.supplierSite}
                                        />
                                }
                                onChange={(event, value) => setTemporaryDemand({ ...temporaryDemand, supplierSite: value ?? undefined })}
                                value={temporaryDemand.supplierSite}
                                isOptionEqualToValue={(option, value) => option === value}
                            />
                        </Grid>
                    </Grid>
                    <Box display="flex" gap="1rem" width="100%" justifyContent="stretch">
                        <Button variant="contained" color="error" sx={{ marginTop: '2rem', flexGrow: '1' }} onClick={onClose}>
                            Close
                        </Button>
                        <Button
                            variant="contained"
                            color="primary"
                            sx={{ marginTop: '2rem', flexGrow: '1' }}
                            onClick={() => handleSaveClick(temporaryDemand)}
                        >
                            Save
                        </Button>
                    </Box>
                </Stack>
            </Dialog>
            <PageSnackbarStack>
                {notifications.map((notification, index) => (
                    <PageSnackbar
                        key={index}
                        open={!!notification}
                        severity={notification?.severity}
                        title={notification?.title}
                        description={notification?.description}
                        autoClose={true}
                        onCloseNotification={() => setNotifications((ns) => ns.filter((_, i) => i !== index) ?? [])}
                    />
                ))}
            </PageSnackbarStack>
        </>
    );
};
