import { createTheme } from '@mui/material/styles';

// Histamind Dark-Theme (HealthForge / Ported from Histamind Design System)
// Source Tokens: background #070A12, card #141A26, accent #7C5CFF→#4DD0E1

export const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#4DD0E1',       // Ambient Cyan
      light: '#88FFFF',
      dark: '#00ACC1',
      contrastText: '#070A12',
    },
    secondary: {
      main: '#7C5CFF',       // Ambient Violet
      light: '#B39DDB',
      dark: '#5C3FD9',
      contrastText: '#FFFFFF',
    },
    error: {
      main: '#FF5470',       // Over-UL = hot pink-red
    },
    warning: {
      main: '#FFB454',       // Relax/Amber
    },
    success: {
      main: '#22D3A6',       // Good/Mint
    },
    background: {
      default: '#070A12',    // Deep cosmic near-black
      paper: '#141A26',      // Glassy floor
    },
    text: {
      primary: '#F5F7FA',
      secondary: 'rgba(245,247,250,0.80)',
      disabled: 'rgba(245,247,250,0.50)',
    },
    divider: 'rgba(255,255,255,0.08)',
  },
  shape: {
    borderRadius: 10,
  },
  typography: {
    fontFamily: '"Manrope", "Roboto", "Segoe UI", sans-serif',
    h4: { fontWeight: 700, letterSpacing: '-0.3px' },
    h5: { fontWeight: 700, letterSpacing: '-0.3px' },
    h6: { fontWeight: 600, letterSpacing: '-0.2px' },
    button: { fontWeight: 600, textTransform: 'none' },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          border: '1px solid rgba(255,255,255,0.06)',
          background: 'linear-gradient(180deg, rgba(255,255,255,0.07) 0%, rgba(255,255,255,0.01) 100%)',
          backdropFilter: 'blur(8px)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        contained: {
          background: 'linear-gradient(135deg, #7C5CFF 0%, #4DD0E1 100%)',
          boxShadow: '0 4px 20px rgba(124,92,255,0.35)',
          '&:hover': {
            boxShadow: '0 6px 28px rgba(124,92,255,0.50)',
          },
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          background: 'rgba(7,10,18,0.85)',
          backdropFilter: 'blur(12px)',
          borderBottom: '1px solid rgba(255,255,255,0.06)',
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          background: '#070A12',
          borderRight: '1px solid rgba(255,255,255,0.06)',
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          margin: '2px 8px',
          '&.Mui-selected': {
            background: 'linear-gradient(135deg, rgba(124,92,255,0.20), rgba(77,208,225,0.10))',
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(255,255,255,0.04)',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: 'rgba(255,255,255,0.06)',
          '&:hover': { backgroundColor: 'rgba(255,255,255,0.08)' },
          '&.Mui-focused': { backgroundColor: 'rgba(255,255,255,0.08)' },
        },
        input: {
          color: '#F5F7FA',
          '&::placeholder': { color: 'rgba(245,247,250,0.40)', opacity: 1 },
        },
        notchedOutline: {
          borderColor: 'rgba(255,255,255,0.15)',
        },
      },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: {
          color: 'rgba(245,247,250,0.60)',
          '&.Mui-focused': { color: '#4DD0E1' },
        },
      },
    },
    MuiInputBase: {
      styleOverrides: {
        root: {
          color: '#F5F7FA',
        },
      },
    },
    MuiSelect: {
      styleOverrides: {
        icon: { color: 'rgba(245,247,250,0.60)' },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          color: '#F5F7FA',
        },
      },
    },
  },
});

// Global body styles for the cosmic background
export const globalStyles = {
  'html, body, #root': {
    margin: 0,
    padding: 0,
    minHeight: '100vh',
    background: '#070A12',
    color: '#F5F7FA',
    fontFamily: '"Manrope", "Roboto", "Segoe UI", sans-serif',
    WebkitFontSmoothing: 'antialiased',
  },
  '*': {
    boxSizing: 'border-box' as const,
  },
  '::-webkit-scrollbar': {
    width: 6,
  },
  '::-webkit-scrollbar-track': {
    background: '#070A12',
  },
  '::-webkit-scrollbar-thumb': {
    background: 'rgba(255,255,255,0.10)',
    borderRadius: 3,
  },
};
