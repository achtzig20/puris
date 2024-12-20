import { useParams } from 'react-router-dom';
import { NotFoundView } from './errors/NotFoundView';
import { DirectionType } from '@models/types/erp/directionType';
import { DataModalProvider } from '@contexts/dataModalContext';
import { MaterialDetails } from '@features/material-details/components/MaterialDetails';
import { useMaterial } from '@hooks/useMaterial';
import { Box } from '@mui/material';
import { useTitle } from '@contexts/titleProvider';
import { useEffect } from 'react';

export function MaterialDetailView() {
    const { materialNumber, direction } = useParams();
    const directionType: DirectionType = direction === 'inbound' ? DirectionType.Inbound : DirectionType.Outbound;
    const { material, isLoading } = useMaterial(materialNumber ?? '');
    const { setTitle } = useTitle();

    useEffect(() => {
        if (!isLoading && material) {
            setTitle(`${material.name} (${direction})`);
        }
    }, [direction, isLoading, material, setTitle])

    if (isLoading) {
        return <Box>Loading...</Box>
    }

    if (!['OUTBOUND', 'INBOUND'].includes(direction?.toUpperCase() ?? '') || !materialNumber || !material) {
        return <NotFoundView />;
    }
    return (
        <DataModalProvider material={material}>
            <MaterialDetails material={material} direction={directionType} />
        </DataModalProvider>
    );
}
