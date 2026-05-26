import { createTheme } from '@mui/material/styles';

// Brand seed: #7CB342 Olive/Apple Green — LOCKED in docs/GUI.md §2.
// Full token map will be filled in later sprints.
export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#7CB342',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#55624C',
    },
    background: {
      default: '#FCFDF6',
      paper: '#FFFFFF',
    },
  },
  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily: 'Roboto, "Helvetica Neue", Arial, sans-serif',
  },
});
