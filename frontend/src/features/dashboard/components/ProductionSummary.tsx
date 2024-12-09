import { Delivery } from '@models/types/data/delivery';
import { Production } from '@models/types/data/production';
import { Stock } from '@models/types/data/stock';
import CalendarWeekSummary from './CalendarWeekSummary';
import { CalendarWeek, getCalendarWeek, incrementCalendarWeek } from '@util/date-helpers';
import { useState } from 'react';
import { Box, IconButton, Stack, Typography, useTheme } from '@mui/material';
import { Partner } from '@models/types/edc/partner';
import { InfoOutlined } from '@mui/icons-material';
import SummaryPanel from './SummaryPanel';
import CollapsibleSummary from './CollapsibleSummary';
import { useProduction } from '../hooks/useProduction';
import { useDelivery } from '../hooks/useDelivery';
import { useStocks } from '@features/stock-view/hooks/useStocks';
import { DirectionType } from '@models/types/erp/directionType';
import { createSummary } from '../util/summary-service';

export type Expandable<T extends object> = {
    isExpanded: boolean;
} & T;

function initializeCalendarWeeks(): Expandable<CalendarWeek>[] {
    const today = new Date();
    const numberOfWeeks = today.getUTCDay() === 1 ? 4 : 5;
    const weeks: Expandable<CalendarWeek>[] = [];
    const currentCalendarWeek = getCalendarWeek(today);
    for (let i = 0; i < numberOfWeeks; i++) {
        weeks.push({
            ...incrementCalendarWeek(currentCalendarWeek, i),
            isExpanded: i === 0,
        });
    }
    return weeks;
}

type ProductionSummaryProps = {
    materialNumber: string;
    direction: DirectionType;
};

export default function ProductionSummary({ materialNumber, direction }: ProductionSummaryProps) {
    const theme = useTheme();
    const [calendarWeeks, setCalendarWeeks] = useState<Expandable<CalendarWeek>[]>(() => initializeCalendarWeeks());
    const [partners, setPartners] = useState<Expandable<Partner>[]>([
        {
            isExpanded: false,
            uuid: 'fcf93265-f9ad-4659-ae85-80d3fd222383',
            name: 'Control Unit Creator Inc.',
            edcUrl: 'http://customer-control-plane:8184/api/v1/dsp',
            bpnl: 'BPNL4444444444XX',
            addresses: [],
            sites: [
                {
                    bpns: 'BPNS4444444444XX',
                    name: 'Hamburg',
                    addresses: [],
                },
            ],
        },
        {
            isExpanded: false,
            uuid: 'fcf93265-f9ad-4659-ae85-80d3fd222383',
            name: 'Control Unit Creator Inc. 2.0',
            edcUrl: 'http://customer-control-plane:8184/api/v1/dsp',
            bpnl: 'BPNL4444444444XY',
            addresses: [],
            sites: [
                {
                    bpns: 'BPNS4444444444XX',
                    name: 'Control Unit Creator Production Site',
                    addresses: [],
                },
            ],
        },
    ]);
    const { productions, isLoadingProductions } = useProduction(materialNumber ?? null, null);
    const { deliveries, isLoadingDeliveries } = useDelivery(materialNumber ?? null, null);
    const { stocks, isLoadingStocks } = useStocks(direction === 'INBOUND' ? 'material' : 'product');
    if (isLoadingProductions || isLoadingDeliveries || isLoadingStocks) {
        return <Typography variant="body1">Loading...</Typography>;
    }
    console.log(createSummary(productions ?? [], deliveries ?? [], stocks ?? []));

    const handleExpandedChange = (state: boolean, index: number) => {
        setCalendarWeeks((prev) => prev.map((cw, i) => (i === index ? { ...cw, isExpanded: state } : cw)));
    };
    return (
        <Box sx={{ backgroundColor: 'white', borderRadius: '.5rem', overflow: 'hidden', boxShadow: 'rgba(0,0,0,0.1) 0px 1px 3px 0px' }}>
            <Stack sx={{ overflowX: 'auto', position: 'relative' }}>
                <SummaryPanel calendarWeeks={calendarWeeks} onExpandedChange={handleExpandedChange} showHeader />
                {partners.map((partner) => (
                    <CollapsibleSummary
                        key={partner.bpnl}
                        renderTitle={() => (
                            <>
                                <Typography variant="body1">{partner.name}</Typography>
                                <Typography variant="body3" color="#ccc">
                                    ({partner.bpnl})
                                </Typography>
                            </>
                        )}
                        calendarWeeks={calendarWeeks}
                        onExpandedChange={handleExpandedChange}
                    >
                        {partner.sites.map((site) => (
                            <CollapsibleSummary
                                key={site.bpns}
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
                                calendarWeeks={calendarWeeks}
                                onExpandedChange={handleExpandedChange}
                            />
                        ))}
                    </CollapsibleSummary>
                ))}
            </Stack>
        </Box>
    );
}
