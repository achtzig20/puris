import { CalendarWeek } from '@util/date-helpers';
import { Expandable } from './ProductionSummary';
import { IconButton, Stack, Typography, useTheme } from '@mui/material';
import { InfoOutlined } from '@mui/icons-material';
import CalendarWeekSummary from './CalendarWeekSummary';

type SummaryPanelProps = {
    calendarWeeks: Expandable<CalendarWeek>[];
    showHeader?: boolean;
    onExpandedChange: (state: boolean, index: number) => void;
};

export default function SummaryPanel({ calendarWeeks, onExpandedChange, showHeader = false }: SummaryPanelProps) {
    const theme = useTheme();
    return (
        <Stack direction="row">
            <Stack flex={1} minWidth="12rem" sx={{ position: 'sticky', left: 0, backgroundColor: 'white', zIndex: 100, borderRight: '1px solid #e5e5e5' }}>
                {showHeader && (
                    <>
                        <Stack
                            justifyContent="center"
                            paddingInlineStart=".5rem"
                            sx={{ backgroundColor: theme.palette.primary.dark, color: theme.palette.primary.contrastText, height: '2rem', zIndex: 10 }}
                        >
                            <Typography variant="body1" component="h3">
                                Production Summary
                            </Typography>
                        </Stack>
                        <Stack sx={{ backgroundColor: '#f5f5f5', height: '2.5rem' }}></Stack>
                    </>
                )}
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    Planned Production
                    <IconButton sx={{ padding: 0, fontSize: '1rem', color: '#999' }}>
                        <InfoOutlined />
                    </IconButton>
                </Stack>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    Outgoing Shipments
                    <IconButton sx={{ padding: 0, fontSize: '1rem', color: '#999' }}>
                        <InfoOutlined />
                    </IconButton>
                </Stack>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    Projected Item Stock
                    <IconButton sx={{ padding: 0, fontSize: '1rem', color: '#999' }}>
                        <InfoOutlined />
                    </IconButton>
                </Stack>
            </Stack>
            <Stack direction="row" width="100%">
                {calendarWeeks.map((cw, index) => (
                    <CalendarWeekSummary
                        key={cw.week}
                        isExpanded={cw.isExpanded}
                        cw={cw}
                        onToggleExpanded={(state) => onExpandedChange(state, index)}
                        showHeader={showHeader}
                    ></CalendarWeekSummary>
                ))}
            </Stack>
        </Stack>
    );
}
