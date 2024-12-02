/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0 
*/

import * as React from 'react';
import { styled, useTheme, Theme, CSSObject } from '@mui/material/styles';
import Box from '@mui/material/Box';
import MuiDrawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import Divider from '@mui/material/Divider';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import HomeIcon from '@mui/icons-material/Home';
import CatalogIcon from '@mui/icons-material/Category';
import StockIcon from '@mui/icons-material/Inventory';
import HandshakeIcon from '@mui/icons-material/Handshake';
import SyncAltIcon from '@mui/icons-material/SyncAlt';
import NotificationsIcon from '@mui/icons-material/Notifications';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuIcon from '@mui/icons-material/Menu';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import AuthenticationService from '@services/authentication-service';
import { useAuth } from '@hooks/useAuth';
import { OverridableComponent } from '@mui/material/OverridableComponent';
import { SvgIconTypeMap } from '@mui/material';
import { Role } from '@models/types/auth/role';
import { Link } from 'react-router-dom';

const openedMixin = (theme: Theme): CSSObject => ({
    width: theme.sidebarWidth,
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
    }),
    overflowX: 'hidden',
    position: 'relative',
});

const closedMixin = (theme: Theme): CSSObject => ({
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    width: `calc(${theme.spacing(7)} + 1px)`,
    position: 'relative',

    [theme.breakpoints.up('sm')]: {
        width: `calc(${theme.spacing(8)} + 1px)`,
    },
});

const DrawerHeader = styled('div')(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    paddingLeft: theme.spacing(2.5),
    paddingRight: theme.spacing(2.5),
    ...theme.mixins.toolbar,
}));

const Drawer = styled(MuiDrawer, { shouldForwardProp: (prop) => prop !== 'open' })(({ theme, open }) => ({
    width: theme.sidebarWidth,
    flexShrink: 0,
    whiteSpace: 'nowrap',
    boxSizing: 'border-box',
    ...(open && {
        ...openedMixin(theme),
        '& .MuiDrawer-paper': openedMixin(theme),
    }),
    ...(!open && {
        ...closedMixin(theme),
        '& .MuiDrawer-paper': closedMixin(theme),
    }),
}));

type SideBarItemProps = (
    | {
          variant?: 'link';
          path: string;
      }
    | {
          variant: 'button';
          action?: () => void;
      }
) & {
    name: string;
    icon: React.ReactElement<OverridableComponent<SvgIconTypeMap<{}, 'svg'>>>;
    requiredRoles?: Role[];
};

const sideBarItems: SideBarItemProps[] = [
    { name: 'Dashboard', icon: <HomeIcon />, path: '/dashboard' },
    { name: 'Stocks', icon: <StockIcon />, path: '/stocks' },
    { name: 'Catalog', icon: <CatalogIcon />, path: '/catalog', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'Negotiations', icon: <HandshakeIcon />, path: '/negotiations', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'Transfers', icon: <SyncAltIcon />, path: '/transfers', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'Notifications', icon: <NotificationsIcon />, path: '/notifications' },
    { name: 'Logout', icon: <LogoutIcon />, action: AuthenticationService.logout, variant: 'button' },
];

export default function MiniDrawer() {
    const theme = useTheme();
    const [open, setOpen] = React.useState(true);
    const { hasRole } = useAuth();

    const handleDrawerOpen = () => {
        setOpen(true);
    };

    const handleDrawerClose = () => {
        setOpen(false);
    };

    return (
        <Box sx={{ display: 'flex' }}>
            <Drawer variant="permanent" open={open}>
                <DrawerHeader>
                    {open ? (
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                            <img height="30px" src="puris-logo.svg" alt="Puris icon"></img>

                            <IconButton onClick={handleDrawerClose}>
                                {theme.direction === 'rtl' ? <ChevronRightIcon /> : <ChevronLeftIcon />}
                            </IconButton>
                        </Box>
                    ) : (
                        <IconButton sx={{ px: 0 }} onClick={handleDrawerOpen}>
                            <MenuIcon />
                        </IconButton>
                    )}
                </DrawerHeader>
                <Divider />
                <List>
                    {sideBarItems.map((item) => {
                        if (item.requiredRoles && !hasRole(item.requiredRoles)) return null;

                        return (
                            <ListItem key={item.name} disablePadding sx={{ display: 'block' }}>
                                <ListItemButton
                                    sx={{
                                        minHeight: 48,
                                        justifyContent: open ? 'initial' : 'center',
                                        px: 2.5,
                                    }}
                                    onClick={item.variant === 'button' ? item.action : undefined}
                                    component={'path' in item ? 'a' : 'div'}
                                    {...('path' in item ? { href: item.path } : {})}
                                >
                                    <ListItemIcon
                                        sx={{
                                            minWidth: 0,
                                            mr: open ? 3 : 'auto',
                                            justifyContent: 'center',
                                        }}
                                    >
                                        {item.icon}
                                    </ListItemIcon>
                                    <ListItemText primary={item.name} sx={{ opacity: open ? 1 : 0 }} />
                                </ListItemButton>
                            </ListItem>
                        );
                    })}
                </List>
                <Divider />
                <List>
                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <ListItemButton sx={{ justifyContent: open ? 'initial' : 'center', px: 2.5 }} href="/user-guide">
                            <ListItemText primary="User Guide" sx={{ opacity: open ? 1 : 0 }} />
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <ListItemButton sx={{ justifyContent: open ? 'initial' : 'center', px: 2.5 }} href="/aboutLicense">
                            <ListItemText primary="About License" sx={{ opacity: open ? 1 : 0 }} />
                        </ListItemButton>
                    </ListItem>
                </List>
            </Drawer>
        </Box>
    );
}
