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
import { Production } from '@models/types/data/production';
import { Autocomplete, Box, Button, Checkbox, Dialog, DialogTitle, Grid, Stack, Typography } from '@mui/material';
import { getUnitOfMeasurement } from '@util/helpers';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { postProductionRange } from '@services/productions-service';
import { Notification } from '@models/types/data/notification';

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
    const [isRange, setIsRange] = useState<boolean>(false);
    const { partners } = usePartners('product', temporaryProduction?.material?.materialNumberSupplier ?? null);
    const [startDate, setStartDate] = useState<Date | null>(null);
    const [endDate, setEndDate] = useState<Date | null>(null);
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [formError, setFormError] = useState(false);

    const isCustomerOrderInvalid = () => {
        // All three are undefined, which is allowed
        if (!temporaryProduction?.customerOrderNumber
            && !temporaryProduction?.customerOrderPositionNumber
            && !temporaryProduction?.supplierOrderNumber
            ) {
            return false;
        // All three are set, which is allowed
        } else if (temporaryProduction?.customerOrderNumber
            && temporaryProduction?.customerOrderPositionNumber
            && temporaryProduction?.supplierOrderNumber
            ) {
            return false;
        // If any of them is set while others are undefined, or vice versa, it's not allowed
        } else {
            return true;
        }
    }

    const handleSaveClick = (production: Partial<Production>) => {
        //Form Validation
        if (
            !startDate
            || (isRange && !endDate)
            || !temporaryProduction?.quantity
            || !temporaryProduction?.measurementUnit
            || !temporaryProduction?.partner
            || isCustomerOrderInvalid()
            ) {
            setFormError(true);
            return;
        } else {
            setFormError(false);
        }

        if (isRange) {
            if (endDate === null) return;
            const length = Math.max(Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)), 0) + 1;
            const range = Array.from({ length }, (_, i) => {
                const date = new Date(`${new Date(startDate.toDateString())}Z`);
                date.setDate(date.getDate() + i);
                return { ...production, estimatedTimeOfCompletion: date };
            });
            postProductionRange(range)
                .then(() => {
                    onSave;
                    setNotifications((ns) => [
                        ...ns,
                        {
                            title: 'Production Created',
                            description: 'The Production has been saved successfully',
                            severity: 'success',
                        },
                    ]);
                })
                .catch((error) => {
                    setNotifications((ns) => [
                        ...ns,
                        {
                            title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                            description: error.status === 409 ? 'Date conflicting with another Production' : error.error,
                            severity: 'error',
                        },
                    ]);
                });
        } else {
            postProductionRange([{ ...production, estimatedTimeOfCompletion: startDate }])
            .then(() => {
                onSave;
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: 'Production Created',
                        description: 'The Production has been saved successfully',
                        severity: 'success',
                    },
                ]);
            })
            .catch((error) => {
                setNotifications((ns) => [
                    ...ns,
                    {
                        title: error.status === 409 ? 'Conflict' : 'Error requesting update',
                        description: error.status === 409 ? 'Date conflicting with another Production' : error.error,
                        severity: 'error',
                    },
                ]);
            });
        }
        onClose();
    };
    useEffect(() => {
        if (production) {
            setTemporaryProduction(production);
        }
    }, [production]);
    return (
        <>
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
                                checked={isRange}
                                onChange={(event) => setIsRange(event.target.checked)}
                            />
                            <label htmlFor="isBlocked"> Create Range </label>
                        </Grid>
                        <Grid item xs={6}>
                            <Datepicker
                                label="Start Date"
                                placeholder="Pick Production Start"
                                locale="de"
                                error={formError && !startDate}
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
                                error={formError && (isRange && !endDate)}
                                readOnly={false}
                                value={endDate}
                                onChangeItem={(event) => setEndDate(event)}
                                disabled={!isRange}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <Input
                                label={`${isRange ? 'Quantity per day' : 'Quantity'}*`}
                                type="number"
                                value={temporaryProduction.quantity}
                                error={formError && !temporaryProduction?.quantity}
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
                                renderInput={
                                    (params) =>
                                        <Input
                                            {...params}
                                            label="UOM*"
                                            placeholder="Select unit"
                                            error={formError && !temporaryProduction?.measurementUnit}
                                        />
                                }
                                onChange={(_, value) => setTemporaryProduction((curr) => ({ ...curr, measurementUnit: value?.key }))}
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
                                            error={formError && !temporaryProduction?.partner}
                                        />
                                }
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
                                error={formError && isCustomerOrderInvalid() && !temporaryProduction?.customerOrderNumber}
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
                                error={formError && isCustomerOrderInvalid() && !temporaryProduction?.customerOrderPositionNumber}
                                value={temporaryProduction?.customerOrderPositionNumber ?? null}
                                onChange={(event) =>
                                    setTemporaryProduction({ ...temporaryProduction, customerOrderPositionNumber: event.target.value })
                                }
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <Input
                                id="supplier-order-number"
                                label="Supplier Order Number"
                                type="text"
                                error={formError && isCustomerOrderInvalid() && !temporaryProduction?.supplierOrderNumber}
                                value={temporaryProduction?.supplierOrderNumber ?? null}
                                onChange={(event) =>
                                    setTemporaryProduction({ ...temporaryProduction, supplierOrderNumber: event.target.value })
                                }
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
