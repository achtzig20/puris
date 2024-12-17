import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { CalendarWeekProvider } from '@contexts/calendarWeekContext';
import { Box, capitalize, Stack, Typography } from '@mui/material';
import { MaterialDetailsHeader } from './MaterialDetailsHeader';
import { SummaryPanel } from './SummaryPanel';
import { CollapsibleSummary } from './CollapsibleSummary';
import { useMaterialDetails } from '../hooks/useMaterialDetails';
import { useNotifications } from '@contexts/notificationContext';
import { useDataModal } from '@contexts/dataModalContext';
import { ReactNode, useEffect, useState } from 'react';
import { groupBy } from '@util/helpers';
import { DirectionType } from '@models/types/erp/directionType';
import { createSummary } from '../util/summary-service';
import { Partner } from '@models/types/edc/partner';
import { requestReportedStocks } from '@services/stocks-service';
import { requestReportedDeliveries } from '@services/delivery-service';
import { requestReportedProductions } from '@services/productions-service';
import { requestReportedDemands } from '@services/demands-service';
import { NotFoundView } from '@views/errors/NotFoundView';
import { Material } from '@models/types/data/stock';
import { BPNS } from '@models/types/edc/bpn';

type SummaryContainerProps = {
    children: ReactNode;
};

function SummaryContainer({ children }: SummaryContainerProps) {
    return (
        <Box
            sx={{
                backgroundColor: 'white',
                borderRadius: '.5rem',
                overflow: 'hidden',
                boxShadow: 'rgba(0,0,0,0.1) 0px 1px 3px 0px',
            }}
        >
            <Stack sx={{ overflowX: 'auto', position: 'relative' }}>{children}</Stack>
        </Box>
    );
}

type MaterialDetailsProps = {
    material: Material;
    direction: DirectionType;
};

export function MaterialDetails({ material, direction }: MaterialDetailsProps) {
    const [isRefreshing, setIsRefreshing] = useState(false);
    const { notify } = useNotifications();
    const { addOnSaveListener } = useDataModal();
    const {
        productions,
        demands,
        deliveries,
        stocks,
        expandablePartners,
        sites,
        reportedDemands,
        reportedProductions,
        reportedStocks,
        isLoading,
        refresh,
    } = useMaterialDetails(material.ownMaterialNumber ?? '', direction);
    const incomingDeliveries = deliveries?.filter((d) => sites?.some((site) => site.bpns === d.destinationBpns));
    const outgoingShipments = deliveries?.filter((d) => sites?.some((site) => site.bpns === d.originBpns));
    const groupedProductions = groupBy(productions ?? [], (prod) => prod.productionSiteBpns);
    const groupedDemands = groupBy(demands ?? [], (dem) => dem.demandLocationBpns);
    const groupedIncomingDeliveries = groupBy(incomingDeliveries ?? [], (del) => del.destinationBpns);
    const groupedOutgoingShipments = groupBy(outgoingShipments ?? [], (del) => del.originBpns);
    const groupedStocks = groupBy(stocks ?? [], (stock) => stock.stockLocationBpns);

    useEffect(() => {
        addOnSaveListener((category) => refresh([category]));
    }, [addOnSaveListener, refresh]);

    if (isLoading) {
        return <Typography variant="body1">Loading...</Typography>;
    }

    if (!material) {
        return <NotFoundView />;
    }

    const summary =
        direction === DirectionType.Outbound
            ? createSummary('production', productions ?? [], outgoingShipments ?? [], stocks ?? [])
            : createSummary('demand', demands ?? [], incomingDeliveries ?? [], stocks ?? []);

    const createSummaryByPartnerAndDirection = (partner: Partner, direction: DirectionType, partnerSite?: BPNS, ownSite?: BPNS) => {
        let partnerStocks = reportedStocks?.filter((s) => s.partner.bpnl === partner.bpnl);
        let partnerBpnss = partner.sites.map((s) => s.bpns);
        if (partnerSite) {
            partnerBpnss = partnerBpnss.filter((bpns) => bpns === partnerSite);
            partnerStocks = partnerStocks?.filter((stock) => stock.stockLocationBpns === partnerSite);
        }
        if (direction === DirectionType.Outbound) {
            const demands = reportedDemands?.filter(
                (d) =>
                    d.partnerBpnl === partner.bpnl &&
                    (!partnerSite || d.demandLocationBpns === partnerSite) &&
                    (!ownSite || d.supplierLocationBpns === ownSite)
            );
            const deliveries = outgoingShipments?.filter(
                (d) => partnerBpnss.includes(d.destinationBpns) && (!ownSite || d.originBpns === ownSite)
            );
            return createSummary('demand', demands ?? [], deliveries ?? [], partnerStocks ?? []);
        } else {
            const productions = reportedProductions?.filter(
                (p) => p.partner.bpnl === partner.bpnl && (!partnerSite || p.productionSiteBpns === partnerSite)
            );
            const shipments = incomingDeliveries?.filter(
                (d) => partnerBpnss.includes(d.originBpns) && (!ownSite || d.destinationBpns === ownSite)
            );
            return createSummary('production', productions ?? [], shipments ?? [], partnerStocks ?? []);
        }
    };

    const handleRefresh = () => {
        setIsRefreshing(true);
        Promise.all([
            requestReportedStocks(direction === DirectionType.Outbound ? 'material' : 'product', material.ownMaterialNumber),
            requestReportedDeliveries(material.ownMaterialNumber),
            direction === DirectionType.Inbound
                ? requestReportedProductions(material.ownMaterialNumber)
                : requestReportedDemands(material.ownMaterialNumber),
        ])
            .then(() => {
                notify({
                    title: 'Update requested',
                    description: `Requested update from partners for ${material.ownMaterialNumber}. Please reload dialog later.`,
                    severity: 'success',
                });
            })
            .catch((error: unknown) => {
                const msg =
                    error !== null && typeof error === 'object' && 'message' in error && typeof error.message === 'string'
                        ? error.message
                        : 'Unknown Error';
                notify({
                    title: 'Error requesting update',
                    description: msg,
                    severity: 'error',
                });
            })
            .finally(() => setIsRefreshing(false));
    };

    return (
        <CalendarWeekProvider>
            <Stack spacing={2}>
                <ConfidentialBanner />
                <MaterialDetailsHeader material={material} direction={direction} onRefresh={handleRefresh} />
                <Stack spacing={10}>
                    <SummaryContainer>
                        <SummaryPanel title={`${capitalize(summary.type ?? '')} Summary`} summary={summary} showHeader />
                        {expandablePartners.map((partner) => (
                            <CollapsibleSummary
                                key={partner.bpnl}
                                summary={createSummaryByPartnerAndDirection(partner, direction)}
                                renderTitle={() => (
                                    <>
                                        <Typography variant="body1">{partner.name}</Typography>
                                        <Typography variant="body3" color="#ccc">
                                            ({partner.bpnl})
                                        </Typography>
                                    </>
                                )}
                            >
                                {partner.sites.map((site) => (
                                    <CollapsibleSummary
                                        key={site.bpns}
                                        summary={createSummaryByPartnerAndDirection(partner, direction, site.bpns)}
                                        variant="sub"
                                        renderTitle={() => (
                                            <>
                                                <Typography variant="body2" color="#ccc">
                                                    {partner.name}/
                                                </Typography>
                                                <Typography variant="body1">{site.name}</Typography>
                                                <Typography variant="body3" color="#ccc">
                                                    ({site.bpns})
                                                </Typography>
                                            </>
                                        )}
                                    />
                                ))}
                            </CollapsibleSummary>
                        ))}
                    </SummaryContainer>
                    {sites?.map((site) => (
                        <SummaryContainer key={site.bpns}>
                            <SummaryPanel
                                title={site.name}
                                summary={
                                    direction === DirectionType.Outbound
                                        ? createSummary(
                                              'production',
                                              groupedProductions[site.bpns] ?? [],
                                              groupedOutgoingShipments[site.bpns] ?? [],
                                              groupedStocks[site.bpns] ?? []
                                          )
                                        : createSummary(
                                              'demand',
                                              groupedDemands[site.bpns] ?? [],
                                              groupedIncomingDeliveries[site.bpns] ?? [],
                                              groupedStocks[site.bpns] ?? []
                                          )
                                }
                                showHeader
                            ></SummaryPanel>
                            {expandablePartners.map((partner) => (
                                <CollapsibleSummary
                                    key={partner.bpnl}
                                    summary={createSummaryByPartnerAndDirection(partner, direction, undefined, site.bpns)}
                                    renderTitle={() => (
                                        <>
                                            <Typography variant="body1">{partner.name}</Typography>
                                            <Typography variant="body3" color="#ccc">
                                                ({partner.bpnl})
                                            </Typography>
                                        </>
                                    )}
                                >
                                    {partner.sites.map((partnerSite) => (
                                        <CollapsibleSummary
                                            key={partnerSite.bpns}
                                            summary={createSummaryByPartnerAndDirection(partner, direction, partnerSite.bpns, site.bpns)}
                                            variant="sub"
                                            renderTitle={() => (
                                                <>
                                                    <Typography variant="body2" color="#ccc">
                                                        {partner.name}/
                                                    </Typography>
                                                    <Typography variant="body1">{partnerSite.name}</Typography>
                                                    <Typography variant="body3" color="#ccc">
                                                        ({partnerSite.bpns})
                                                    </Typography>
                                                </>
                                            )}
                                        />
                                    ))}
                                </CollapsibleSummary>
                            ))}
                        </SummaryContainer>
                    ))}
                </Stack>
            </Stack>
        </CalendarWeekProvider>
    );
}
