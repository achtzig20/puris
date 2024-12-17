import { Material } from '@models/types/data/stock';
import { DirectionType } from '@models/types/erp/directionType';
import { Add, ChevronLeftOutlined, Refresh } from '@mui/icons-material';
import { Button, IconButton, Stack, Typography } from '@mui/material';
import { useDataModal } from '@contexts/dataModalContext';

type MaterialDetailsHeaderProps = {
    material: Material;
    direction: DirectionType;
    onRefresh: () => void;
};

export function MaterialDetailsHeader({ material, direction, onRefresh }: MaterialDetailsHeaderProps) {
    const { openDialog } = useDataModal();
    return (
        <>
            <Stack direction="row" alignItems="center" spacing={1} width="100%">
                <IconButton>
                    <ChevronLeftOutlined />
                </IconButton>
                <Typography variant="h2" component="h1" marginRight="auto !important">
                    Production Information for {material?.name}
                </Typography>
                <Button sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }} onClick={onRefresh}>
                    <Refresh></Refresh> Refresh
                </Button>
                {direction === DirectionType.Outbound ? (
                    <Button sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }} onClick={() => openDialog('production', {}, 'create')}>
                        <Add></Add> Add Production
                    </Button>
                ) : (
                    <Button sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }} onClick={() => openDialog('demand', {}, 'create')}>
                        <Add></Add> Add Demand
                    </Button>
                )}
                <Button
                    sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                    onClick={() =>
                        openDialog(
                            'delivery',
                            { departureType: 'estimated-departure', arrivalType: 'estimated-arrival' },
                            'create',
                            direction?.toUpperCase() === 'INBOUND' ? 'incoming' : 'outgoing',
                            null
                        )
                    }
                >
                    <Add></Add> Add Delivery
                </Button>
            </Stack>
        </>
    );
}
