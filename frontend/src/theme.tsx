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
        warning: {
            main: '#e74444',
            light: '#fdedd6'
        }
    },
    typography: {
        fontFamily: ['Inter', 'sans-serif'].join(','),
        body1: {
            fontSize: '1rem'
        },
        body2: {
            fontSize: '.875rem'
        },
        body3: {
            fontSize: '.75rem',
            fontFamily: ['Inter', 'sans-serif'].join(','),
        },
    },
    components: {
        MuiIconButton: {
            styleOverrides: {
                root: {
                    color: '#000000'
                }
            }
        },
        MuiListItemIcon: {
            styleOverrides: {
                root: {
                    color: 'inherit'
                }
            }
        },
    }
});
export default theme;
