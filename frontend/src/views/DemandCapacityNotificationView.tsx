import { Tab, TabPanel, Tabs, Table } from '@catena-x/portal-shared-components';
import { Box, Button, Stack, Typography } from '@mui/material';
import { getDemandAndCapacityNotification } from '@services/demand-capacity-notification';
import { useEffect, useState } from 'react';
import { Add } from '@mui/icons-material';
import { DemandCapacityNotificationInformationModal } from '@features/notifications/components/NotificationInformationModal';
import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { EFFECTS } from '@models/constants/effects';
import { LEADING_ROOT_CAUSE } from '@models/constants/leading-root-causes';
import { STATUS } from '@models/constants/status';


export const DemandCapacityNotificationView = () => {

    const [selectedTab, setSelectedTab] = useState<number>(0);
    const [demandCapacityNotification, setDemandCapacityNotification] = useState<DemandCapacityNotification[]>([]);
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [selectedNotification, setSelectedNotification] = useState<DemandCapacityNotification | null>(null);

    const tabs = ['Incoming', 'Outgoing'];

    const fetchAndLogNotification = async () => {
        try {
            const result = await getDemandAndCapacityNotification(selectedTab === 0);
            setDemandCapacityNotification(result);
        } catch (error) {
            console.error(error);
        }
    }

    useEffect(() => {
        fetchAndLogNotification();
    }, [selectedTab]);

    const TabPanelContent = ({ notifications }: { notifications: DemandCapacityNotification[] }) => {
        return notifications.length !== 0 ?
            <DemandCapacityNotificationTable onRowSelected={(notification) => {
                setModalOpen(true);
                setSelectedNotification(notification);
            }} notifications={notifications} /> :
            <Typography>No notifications available</Typography>
    }

    return (
        <>
            <Stack spacing={2} alignItems='center' width='100%' height='100%'>
                <h1 className="text-3xl font-semibold text-gray-700 mb-10">Demand And Capacity Notifications</h1>
                <Tabs value={selectedTab} onChange={(_, value: number) => setSelectedTab(value)}>
                    {tabs.map((tab, index) => <Tab key={index} label={tab} />)}
                </Tabs>
                <Button variant="contained" onClick={() => {
                    setSelectedNotification(null);
                    setModalOpen(true)
                }
                }>
                    <Add></Add> Send Notification
                </Button>
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
                demandCapacityNotification={selectedNotification}
                onClose={() =>
                    setModalOpen(false)
                }
                onSave={fetchAndLogNotification}

            />
        </>
    );
};

type NotificationTableProps = {
    notifications: DemandCapacityNotification[],
    onRowSelected: (notification: DemandCapacityNotification) => void;
}

const DemandCapacityNotificationTable: React.FC<NotificationTableProps> = ({ notifications, onRowSelected }) => {
    return (
        <Box width="100%">
            <Table
                onRowClick={(value) => {
                    onRowSelected(value.row);
                }}
                title="Demand and Capacity Notifications"
                columns={[
                    { headerName: 'Text', field: 'text', width: 200 },
                    { headerName: 'Partners Bpnl', field: 'partnerBpnl', width: 200 },
                    { headerName: 'Leading Root Cause', field: 'leadingRootCause', width: 120, valueFormatter: (params) => LEADING_ROOT_CAUSE.find((cause) => cause.key === params.value)?.value },
                    { headerName: 'Effect', field: 'effect', width: 120, valueFormatter: (params) => EFFECTS.find((effect) => effect.key === params.value)?.value, },
                    { headerName: ' Affected Material Numbers', field: 'affectedMaterialNumbers', width: 200 },
                    { headerName: ' Affected Sites Sender', field: 'affectedSitesBpnsSender', width: 200 },
                    { headerName: ' Affected Sites Recipient', field: 'affectedSitesBpnsRecipient', width: 200 },
                    { headerName: 'Status', field: 'status', width: 100, valueFormatter: (params) => STATUS.find((status) => status.key === params.value)?.value },

                ]}
                rows={notifications ?? []}
                getRowId={(row) => row.uuid}
                noRowsMsg='No Notifications found'
            />
        </Box>
    );
}
