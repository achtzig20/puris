import { createTheme } from '@mui/material/styles';

declare module '@mui/material/styles' {
    interface Theme {
        sidebarWidth: number;
    }
    interface ThemeOptions {
        sidebarWidth?: number;
    }
}

const theme = createTheme({
    palette: {
        primary: {
            dark: '#081F4B',
            main: '#264580',
            light: '#0085FF',
            contrastText: '#fff',
        },
        background: {
            default: '#F5F5F7',
            paper: '#fff',
        },
    },
    typography: {
        fontFamily: 'Inter, sans-serif',
    },
});
export default theme;
