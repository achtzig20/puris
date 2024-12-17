import { CloseFullscreenOutlined, OpenInFullOutlined } from '@mui/icons-material';
import { Box, Grid, IconButton, Stack, Typography, useTheme } from '@mui/material';
import { CalendarWeek, incrementDate } from '@util/date-helpers';
import { useState } from 'react';
import { Summary, SummaryType } from '../util/summary-service';

type CalendarWeekSummaryProps<TType extends SummaryType> = {
    cw: CalendarWeek;
    summary: Summary<TType>;
    isExpanded: boolean;
    showHeader: boolean;
    onToggleExpanded?: (state: boolean) => void;
};

export function CalendarWeekSummary<TType extends SummaryType>({
    cw,
    summary,
    isExpanded,
    onToggleExpanded,
    showHeader = false,
}: CalendarWeekSummaryProps<TType>) {
    const theme = useTheme();
    const [weekDates] = useState<Date[]>(() =>
        Array.from(new Array(7).keys()).map((key) => incrementDate(cw.startDate, key))
    );
    return (
        <Stack flex={isExpanded ? 50 : 10} sx={{ borderRight: '1px solid #e5e5e5', minWidth: isExpanded ? '34rem' : '9rem' }}>
            {showHeader && (
                <Stack
                    direction="row"
                    width="100%"
                    height="2.25rem"
                    sx={{ backgroundColor: isExpanded ? theme.palette.primary.main : theme.palette.primary.dark, position: 'relative' }}
                    justifyContent="center"
                    alignItems="center"
                >
                    <Typography variant="body2" component="h4" color={theme.palette.primary.contrastText}>{`CW ${cw.week
                        .toString()
                        .padStart(2, '0')}`}</Typography>
                    <IconButton
                        sx={{ position: 'absolute', top: '0.125rem', right: '0.125rem', color: theme.palette.primary.contrastText, fontSize: '1rem' }}
                        onClick={() => onToggleExpanded?.(!isExpanded)}
                    >
                        {isExpanded ? <CloseFullscreenOutlined /> : <OpenInFullOutlined />}
                    </IconButton>
                </Stack>
            )}
            <Grid container columns={8} flex={1} height="100%">
                {isExpanded ? (
                    <>
                        {weekDates.map((date) => (
                            <Grid key={date.toLocaleDateString()} item xs={1} height="100%">
                                <Stack height="100%">
                                    {showHeader && (
                                        <Stack
                                            height="2.5rem"
                                            display="flex"
                                            justifyContent="center"
                                            alignItems="center"
                                            spacing={0.25}
                                            sx={{ backgroundColor: '#f5f5f5' }}
                                        >
                                            <Typography variant="body2">
                                                {date.toLocaleDateString(undefined, {weekday: 'long'})}
                                            </Typography>
                                            <Typography variant="body2" color="#777">
                                                {date.toLocaleDateString()}
                                            </Typography>
                                        </Stack>
                                    )}
                                    <Box flex={1} display="flex" justifyContent="center" alignItems="center">
                                        <Typography variant="body2">
                                            {summary.dailySummaries[date.toLocaleDateString()]?.primaryValueTotal}
                                        </Typography>
                                    </Box>
                                    <Box flex={1} display="flex" justifyContent="center" alignItems="center">
                                        <Typography variant="body2">
                                            {summary.dailySummaries[date.toLocaleDateString()]?.deliveryTotal}
                                        </Typography>
                                    </Box>
                                    <Box flex={1} display="flex" justifyContent="center" alignItems="center">
                                        <Typography
                                            variant="body2"
                                            color={
                                                summary.dailySummaries[date.toLocaleDateString()]?.stockTotal < 0 ? '#f44336bb' : 'inherit'
                                            }
                                        >
                                            {summary.dailySummaries[date.toLocaleDateString()]?.stockTotal}
                                        </Typography>
                                    </Box>
                                </Stack>
                            </Grid>
                        ))}
                    </>
                ) : null}
                <Grid item xs={isExpanded ? 1 : 8} height="100%">
                    <Stack
                        height="100%"
                        sx={{ borderLeft: isExpanded ? '1px solid #e5e5e5' : 'none', backgroundColor: isExpanded ? 'white' : '#f5f5f5' }}
                    >
                        {showHeader && (
                            <Box
                                height="2.5rem"
                                display="flex"
                                justifyContent="center"
                                alignItems="center"
                                sx={{ backgroundColor: '#f5f5f5' }}
                            >
                                <Stack alignItems="center" spacing={0.25}>
                                    <Typography variant="body2">Summary</Typography>
                                    {!isExpanded && (
                                        <Typography variant="body3" color="#777">{`${cw.startDate.toLocaleDateString()} - ${incrementDate(
                                            cw.startDate,
                                            6
                                        ).toLocaleDateString()}`}</Typography>
                                    )}
                                </Stack>
                            </Box>
                        )}
                        <Box flex={1} display="flex" justifyContent="center" alignItems="center">
                            <Typography variant="body2">
                                {weekDates.reduce(
                                    (total, date) => total + (summary.dailySummaries[date.toLocaleDateString()]?.primaryValueTotal ?? 0),
                                    0
                                )}
                            </Typography>
                        </Box>
                        <Box flex={1} display="flex" justifyContent="center" alignItems="center">
                            <Typography variant="body2">
                                {weekDates.reduce(
                                    (total, date) => total + (summary.dailySummaries[date.toLocaleDateString()]?.deliveryTotal ?? 0),
                                    0
                                )}
                            </Typography>
                        </Box>
                        <Box flex={1} display="flex" justifyContent="center" alignItems="center">
                            <Typography
                                variant="body2"
                                color={
                                    ([...weekDates]
                                        .reverse()
                                        .map((date) => summary.dailySummaries[date.toLocaleDateString()]?.stockTotal)
                                        .find((sum) => sum) ?? 0) < 0
                                        ? '#f44336bb'
                                        : 'inherit'
                                }
                            >
                                {
                                    [...weekDates]
                                        .reverse()
                                        .map((date) => summary.dailySummaries[date.toLocaleDateString()])
                                        .find((sum) => sum)?.stockTotal
                                }
                            </Typography>
                        </Box>
                    </Stack>
                </Grid>
            </Grid>
        </Stack>
    );
}
