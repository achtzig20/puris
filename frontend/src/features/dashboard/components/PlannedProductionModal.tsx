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
import { Datepicker, Input } from '@catena-x/portal-shared-components';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Production } from '@models/types/data/production';
import { Autocomplete, Box, Button, Checkbox, Dialog, DialogTitle, Grid, Stack, Typography } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { postProductionRange } from '@services/productions-service';

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

type PlannedProductionModalProps = {
    open: boolean;
    onClose: () => void;
    onSave: () => void;
    production: Partial<Production> | null;
};

export const PlannedProductionModal = ({ open, onClose, onSave, production }: PlannedProductionModalProps) => {
    const [temporaryProduction, setTemporaryProduction] = useState<Partial<Production>>(production ?? {});
    const [isRange, setIsRange] = useState<boolean>(true);
    const { partners } = usePartners('product', temporaryProduction?.material?.materialNumberSupplier ?? null);
    const [startDate, setStartDate] = useState<Date | null>(null);
    const [endDate, setEndDate] = useState<Date | null>(null);
    const handleSaveClick = (production: Partial<Production>) => {
        if (startDate === null) return;
        if (isRange) {
            postProductionRange([{ ...production, estimatedTimeOfCompletion: startDate }])
                .then(onSave)
                .catch((err) => console.error(err));
        } else {
            if (endDate === null) return;
            const length =
                Math.max(Math.ceil((endDate?.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)), 0) + 1;
            const range = Array.from({ length }, (_, i) => {
                const date = new Date(`${new Date(startDate.toDateString())}Z`);
                date.setDate(date.getDate() + i);
                return { ...production, estimatedTimeOfCompletion: date };
            });
            postProductionRange(range)
                .then(onSave)
                .catch((err) => console.error(err));
        }
        onClose();
    };
    useEffect(() => {
        if (production) {
            setTemporaryProduction(production);
        }
    }, [production]);
    return (
        <Dialog open={open && production !== null} onClose={onClose} title="Delivery Information">
            <DialogTitle fontWeight={600} textAlign="center">
                Production Information
            </DialogTitle>
            <Stack padding="0 2rem 2rem">
                <Grid container spacing={2} width="32rem" padding=".25rem">
                    <GridItem label="Material Number" value={temporaryProduction.material?.materialNumberSupplier ?? ''} />
                    <GridItem label="Site" value={temporaryProduction.productionSiteBpns ?? ''} />

                    <Grid item xs={12} alignContent="center">
                        <Checkbox
                            id="input-mode"
                            checked={isRange === 'multi'}
                            onChange={(event) => setIsRange(event.target.checked ? 'multi' : 'single')}
                        />
                        <label htmlFor="isBlocked"> Create Range </label>
                    </Grid>
                    <Grid item xs={6}>
                        <Datepicker
                            label="Start Date"
                            placeholder="Pick Production Start"
                            locale="de"
                            readOnly={false}
                            value={startDate}
                            onChangeItem={(event) => setStartDate(event)}
                        />
                    </Grid>

                    <Grid item xs={6}>
                        <Datepicker
                            label="End Date"
                            placeholder="Pick Production End"
                            locale="de"
                            readOnly={false}
                            value={endDate}
                            onChangeItem={(event) => setEndDate(event)}
                            disabled={isRange === 'single'}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <Input
                            label={`${isRange === 'multi' ? 'Quantity per day' : 'Quantity'}*`}
                            type="number"
                            value={temporaryProduction.quantity}
                            onChange={(e) => setTemporaryProduction((curr) => ({ ...curr, quantity: parseFloat(e.target.value) }))}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <Autocomplete
                            id="uom"
                            value={
                                temporaryProduction.measurementUnit
                                    ? {
                                          key: temporaryProduction.measurementUnit,
                                          value: getUnitOfMeasurement(temporaryProduction.measurementUnit),
                                      }
                                    : null
                            }
                            options={UNITS_OF_MEASUREMENT}
                            getOptionLabel={(option) => option?.value ?? ''}
                            renderInput={(params) => <Input {...params} label="UOM*" placeholder="Select unit" />}
                            onChange={(_, value) => setTemporaryProduction((curr) => ({ ...curr, measurementUnit: value?.key }))}
                            isOptionEqualToValue={(option, value) => option?.key === value?.key}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <Autocomplete
                            id="partner"
                            options={partners ?? []}
                            getOptionLabel={(option) => option?.name ?? ''}
                            renderInput={(params) => <Input {...params} label="Partner*" placeholder="Select a Partner" />}
                            onChange={(event, value) => setTemporaryProduction({ ...temporaryProduction, partner: value ?? undefined })}
                            value={temporaryProduction.partner}
                            isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <Input
                            id="customer-order-number"
                            label="Customer Order Number"
                            type="text"
                            value={temporaryProduction?.customerOrderNumber ?? null}
                            onChange={(event) =>
                                setTemporaryProduction({ ...temporaryProduction, customerOrderNumber: event.target.value })
                            }
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <Input
                            id="customer-order-position-number"
                            label="Customer Order Position"
                            type="text"
                            value={temporaryProduction?.customerOrderPositionNumber ?? null}
                            onChange={(event) =>
                                setTemporaryProduction({ ...temporaryProduction, customerOrderPositionNumber: event.target.value })
                            }
                            disabled={!temporaryProduction?.customerOrderNumber}
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <Input
                            id="supplier-order-number"
                            label="Supplier Order Number"
                            type="text"
                            value={temporaryProduction?.supplierOrderNumber ?? null}
                            onChange={(event) =>
                                setTemporaryProduction({ ...temporaryProduction, supplierOrderNumber: event.target.value })
                            }
                            disabled={!temporaryProduction?.customerOrderNumber}
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
                        onClick={() => handleSaveClick(temporaryProduction)}
                    >
                        Save
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};
