import { IconButton, Stack, SxProps, Theme, Typography, useTheme } from '@mui/material';
import { InfoOutlined } from '@mui/icons-material';
import { CalendarWeekSummary } from '../../material-details/components/CalendarWeekSummary';
import { Summary, SummaryType } from '../util/summary-service';
import { useCalendarWeeks } from '@contexts/calendarWeekContext';

type SummaryPanelProps<TType extends SummaryType> = {
    sx?: SxProps<Theme>
    title?: string;
    summary: Summary<TType>;
    showHeader?: boolean;
};

export function SummaryPanel<TType extends SummaryType>({ sx = {}, title, summary, showHeader = false }: SummaryPanelProps<TType>) {
    const theme = useTheme();
    const { calendarWeeks, expandWeek } = useCalendarWeeks();
    return (
        <Stack direction="row" sx={sx}>
            <Stack
                flex={1}
                minWidth="12rem"
                sx={{ position: 'sticky', left: 0, backgroundColor: 'white', zIndex: 100, borderRight: '1px solid #e5e5e5'}}
            >
                {showHeader && (
                    <>
                        <Stack
                            justifyContent="center"
                            paddingInlineStart=".5rem"
                            sx={{
                                backgroundColor: theme.palette.primary.dark,
                                color: theme.palette.primary.contrastText,
                                height: '2.25rem',
                                zIndex: 10,
                            }}
                        >
                            <Typography variant={title && title.length < 30 ? 'body1' : 'body2'} component="h3" textAlign={title && title.length < 30 ? 'start' : 'start'}>
                                {title}
                            </Typography>
                        </Stack>
                        <Stack sx={{ backgroundColor: '#f5f5f5', height: '2.5rem' }}></Stack>
                    </>
                )}
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    {summary.type === 'production' ? 'Planned Production' : 'Material Demand'}
                    <IconButton sx={{ padding: 0, fontSize: '1rem', color: '#999' }}>
                        <InfoOutlined />
                    </IconButton>
                </Stack>
                <Stack direction="row" alignItems="center" gap={0.75} flexGrow={1} padding=".75rem .5rem">
                    {summary.type === 'production' ? 'Outgoing Shipments' : 'Incoming Deliveries'}
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
                        summary={summary}
                        isExpanded={cw.isExpanded}
                        cw={cw}
                        onToggleExpanded={(state) => expandWeek(state, index)}
                        showHeader={showHeader}
                    ></CalendarWeekSummary>
                ))}
            </Stack>
        </Stack>
    );
}
