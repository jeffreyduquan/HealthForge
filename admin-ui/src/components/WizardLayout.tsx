import { Box, Button, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import SaveIcon from '@mui/icons-material/Save';

interface WizardLayoutProps {
  title: string;
  step: number;
  totalSteps: number;
  stepLabels: string[];
  onBack: () => void;
  onNext: () => void;
  onSave: () => void;
  canNext?: boolean;
  canSave?: boolean;
  saving?: boolean;
  children: React.ReactNode;
}

export default function WizardLayout({
  title,
  step,
  totalSteps,
  stepLabels,
  onBack,
  onNext,
  onSave,
  canNext = true,
  canSave = false,
  saving = false,
  children,
}: WizardLayoutProps) {
  const isLast = step === totalSteps - 1;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header: Title + Step dots */}
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ px: 2, py: 1.5, borderBottom: 1, borderColor: 'divider' }}
      >
        <Typography variant="h6">{title}</Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          {stepLabels.map((label, i) => (
            <Box key={i} sx={{ textAlign: 'center' }}>
              <Box
                sx={{
                  width: 28,
                  height: 28,
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: i === step ? 'primary.main' : i < step ? 'primary.light' : 'grey.300',
                  color: i <= step ? 'white' : 'text.secondary',
                  fontSize: 13,
                  fontWeight: 600,
                  mx: 'auto',
                }}
              >
                {i < step ? '✓' : i + 1}
              </Box>
              <Typography variant="caption" sx={{ mt: 0.3, color: i === step ? 'primary.main' : 'text.secondary' }}>
                {label}
              </Typography>
            </Box>
          ))}
        </Stack>
      </Stack>

      {/* Content */}
      <Box sx={{ flex: 1, overflow: 'auto', p: 3 }}>
        {children}
      </Box>

      {/* Footer: Back / Next / Save */}
      <Stack
        direction="row"
        justifyContent="space-between"
        sx={{ px: 3, py: 2, borderTop: 1, borderColor: 'divider' }}
      >
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={onBack}
          disabled={step === 0}
        >
          Zurück
        </Button>
        {isLast ? (
          <Button
            variant="contained"
            startIcon={<SaveIcon />}
            onClick={onSave}
            disabled={!canSave || saving}
          >
            {saving ? 'Speichert…' : 'Speichern'}
          </Button>
        ) : (
          <Button
            variant="contained"
            endIcon={<ArrowForwardIcon />}
            onClick={onNext}
            disabled={!canNext}
          >
            Weiter
          </Button>
        )}
      </Stack>
    </Box>
  );
}
