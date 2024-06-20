/*
Copyright (c) 2023,2024 Volkswagen AG
Copyright (c) 2023,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2023,2024 Contributors to the Eclipse Foundation

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

import { Tab, TabPanel, Tabs, Table } from '@catena-x/portal-shared-components';
import { Box, Button, Stack, IconButton, Tooltip } from '@mui/material';
import { getDemandAndCapacityNotification, putDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { useCallback, useEffect, useState } from 'react';
import { Send } from '@mui/icons-material';
import { DemandCapacityNotificationInformationModal } from '@features/notifications/components/NotificationInformationModal';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { EFFECTS } from '@models/constants/effects';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { STATUS } from '@models/constants/status';
import { ModalMode } from '@models/types/data/modal-mode';
import { Edit, Visibility, ArrowOutward, Check } from '@mui/icons-material';
import { timeAgo } from '@util/date-helper';

export const DemandCapacityNotificationView = () => {

    const [selectedTab, setSelectedTab] = useState<number>(0);
    const [demandCapacityNotification, setDemandCapacityNotification] = useState<DemandCapacityNotification[]>([]);
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [mode, setMode] = useState<ModalMode>('create');
    const [selectedNotification, setSelectedNotification] = useState<DemandCapacityNotification | null>(null);

    const tabs = ['Incoming', 'Outgoing'];

    const fetchAndLogNotification = useCallback(async () => {
        try {
            const result = await getDemandAndCapacityNotification(selectedTab === 0);
            setDemandCapacityNotification(result);
        } catch (error) {
            console.error(error);
        }
    }, [selectedTab]);

    const resolveNotification = (notification: DemandCapacityNotification) => putDemandAndCapacityNotification(notification as DemandCapacityNotification).then(() => {
        fetchAndLogNotification();
    });

    useEffect(() => {
        fetchAndLogNotification();
    }, [selectedTab, fetchAndLogNotification]);

    const TabPanelContent = ({ notifications }: { notifications: DemandCapacityNotification[] }) => {
        return (
            <DemandCapacityNotificationTable onRowSelected={(notification, action) => {
                if (action === 'resolve') {
                    notification.status = 'resolved';
                    resolveNotification(notification);

                } else {
                    setMode(action);
                    setModalOpen(true);
                    setSelectedNotification(notification);
                }
            }} notifications={notifications} isIncoming={selectedTab === 0} />
        );
    }

    return (
        <>
            <Stack spacing={2} alignItems='center' width='100%' height='100%'>
                <h1 className="text-3xl font-semibold text-gray-700 mb-10"> Demand And Capacity Notifications </h1>
                <Stack width='100%' direction="row" justifyContent="space-between">
                    <Tabs value={selectedTab} onChange={(_, value: number) => setSelectedTab(value)}>
                        {tabs.map((tab, index) => <Tab key={index} label={tab} />)}
                    </Tabs>
                    <Button variant="contained" sx={{ display: 'flex', gap: '.5rem' }} onClick={() => {
                        setSelectedNotification(null);
                        setMode('create');
                        setModalOpen(true)
                    }}>
                        <Send></Send> Send Notification
                    </Button>
                </Stack>
                <Box width='100%' display='flex' marginTop='0 !important' paddingBottom='2rem'>
                    {tabs.map((_, index) => (
                        <TabPanel key={index} value={selectedTab} index={index}>
                            <TabPanelContent notifications={demandCapacityNotification} />
                        </TabPanel>
                    ))}
                </Box>
            </Stack>

            <DemandCapacityNotificationInformationModal
                open={modalOpen}
                mode={mode}
                demandCapacityNotification={selectedNotification}
                onClose={() => {
                    setSelectedNotification(null);
                    setModalOpen(false);
                }
                }
                onSave={fetchAndLogNotification}

            />
        </>
    );
};

type NotificationTableProps = {
    isIncoming: boolean;
    notifications: DemandCapacityNotification[],
    onRowSelected: (notification: DemandCapacityNotification, action: ModalMode | 'resolve') => void;
}

const DemandCapacityNotificationTable: React.FC<NotificationTableProps> = ({ notifications, onRowSelected, isIncoming }) => {
    return (
        <Box width="100%">
            <Table
                initialState={{
                    sorting: {
                        sortModel: [{ field: 'contentChangedAt', sort: 'desc' }],
                    },
                }}
                getRowClassName={(params) => `notification--${params.row.status}`}
                sx={{
                    ".actionButton": {
                        display: "none"
                    },
                    ".time-ago": {
                        display: "block"
                    },
                    ".MuiDataGrid-row:hover": {
                        ".actionButton": {
                            display: "block"
                        },
                        ".time-ago": {
                            display: "none"
                        }
                    },
                    ".notification--resolved": {
                        backgroundColor: "#f7f6f6"
                    },
                    ".notification--open": {
                        fontWeight: 700
                    }
                }}
                noRowsMsg='No Notifications found'
                title={"Demand and Capacity Notifications"}
                columns={[
                    { headerName: 'Text', field: 'text', width: 200 },
                    { headerName: 'Partner Bpnl', field: 'partnerBpnl', width: 200 },
                    { headerName: 'Leading Root Cause', field: 'leadingRootCause', width: 180, valueFormatter: (params) => LEADING_ROOT_CAUSE.find((cause) => cause.key === params.value)?.value },
                    { headerName: 'Effect', field: 'effect', width: 160, valueFormatter: (params) => EFFECTS.find((effect) => effect.key === params.value)?.value, },
                    { headerName: ' Material Numbers', field: 'affectedMaterialNumbers', width: 200 },
                    { headerName: ' Sites Sender', field: 'affectedSitesBpnsSender', width: 200 },
                    { headerName: ' Sites Recipient', field: 'affectedSitesBpnsRecipient', width: 200 },
                    { headerName: 'Status', field: 'status', width: 120, valueFormatter: (params) => STATUS.find((status) => status.key === params.value)?.value },
                    {
                        headerName: 'Changed at', align: 'right', minWidth: 150, flex: 1, field: 'contentChangedAt', renderCell: (params) => (
                            <>
                                <span className='time-ago'>
                                    {timeAgo(new Date, new Date(params.value))}</span>
                                <span className='actionButton'>
                                    <Tooltip title="View">
                                        <IconButton
                                            sx={{ mr: 1 }}
                                            tabIndex={params.hasFocus ? 0 : -1}
                                            onClick={() => onRowSelected(params.row, 'view')}
                                            color='primary'
                                        >
                                            <Visibility></Visibility>
                                        </IconButton>
                                    </Tooltip>
                                    {isIncoming ?
                                        <Tooltip title="React">
                                            <IconButton
                                                sx={{ mr: 1 }}
                                                tabIndex={params.hasFocus ? 0 : -1}
                                                onClick={() => { onRowSelected(params.row, 'react'); }}
                                                color='primary'
                                            >
                                                <ArrowOutward></ArrowOutward>
                                            </IconButton>
                                        </Tooltip> :
                                        <>
                                            <Tooltip title="Edit">
                                                <IconButton
                                                    sx={{ mr: 1 }}
                                                    tabIndex={params.hasFocus ? 0 : -1}
                                                    onClick={() => { onRowSelected(params.row, 'edit'); }}
                                                    color='primary'
                                                >
                                                    <Edit></Edit>
                                                </IconButton>
                                            </Tooltip>
                                            {params.row.status !== 'resolved' &&
                                                <Tooltip title="Resolve">
                                                    <IconButton
                                                        sx={{ mr: 1 }}
                                                        tabIndex={params.hasFocus ? 0 : -1}
                                                        onClick={() => { onRowSelected(params.row, 'resolve'); }}
                                                        color='primary'
                                                    >
                                                        <Check></Check>
                                                    </IconButton>
                                                </Tooltip>
                                            }
                                        </>
                                    }
                                </span>
                            </>
                        ),
                    },

                ]}
                rows={notifications ?? []}
                getRowId={(row) => row.notificationId}
            />
        </Box>
    );
}
