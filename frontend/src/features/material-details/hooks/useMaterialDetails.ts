import { useDelivery } from '@features/material-details/hooks/useDelivery';
import { useDemand } from '@features/material-details/hooks/useDemand';
import { useProduction } from '@features/material-details/hooks/useProduction';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { useSites } from '@features/stock-view/hooks/useSites';
import { useStocks } from '@features/stock-view/hooks/useStocks';
import { Partner } from '@models/types/edc/partner';
import { DirectionType } from '@models/types/erp/directionType';
import { useEffect, useState } from 'react';
import { Expandable } from '../models/expandable';
import { useReportedProduction } from './useReportedProduction';
import { useReportedDemand } from './useReportedDemand';
import { useReportedStocks } from '@features/stock-view/hooks/useReportedStocks';
import { Production } from '@models/types/data/production';
import { Demand } from '@models/types/data/demand';
import { Delivery } from '@models/types/data/delivery';
import { Stock } from '@models/types/data/stock';

export type DataCategory = 'production' | 'demand' | 'stock' | 'delivery';

export type DataCategoryTypeMap = {
    'production': Production;
    'demand': Demand;
    'delivery': Delivery;
    'stock': Stock;
}

export function useMaterialDetails(materialNumber: string, direction: DirectionType) {
    const { sites, isLoadingSites } = useSites();
    const { productions, isLoadingProductions, refreshProduction } = useProduction(materialNumber ?? null, null);
    const { demands, isLoadingDemands, refreshDemand } = useDemand(materialNumber ?? null, null);
    const { deliveries, isLoadingDeliveries, refreshDelivery } = useDelivery(materialNumber ?? null, null);
    const { stocks, isLoadingStocks, refreshStocks } = useStocks(direction === 'INBOUND' ? 'material' : 'product');
    const { partners, isLoadingPartners } = usePartners(direction === 'INBOUND' ? 'product' : 'material', materialNumber);
    const [expandablePartners, setExpandablePartners] = useState<Expandable<Partner>[]>([]);
    const { reportedProductions, isLoadingReportedProductions } = useReportedProduction(materialNumber ?? null);
    const { reportedDemands, isLoadingReportedDemands } = useReportedDemand(materialNumber ?? null);
    const { reportedStocks, isLoadingReportedStocks } = useReportedStocks(
        direction === 'INBOUND' ? 'product' : 'material',
        materialNumber ?? null
    );

    const refresh = (categoriesToRefresh: DataCategory[]) => {
        categoriesToRefresh.forEach(category => {
            switch(category) {
                case 'production':
                    refreshProduction();
                    break;
                case 'demand':
                    refreshDemand();
                    break;
                case 'delivery':
                    refreshDelivery();
                    break;
                case 'stock':
                    refreshStocks();
                    break;
                default:
                    return;
            }
        })
    }

    const isLoading =
        isLoadingProductions ||
        isLoadingDemands ||
        isLoadingDeliveries ||
        isLoadingStocks ||
        isLoadingPartners ||
        isLoadingSites ||
        isLoadingReportedProductions ||
        isLoadingReportedDemands ||
        isLoadingReportedStocks;
    useEffect(() => {
        if (isLoadingPartners) return;
        setExpandablePartners(partners?.map((p) => ({ isExpanded: false, ...p })) ?? []);
    }, [isLoadingPartners, partners]);
    return {
        isLoading,
        productions,
        demands,
        deliveries,
        stocks,
        sites,
        expandablePartners,
        reportedProductions,
        reportedDemands,
        reportedStocks,
        refresh,
    };
}
