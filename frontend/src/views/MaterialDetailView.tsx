import { ConfidentialBanner } from '@components/ConfidentialBanner';
import ProductionSummary from '@features/dashboard/components/ProductionSummary';
import { useMaterial } from '@hooks/useMaterial';
import { ChevronLeftOutlined } from '@mui/icons-material';
import { IconButton, Stack, Typography } from '@mui/material';
import { useParams } from 'react-router-dom';
import { NotFoundView } from './errors/NotFoundView';
import { DirectionType } from '@models/types/erp/directionType';

export default function MaterialDetailView() {
    const { materialNumber, direction } = useParams();
    const { material, isLoading: isLoadingMaterial } = useMaterial(materialNumber!);
    if (!['OUTBOUND', 'INBOUND'].includes(direction?.toUpperCase() ?? '') || !materialNumber) {
        return <NotFoundView />;
    }
    if (isLoadingMaterial) {
        return <Typography variant="body1">Loading...</Typography>
    }

    if (!material) {
        return <NotFoundView />
    }
    
    return (
        <Stack spacing={2}>
            <ConfidentialBanner />
            <Stack direction="row" alignItems="center" spacing={1}>
                <IconButton>
                    <ChevronLeftOutlined />
                </IconButton>
                <Typography variant="h2" component="h1">
                    Production Information for {material?.name}
                </Typography>
            </Stack>
            <ProductionSummary materialNumber={materialNumber}  direction={direction?.toUpperCase() as DirectionType}  />
        </Stack>
    );
}
