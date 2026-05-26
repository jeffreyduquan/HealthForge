import { useState } from 'react';
import {
  AppBar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import DashboardIcon from '@mui/icons-material/Dashboard';
import MailOutlineIcon from '@mui/icons-material/MailOutline';
import FlagIcon from '@mui/icons-material/Flag';
import ScienceIcon from '@mui/icons-material/Science';
import RestaurantMenuIcon from '@mui/icons-material/RestaurantMenu';
import EditNoteIcon from '@mui/icons-material/EditNote';
import GroupIcon from '@mui/icons-material/Group';
import HistoryIcon from '@mui/icons-material/History';
import QueryStatsIcon from '@mui/icons-material/QueryStats';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { logout } from '../api/client';

const DRAWER_WIDTH = 240;

interface NavItem {
  path: string;
  label: string;
  icon: React.ReactNode;
}

const NAV_ITEMS: NavItem[] = [
  { path: '/', label: 'Dashboard', icon: <DashboardIcon /> },
  { path: '/statistics', label: 'Statistik', icon: <QueryStatsIcon /> },
  { path: '/audit', label: 'Audit-Log', icon: <HistoryIcon /> },
  { path: '/invites', label: 'Einladungen', icon: <MailOutlineIcon /> },
  { path: '/reports', label: 'Reports', icon: <FlagIcon /> },
  { path: '/supplements', label: 'Supplements', icon: <ScienceIcon /> },
  { path: '/ingredients', label: 'Zutaten', icon: <RestaurantMenuIcon /> },
  { path: '/field-prs', label: 'Field-PRs', icon: <EditNoteIcon /> },
  { path: '/users', label: 'Nutzer', icon: <GroupIcon /> },
];

export default function Layout({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(true);
  const navigate = useNavigate();
  const location = useLocation();
  const onLogout = async () => {
    await logout();
    navigate('/login');
  };
  const drawer = (
    <Box role="navigation">
      <Toolbar>
        <Typography variant="h6" component={Link} to="/" sx={{ color: 'inherit', textDecoration: 'none' }}>
          HealthForge
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {NAV_ITEMS.map((item) => (
          <ListItemButton
            key={item.path}
            component={Link}
            to={item.path}
            selected={location.pathname === item.path}
          >
            <ListItemIcon>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
        <Divider sx={{ my: 1 }} />
        <ListItemButton onClick={onLogout}>
          <ListItemIcon><LogoutIcon /></ListItemIcon>
          <ListItemText primary="Abmelden" />
        </ListItemButton>
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { sm: open ? `calc(100% - ${DRAWER_WIDTH}px)` : '100%' },
          ml: { sm: open ? `${DRAWER_WIDTH}px` : 0 },
        }}
      >
        <Toolbar>
          <IconButton color="inherit" edge="start" onClick={() => setOpen((v) => !v)} sx={{ mr: 2 }}>
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap>HealthForge Admin</Typography>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="persistent"
        open={open}
        sx={{
          width: open ? DRAWER_WIDTH : 0,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        {drawer}
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 0, minHeight: '100vh' }}>
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
