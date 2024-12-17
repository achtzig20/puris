import { ReactNode, useState } from 'react';
import { Button, Stack, useTheme } from '@mui/material';
import { SummaryPanel } from './SummaryPanel';
import { ChevronRightOutlined, SubdirectoryArrowRightOutlined } from '@mui/icons-material';
import { Summary, SummaryType } from '../util/summary-service';

type CollapsibleSummaryProps<TType extends SummaryType> = {
    variant?: 'default' | 'sub';
    summary: Summary<TType>;
    renderTitle: () => ReactNode;
    children?: ReactNode;
};

export function CollapsibleSummary<TType extends SummaryType>({
    summary,
    renderTitle,
    children,
    variant = 'default',
}: CollapsibleSummaryProps<TType>) {
    const theme = useTheme();
    const [isExpanded, setIsExpanded] = useState(false);
    return (
        <>
            <Button
                variant="text"
                sx={{ flexGrow: 1, padding: 0, borderRadius: 0, textTransform: 'none', mindWidth: "100%", position: 'sticky', left: 0, display: 'flex' }}
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

            <SummaryPanel sx={{display: isExpanded ? 'flex' : 'none'}} summary={summary} />

            {isExpanded ? children : null}
        </>
    );
}
