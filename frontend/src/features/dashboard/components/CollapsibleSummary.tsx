import { CalendarWeek } from '@util/date-helpers';
import { Expandable } from './ProductionSummary';
import { ReactNode, useState } from 'react';
import { Box, Button, Stack, useTheme } from '@mui/material';
import SummaryPanel from './SummaryPanel';
import { ChevronRightOutlined, SubdirectoryArrowRightOutlined } from '@mui/icons-material';

type CollapsibleSummaryProps = {
    variant?: 'default' | 'sub';
    renderTitle: () => ReactNode;
    calendarWeeks: Expandable<CalendarWeek>[];
    onExpandedChange: (state: boolean, index: number) => void;
    children?: ReactNode;
};

export default function CollapsibleSummary({
    renderTitle,
    calendarWeeks,
    onExpandedChange,
    children,
    variant = 'default',
}: CollapsibleSummaryProps) {
    const theme = useTheme();
    const [isExpanded, setIsExpanded] = useState(false);
    return (
        <Stack width="fit-content" minWidth="100%">
            <Button
                variant="text"
                sx={{ flexGrow: 1, padding: 0, borderRadius: 0, textTransform: 'none' }}
                onClick={() => setIsExpanded((prev) => !prev)}
            >
                <Stack
                    direction="row"
                    alignItems="center"
                    spacing={0.5}
                    sx={{
                        minHeight: '2rem',
                        width: '100%',
                        paddingLeft: '.5rem',
                        verticalAlign: 'middle',
                        backgroundColor: variant === 'default' ? theme.palette.primary.main : theme.palette.primary.light,
                        color: theme.palette.primary.contrastText,
                    }}
                >
                    {<ChevronRightOutlined sx={{ rotate: isExpanded ? '90deg' : '0deg', transition: 'rotate 300ms ease-in-out' }} />}
                    {variant === 'sub' && <SubdirectoryArrowRightOutlined />}
                    {renderTitle()}
                </Stack>
            </Button>
            <Box
                sx={{
                    display: 'grid',
                    gridTemplateRows: isExpanded ? '1fr' : '0',
                    overflowY: 'hidden',
                    transition: 'all 300ms',
                }}
            >
                <Box width="100%">
                    <SummaryPanel calendarWeeks={calendarWeeks} onExpandedChange={onExpandedChange} />
                    {children}
                </Box>
            </Box>
        </Stack>
    );
}
