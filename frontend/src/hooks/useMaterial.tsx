import { Material } from '@models/types/data/stock';
import { useFetch } from './useFetch';
import { config } from '@models/constants/config';

export function useMaterial(materialNumber: string) {
    const params = new URLSearchParams();
    params.set('ownMaterialNumber', btoa(materialNumber));
    const { data, error, isLoading, refresh } = useFetch<Material>(config.app.BACKEND_BASE_URL + 'materials?' + params.toString());
    return {
        material: data,
        error,
        isLoading,
        refresh,
    };
}
