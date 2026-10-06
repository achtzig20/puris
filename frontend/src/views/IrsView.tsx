/*
Copyright (c) 2026 Volkswagen AG
Copyright (c) 2026 Contributors to the Eclipse Foundation

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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/

import { Table } from '@catena-x/portal-shared-components';
import { Box } from '@mui/material';
import { useEffect } from 'react';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useTitle } from '@contexts/titleProvider';
import { useIrsRootGrants } from '@hooks/irs/useIrsRootGrants';
import { useIrsPartnerGrants } from '@hooks/irs/useIrsPartnerGrants';
import { useIrsJobs } from '@hooks/irs/useIrsJobs';
import { useIrsRequests } from '@hooks/irs/useIrsRequests';
import { IrsChainOpeningGrant } from '@models/types/irs/irs-grant';

const formatInstant = (value: string | null | undefined) => (value ? new Date(value).toLocaleString() : '-');

const grantColumns = [
    { headerName: 'Global Asset Id', field: 'globalAssetId', flex: 1.25 },
    { headerName: 'Source Disruption Id', field: 'sourceDisruptionId', flex: 1 },
    { headerName: 'Requester BPN', field: 'requesterBpn', flex: 1 },
    {
        headerName: 'Allowed BPNLs',
        field: 'allowedBpnlSet',
        flex: 1,
        valueGetter: (params: { row: IrsChainOpeningGrant }) => params.row.allowedBpnlSet?.join(', ') ?? '-',
    },
    {
        headerName: 'Valid From',
        field: 'validFrom',
        flex: 0.75,
        valueGetter: (params: { row: IrsChainOpeningGrant }) => formatInstant(params.row.validFrom),
    },
    {
        headerName: 'Valid To',
        field: 'validTo',
        flex: 0.75,
        valueGetter: (params: { row: IrsChainOpeningGrant }) => formatInstant(params.row.validTo),
    },
    { headerName: 'Sync Status', field: 'syncStatus', flex: 0.75 },
];

export const IrsView = () => {
    const { rootGrants } = useIrsRootGrants();
    const { partnerGrants } = useIrsPartnerGrants();
    const { jobs } = useIrsJobs();
    const { requests } = useIrsRequests();
    const { setTitle } = useTitle();

    useEffect(() => {
        setTitle('IRS Integration');
    }, [setTitle]);

    return (
        <Box width="100%" sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <ConfidentialBanner />
            <Table
                title="Root Grants"
                columns={grantColumns}
                rows={rootGrants ?? []}
                getRowId={(row) => row.uuid}
                noRowsMsg="No root grants found"
                getRowHeight={() => 'auto'}
                rowSelection={false}
            />
            <Table
                title="Partner Grants"
                columns={grantColumns}
                rows={partnerGrants ?? []}
                getRowId={(row) => row.uuid}
                noRowsMsg="No partner grants found"
                getRowHeight={() => 'auto'}
                rowSelection={false}
            />
            <Table
                title="Jobs"
                columns={[
                    { headerName: 'Job Id', field: 'jobId', flex: 1 },
                    { headerName: 'Material Number', field: 'ownMaterialNumber', flex: 1 },
                    { headerName: 'Source Disruption Id', field: 'sourceDisruptionId', flex: 1 },
                    { headerName: 'State', field: 'state', flex: 0.75 },
                    { headerName: 'Request Status', field: 'requestStatus', flex: 0.75 },
                ]}
                rows={jobs ?? []}
                getRowId={(row) => row.uuid}
                noRowsMsg="No jobs found"
                getRowHeight={() => 'auto'}
                rowSelection={false}
            />
            <Table
                title="Requests"
                columns={[
                    { headerName: 'Type', field: 'type', flex: 1.25 },
                    { headerName: 'Method', field: 'method', flex: 0.5 },
                    { headerName: 'Path', field: 'path', flex: 1 },
                    { headerName: 'Status', field: 'status', flex: 0.75 },
                    {
                        headerName: 'Attempts',
                        field: 'attemptCount',
                        flex: 0.5,
                        valueGetter: (params) => `${params.row.attemptCount} / ${params.row.maxAttempts}`,
                    },
                    {
                        headerName: 'Created At',
                        field: 'createdAt',
                        flex: 0.75,
                        valueGetter: (params) => formatInstant(params.row.createdAt),
                    },
                    {
                        headerName: 'Last Attempt At',
                        field: 'lastAttemptAt',
                        flex: 0.75,
                        valueGetter: (params) => formatInstant(params.row.lastAttemptAt),
                    },
                    {
                        headerName: 'Next Attempt At',
                        field: 'nextAttemptAt',
                        flex: 0.75,
                        valueGetter: (params) => formatInstant(params.row.nextAttemptAt),
                    },
                    { headerName: 'Last Error', field: 'lastErrorMessage', flex: 1.25 },
                ]}
                rows={requests ?? []}
                getRowId={(row) => row.uuid}
                noRowsMsg="No requests found"
                getRowHeight={() => 'auto'}
                rowSelection={false}
            />
        </Box>
    );
};
