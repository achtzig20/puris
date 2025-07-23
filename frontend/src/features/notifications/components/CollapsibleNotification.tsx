/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

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
import { useState } from 'react';
import { Box, Button, IconButton, Stack, Typography, useTheme } from '@mui/material';
import { CheckCircle, ChevronRightOutlined, Edit } from '@mui/icons-material';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { Partner } from '@models/types/edc/partner';
import { Table } from '@catena-x/portal-shared-components';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { EFFECTS } from '@models/constants/effects';
import { STATUS } from '@models/constants/status';

type CollapsibleDemandNotificationProps = {
    disruptionId: string;
    notifications: DemandCapacityNotification[];
    partners: Partner[] | null;
    isResolved: boolean,
    onForwardClick: (id: string, notifications: any[]) => void;
    onRowSelected: (notification: any) => void;
    onEditClicked: (notification: any) => void;
    onCheckClicked: (notification: any) => void;
};

export function CollapsibleDisruptionPanel({
        disruptionId,
        notifications,
        partners,
        isResolved,
        onForwardClick,
        onRowSelected,
        onEditClicked,
        onCheckClicked,
    }: CollapsibleDemandNotificationProps) {
    const theme = useTheme();
    const [isExpanded, setIsExpanded] = useState(false);

    const incomingCount = notifications.filter(n => n.reported === true).length;
    const outgoingCount = notifications.filter(n => n.reported === false).length;
    const resolvedCount = notifications.filter(n => n.status === 'resolved').length;
    return (
        <>
            <Box
                sx={{
                    flexGrow: 1,
                    padding: 0,
                    textTransform: 'none',
                    minWidth: '100%',
                    position: 'sticky',
                    left: 0,
                    display: 'flex',
                    cursor: 'pointer', // Still shows as clickable
                }}
                onClick={() => setIsExpanded((prev) => !prev)}
                data-testid={`collapsible-notification-button-${disruptionId}`}
            >
                <Stack
                    direction="row"
                    alignItems="center"
                    spacing={0.5}
                    sx={{
                        borderRadius: isExpanded ? '0.75rem 0.75rem 0 0' : '0.75rem',
                        minHeight: '2.5rem',
                        width: '100%',
                        paddingLeft: '.5rem',
                        backgroundColor: isResolved ? theme.palette.primary.main : theme.palette.primary.dark,
                        color: theme.palette.primary.contrastText,
                    }}
                >
                    <Box sx={{ display: 'flex', flex: '1', alignItems: 'center', gap: '.5rem' }}>
                        <ChevronRightOutlined sx={{ rotate: isExpanded ? '90deg' : '0deg', transition: 'rotate 300ms ease-in-out'}} />
                        <Typography variant="body2"><b>{LEADING_ROOT_CAUSE.find((cause) => cause.key === notifications[0].leadingRootCause)?.value}</b></Typography>
                        <Typography variant="body2" color="#ccc">({EFFECTS.find((effect) => effect.key === notifications[0].effect)?.value})</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', flex: '1', justifyContent: 'flex-end', pr: 2, textAlign: 'center', gap: '1rem'}}>
                        <Typography variant="body2"><b>Incoming:</b> {incomingCount}</Typography>
                        <Typography variant="body2"><b>Outgoing:</b> {outgoingCount}</Typography>
                        <Typography variant="body2"><b>Resolved:</b> {resolvedCount}</Typography>
                    </Box>
                    {!isResolved && (
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', flex: '1', alignItems: 'center', pr: 2, }}>
                            <Button
                                variant="contained"
                                sx={{ display: 'flex', gap: '.5rem' }}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onForwardClick(disruptionId, notifications);
                                }}
                            >
                                {
                                    notifications.some((n) => !n.reported && (!n.relatedNotificationIds || n.relatedNotificationIds.length === 0))
                                    ? 'New Notification'
                                    : 'Forward'
                                }
                            </Button>
                        </Box>
                    )}
                </Stack>
            </Box>
            {isExpanded && (
                <DemandCapacityNotificationTable
                    notifications={notifications}
                    partners={partners}
                    onRowSelected={onRowSelected}
                    onEditClicked={onEditClicked}
                    onCheckClicked={onCheckClicked}
                />
            )}
        </>
    );
}

type NotificationTableProps = {
    notifications: DemandCapacityNotification[],
    partners: Partner[] | null,
    showActionsColumn?: boolean;
    onRowSelected: (notification: DemandCapacityNotification) => void;
    onEditClicked?: (notification: DemandCapacityNotification) => void;
    onCheckClicked?: (notification: DemandCapacityNotification) => void;
}

const DemandCapacityNotificationTable: React.FC<NotificationTableProps> = ({ notifications, partners, onRowSelected, onCheckClicked, onEditClicked, showActionsColumn = true, }) => {
    return (
        <Box width="100%" className="hide-title">
            <Table
                onRowClick={(value) => {
                    onRowSelected(value.row);
                }}
                noRowsMsg='No Notifications found'
                title={`Title`}
                columns={[
                    { headerName: 'Direction', field: 'reported', flex: 0.5, valueGetter: (params) => (params.row.reported ? 'Incoming' : 'Outgoing') },
                    { headerName: 'Partner', field: 'partnerBpnl', flex: 1, valueFormatter: (params) => partners?.find((partner) => partner.bpnl === params.value)?.name || params.value },
                    { headerName: 'Material Numbers', field: 'affectedMaterialNumbers', flex: 1 },
                    { headerName: 'Sites Sender', field: 'affectedSitesBpnsSender', flex: 1 },
                    { headerName: 'Sites Recipient', field: 'affectedSitesBpnsRecipient', flex: 1 },
                    { headerName: 'Start date', field: 'startDateOfEffect', flex: 1, renderCell: (data: { row: DemandCapacityNotification }) => (
                        <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Box>{new Date(data.row.startDateOfEffect).toLocaleDateString('en-GB')}</Box>
                            <Box>{new Date(data.row.startDateOfEffect).toLocaleTimeString('en-GB')}</Box>
                        </Stack>
                        ),
                    },
                    { headerName: 'End date', field: 'expectedEndDateOfEffect', flex: 1, renderCell: (data: { row: DemandCapacityNotification }) =>
                        data.row.expectedEndDateOfEffect ? (
                        <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Box>{new Date(data.row.expectedEndDateOfEffect).toLocaleDateString('en-GB')}</Box>
                            <Box>{new Date(data.row.expectedEndDateOfEffect).toLocaleTimeString('en-GB')}</Box>
                        </Stack>
                        ) : null
                    },
                    { headerName: 'Last Updated', field: 'contentChangedAt', flex: 1, renderCell: (data: { row: DemandCapacityNotification }) => (
                        <Stack display="flex" textAlign="center" alignItems="center" justifyContent="center" width="100%" height="100%">
                            <Box>{new Date(data.row.contentChangedAt).toLocaleDateString('en-GB')}</Box>
                            <Box>{new Date(data.row.contentChangedAt).toLocaleTimeString('en-GB')}</Box>
                        </Stack>
                        ),
                    },
                    { headerName: 'Status', field: 'status', flex: 0.5, valueFormatter: (params) => STATUS.find((status) => status.key === params.value)?.value },
                    { headerName: 'Note', field: 'text', flex: 1.25, renderCell: (data: { row: DemandCapacityNotification }) => (
                        <Stack display="flex" justifyContent="center" width="100%" height="100%">
                            <Box>{data.row.text}</Box>
                            {data.row.resolvingMeasureDescription && (
                                <Box>Resolution: {data.row.resolvingMeasureDescription}</Box>
                            )}
                        </Stack>
                        ),
                    },
                    { headerName: '', field: 'actions', flex: 0.5 , renderCell: (params) => {
                        if (params.row.status === 'resolved' || params.row.reported === true) {
                            return null;
                        }
                        return (
                            <Box display="flex" gap={1}>
                                <IconButton
                                    color="primary"
                                    aria-label="edit"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onEditClicked?.(params.row);
                                    }}
                                >
                                    <Edit></Edit>
                                </IconButton>
                                <IconButton
                                    sx={{color: "#3AD053"}}
                                    aria-label="confirm"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onCheckClicked?.(params.row);
                                    }}
                                >
                                    <CheckCircle></CheckCircle>
                                </IconButton>
                            </Box>
                        )
                    }}

                ]}
                rows={notifications ?? []}
                getRowId={(row) => row.uuid}
                getRowClassName={(params) => params.row.status === 'resolved' ? 'resolved-status' : ''}
                columnVisibilityModel={{
                    actions: showActionsColumn
                }}
            />
        </Box>
    );
}