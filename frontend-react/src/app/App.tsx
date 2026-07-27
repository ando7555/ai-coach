import {
  Activity,
  AlertTriangle,
  BarChart3,
  Brain,
  CheckCircle2,
  LineChart,
  Loader2,
  LogOut,
  RefreshCcw,
  ShieldCheck,
  Target,
  Users
} from 'lucide-react';
import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { graphQLClient } from '../api/graphqlClient';
import { AuthProvider, useAuth } from '../auth/AuthContext';

type GoogleCredentialResponse = {
  credential?: string;
};

type GoogleAccounts = {
  id: {
    initialize: (options: { client_id: string; callback: (response: GoogleCredentialResponse) => void }) => void;
    renderButton: (element: HTMLElement, options: Record<string, string | number | boolean>) => void;
  };
};

declare global {
  interface Window {
    google?: {
      accounts: GoogleAccounts;
    };
  }
}

type TabId = 'dashboard' | 'teams' | 'matches' | 'prediction' | 'studio';

type Team = {
  id: string;
  name: string;
  league?: string | null;
  formation?: string | null;
};

type Player = {
  id: string;
  name: string;
  position: string;
  rating?: number | null;
};

type Match = {
  id: string;
  homeTeam: Team;
  awayTeam: Team;
  homeGoals?: number | null;
  awayGoals?: number | null;
  date?: string | null;
};

type Prediction = {
  id: string;
  matchId: string;
  homeWinProbability?: number | null;
  drawProbability?: number | null;
  awayWinProbability?: number | null;
  expectedHomeGoals?: number | null;
  expectedAwayGoals?: number | null;
  bothTeamsToScoreProbability?: number | null;
  over25GoalsProbability?: number | null;
  under25GoalsProbability?: number | null;
  mostLikelyScore?: string | null;
  confidenceLevel: string;
  uncertaintyLevel: string;
  dataQualityStatus: string;
  explanationFactors: string[];
  warnings: string[];
  modelName: string;
  modelVersion: string;
  predictionVersion: number;
  featureSummary: string;
};

type MarketEvaluation = {
  predictionId?: string | null;
  market: string;
  modelProbability: number;
  decimalOdds: number;
  fairOdds: number;
  rawImpliedProbability: number;
  expectedValue: number;
  classification: string;
  validationWarnings: string[];
};

type EvaluationSummary = {
  generatedPredictions: number;
  evaluatedPredictions: number;
  correctWinnerPredictions: number;
  winnerAccuracy: number;
  averageBrierScore?: number | null;
  modelVersion?: string | null;
  notes: string[];
};

type AiResponse =
  | {
      type: 'analysis';
      summary: string;
      keyFactors: string[];
      focusArea: string;
      style: string;
      riskLevel: string;
    }
  | {
      type: 'training';
      summary: string;
      weekStart: string;
      weekEnd: string;
      sessions: Array<{
        date: string;
        focusArea: string;
        intensity: string;
        durationMinutes: number;
        notes?: string | null;
      }>;
    }
  | {
      type: 'season';
      summary: string;
      season: string;
      objectives: string[];
      workloadSnapshots: Array<{
        player: Pick<Player, 'name' | 'position'>;
        matchesLast28Days: number;
        minutesLast28Days: number;
        fatigueLevel: string;
        injuryRisk: string;
        comment?: string | null;
      }>;
    };

type MatchAnalysisResponse = Extract<AiResponse, { type: 'analysis' }>;
type TrainingPlanResponse = Extract<AiResponse, { type: 'training' }>;
type SeasonPlanResponse = Extract<AiResponse, { type: 'season' }>;

const navItems = [
  { id: 'dashboard' as const, label: 'Dashboard', icon: BarChart3 },
  { id: 'teams' as const, label: 'Teams & Squads', icon: Users },
  { id: 'matches' as const, label: 'Matches & Stats', icon: Activity },
  { id: 'prediction' as const, label: 'Prediction Lab', icon: LineChart },
  { id: 'studio' as const, label: 'AI Studio', icon: Brain }
];

const teamQuery = `
  query Teams {
    teams { id name league formation }
  }
`;

const teamContextQuery = `
  query TeamContext($teamId: ID!) {
    playersByTeam(teamId: $teamId) { id name position rating }
    matchesByTeam(teamId: $teamId, first: 50) {
      totalCount
      edges {
        node {
          id
          date
          homeGoals
          awayGoals
          homeTeam { id name league formation }
          awayTeam { id name league formation }
        }
      }
    }
  }
`;

function percent(value?: number | null) {
  return value === null || value === undefined ? 'Unavailable' : `${(value * 100).toFixed(1)}%`;
}

function decimal(value?: number | null, fallback = '-') {
  return value === null || value === undefined ? fallback : value.toFixed(2);
}

function formatDate(value?: string | null) {
  if (!value) {
    return 'Unscheduled';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? value
    : parsed.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
}

function fixtureLabel(match: Match) {
  return `${match.homeTeam.name} vs ${match.awayTeam.name}`;
}

function scoreLabel(match: Match) {
  return match.homeGoals === null ||
    match.homeGoals === undefined ||
    match.awayGoals === null ||
    match.awayGoals === undefined
    ? 'Pending'
    : `${match.homeGoals} - ${match.awayGoals}`;
}

function AuthScreen() {
  const { signInWithGoogle } = useAuth();
  const googleButtonRef = useRef<HTMLDivElement | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const configuredGoogleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;
  const [googleClientId, setGoogleClientId] = useState(configuredGoogleClientId ?? '');
  const [configLoaded, setConfigLoaded] = useState(Boolean(configuredGoogleClientId));

  useEffect(() => {
    if (configuredGoogleClientId) {
      return;
    }

    let cancelled = false;

    async function loadPublicConfig() {
      try {
        const response = await fetch('/api/public-config');
        if (!response.ok) {
          throw new Error(`Config request failed with HTTP ${response.status}`);
        }
        const config = (await response.json()) as { googleClientId?: string };
        if (!cancelled) {
          setGoogleClientId(config.googleClientId ?? '');
        }
      } catch (configError) {
        if (!cancelled) {
          setError(configError instanceof Error ? configError.message : 'Could not load Google sign-in configuration');
        }
      } finally {
        if (!cancelled) {
          setConfigLoaded(true);
        }
      }
    }

    loadPublicConfig();

    return () => {
      cancelled = true;
    };
  }, [configuredGoogleClientId]);

  useEffect(() => {
    if (!googleClientId || !googleButtonRef.current) {
      return;
    }

    const clientId = googleClientId;
    let cancelled = false;

    async function handleCredential(response: GoogleCredentialResponse) {
      if (!response.credential) {
        setError('Google did not return an account credential.');
        return;
      }

      setStatus('Signing in with Google');
      setError(null);

      try {
        await signInWithGoogle(response.credential);
      } catch (authError) {
        setError(authError instanceof Error ? authError.message : 'Google sign-in failed');
      } finally {
        setStatus(null);
      }
    }

    function renderGoogleButton() {
      if (cancelled || !googleButtonRef.current || !window.google?.accounts) {
        return;
      }

      googleButtonRef.current.innerHTML = '';
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: handleCredential
      });
      window.google.accounts.id.renderButton(googleButtonRef.current, {
        theme: 'outline',
        size: 'large',
        type: 'standard',
        shape: 'rectangular',
        text: 'continue_with',
        width: Math.min(360, googleButtonRef.current.clientWidth || 360)
      });
    }

    if (window.google?.accounts) {
      renderGoogleButton();
      return () => {
        cancelled = true;
      };
    }

    const scriptId = 'google-identity-services';
    let script = document.getElementById(scriptId) as HTMLScriptElement | null;

    if (!script) {
      script = document.createElement('script');
      script.id = scriptId;
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onerror = () => setError('Could not load Google sign-in. Check browser privacy settings and network access.');
      document.head.appendChild(script);
    }

    script.addEventListener('load', renderGoogleButton);

    return () => {
      cancelled = true;
      script?.removeEventListener('load', renderGoogleButton);
    };
  }, [googleClientId, signInWithGoogle]);

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <img src="/logo-ai-coach.svg" alt="PitchMind logo" />
        <h1>PitchMind Intelligence Portal</h1>
        <p>Use your Google account to access squads, fixtures, prediction readiness, and AI planning workflows.</p>

        {googleClientId ? (
          <div className="google-auth-box">
            <div ref={googleButtonRef} className="google-auth-button" />
            {status && <span className="auth-status">{status}</span>}
          </div>
        ) : !configLoaded ? (
          <div className="google-auth-box">
            <span className="auth-status">Loading Google sign-in</span>
          </div>
        ) : (
          <div className="inline-alert error">
            Google sign-in is not configured. Set GOOGLE_CLIENT_ID on the backend service.
          </div>
        )}

        {error && <div className="inline-alert error">{error}</div>}
      </section>
    </main>
  );
}

function Portal() {
  const { user, isAuthenticated, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<TabId>('dashboard');
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [players, setPlayers] = useState<Player[]>([]);
  const [matches, setMatches] = useState<Match[]>([]);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const [teamForm, setTeamForm] = useState({ name: '', league: '', formation: '4-3-3' });
  const [playerForm, setPlayerForm] = useState({ name: '', position: 'Forward', rating: 7 });
  const [matchForm, setMatchForm] = useState({ homeTeamId: '', awayTeamId: '', homeGoals: '', awayGoals: '', date: '' });
  const [statForm, setStatForm] = useState({
    playerId: '',
    matchId: '',
    minutesPlayed: 90,
    goals: 0,
    assists: 0,
    yellowCards: 0,
    redCard: false
  });
  const [predictionMatchId, setPredictionMatchId] = useState('');
  const [prediction, setPrediction] = useState<Prediction | null>(null);
  const [evaluationSummary, setEvaluationSummary] = useState<EvaluationSummary | null>(null);
  const [market, setMarket] = useState('HOME_WIN');
  const [decimalOdds, setDecimalOdds] = useState(2.1);
  const [marketEvaluation, setMarketEvaluation] = useState<MarketEvaluation | null>(null);
  const [aiResponse, setAiResponse] = useState<AiResponse | null>(null);
  const [analysisForm, setAnalysisForm] = useState({ matchId: '', focusArea: 'PRESSING', style: 'POSSESSION', riskLevel: 'MEDIUM' });
  const [trainingForm, setTrainingForm] = useState({
    teamId: '',
    weekStart: '',
    weekEnd: '',
    primaryFocus: 'BUILD_UP',
    intensity: 'MEDIUM'
  });
  const [seasonForm, setSeasonForm] = useState({ teamId: '', season: '2026/27', priority: 'Balanced' });

  const selectedTeam = useMemo(() => teams.find((team) => team.id === selectedTeamId) ?? null, [teams, selectedTeamId]);
  const completedMatches = useMemo(
    () => matches.filter((match) => match.homeGoals !== null && match.homeGoals !== undefined && match.awayGoals !== null && match.awayGoals !== undefined),
    [matches]
  );
  const scheduledMatches = useMemo(
    () => matches.filter((match) => match.homeGoals === null || match.homeGoals === undefined || match.awayGoals === null || match.awayGoals === undefined),
    [matches]
  );
  const predictionReadiness = completedMatches.length >= 6
    ? 'Ready'
    : completedMatches.length >= 3
      ? 'Limited'
      : 'Needs History';

  function flashSuccess(message: string) {
    setSuccess(message);
    setError('');
    window.setTimeout(() => setSuccess(''), 4500);
  }

  function flashError(message: string) {
    setError(message);
    setSuccess('');
    window.setTimeout(() => setError(''), 6500);
  }

  async function withStatus<T>(operation: () => Promise<T>) {
    setLoading(true);
    try {
      return await operation();
    } catch (operationError) {
      flashError(operationError instanceof Error ? operationError.message : 'Operation failed');
      throw operationError;
    } finally {
      setLoading(false);
    }
  }

  async function loadTeams(preferredTeamId?: string) {
    await withStatus(async () => {
      const data = await graphQLClient.request<{ teams: Team[] }>(teamQuery);
      setTeams(data.teams);
      const nextTeam = selectInitialTeam(data.teams, preferredTeamId);
      setSelectedTeamId(nextTeam?.id ?? '');
      setMatchForm((current) => ({
        ...current,
        homeTeamId: current.homeTeamId || nextTeam?.id || '',
        awayTeamId: current.awayTeamId || data.teams.find((team) => team.id !== nextTeam?.id)?.id || ''
      }));
      setTrainingForm((current) => ({ ...current, teamId: current.teamId || nextTeam?.id || '' }));
      setSeasonForm((current) => ({ ...current, teamId: current.teamId || nextTeam?.id || '' }));
      await loadEvaluationSummary();
    });
  }

  function selectInitialTeam(nextTeams: Team[], preferredTeamId?: string) {
    return (
      nextTeams.find((team) => team.id === preferredTeamId) ??
      nextTeams.find((team) => team.name === 'Warsaw Athletic' && team.league === 'PitchMind Demo League') ??
      nextTeams[0] ??
      null
    );
  }

  async function loadEvaluationSummary() {
    const data = await graphQLClient.request<{ predictionEvaluationSummary: EvaluationSummary }>(
      `
        query PredictionEvaluationSummary {
          predictionEvaluationSummary {
            generatedPredictions
            evaluatedPredictions
            correctWinnerPredictions
            winnerAccuracy
            averageBrierScore
            modelVersion
            notes
          }
        }
      `
    );
    setEvaluationSummary(data.predictionEvaluationSummary);
  }

  async function loadTeamContext(teamId: string) {
    if (!teamId) {
      setPlayers([]);
      setMatches([]);
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{
        playersByTeam: Player[];
        matchesByTeam: { totalCount: number; edges: Array<{ node: Match }> };
      }, { teamId: string }>(teamContextQuery, { teamId });
      const nextMatches = data.matchesByTeam.edges.map((edge) => edge.node);
      const firstMatchId = nextMatches[0]?.id ?? '';
      const firstPlayerId = data.playersByTeam[0]?.id ?? '';

      setPlayers(data.playersByTeam);
      setMatches(nextMatches);
      setStatForm((current) => ({
        ...current,
        matchId: nextMatches.some((match) => match.id === current.matchId) ? current.matchId : firstMatchId,
        playerId: data.playersByTeam.some((player) => player.id === current.playerId) ? current.playerId : firstPlayerId
      }));
      setPredictionMatchId((current) => (nextMatches.some((match) => match.id === current) ? current : firstMatchId));
      setAnalysisForm((current) => ({ ...current, matchId: nextMatches.some((match) => match.id === current.matchId) ? current.matchId : firstMatchId }));
      setPrediction(null);
      setMarketEvaluation(null);
    });
  }

  useEffect(() => {
    if (isAuthenticated) {
      void loadTeams();
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (selectedTeamId) {
      void loadTeamContext(selectedTeamId);
    }
  }, [selectedTeamId]);

  if (!isAuthenticated) {
    return <AuthScreen />;
  }

  async function createTeam(event: FormEvent) {
    event.preventDefault();
    await withStatus(async () => {
      const data = await graphQLClient.request<{ createTeam: Team }, { name: string; league: string; formation: string }>(
        `
          mutation CreateTeam($name: String!, $league: String, $formation: String) {
            createTeam(name: $name, league: $league, formation: $formation) { id name league formation }
          }
        `,
        teamForm
      );
      setTeamForm({ name: '', league: '', formation: '4-3-3' });
      flashSuccess(`Team "${data.createTeam.name}" created`);
      await loadTeams(data.createTeam.id);
    });
  }

  async function createPlayer(event: FormEvent) {
    event.preventDefault();
    if (!selectedTeam) {
      flashError('Select a team first');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ createPlayer: Player }, { input: Record<string, unknown> }>(
        `
          mutation CreatePlayer($input: CreatePlayerInput!) {
            createPlayer(input: $input) { id name position rating }
          }
        `,
        { input: { teamId: selectedTeam.id, ...playerForm } }
      );
      setPlayerForm({ name: '', position: 'Forward', rating: 7 });
      flashSuccess(`Player "${data.createPlayer.name}" added`);
      await loadTeamContext(selectedTeam.id);
    });
  }

  async function recordMatch(event: FormEvent) {
    event.preventDefault();
    if (!matchForm.homeTeamId || !matchForm.awayTeamId) {
      flashError('Select both teams before recording a match');
      return;
    }
    if (matchForm.homeTeamId === matchForm.awayTeamId) {
      flashError('Home and away teams must be different');
      return;
    }

    await withStatus(async () => {
      await graphQLClient.request<{ recordMatch: Match }, { input: Record<string, unknown> }>(
        `
          mutation RecordMatch($input: MatchInput!) {
            recordMatch(input: $input) {
              id homeGoals awayGoals homeTeam { id name } awayTeam { id name }
            }
          }
        `,
        {
          input: {
            homeTeamId: matchForm.homeTeamId,
            awayTeamId: matchForm.awayTeamId,
            homeGoals: matchForm.homeGoals === '' ? null : Number(matchForm.homeGoals),
            awayGoals: matchForm.awayGoals === '' ? null : Number(matchForm.awayGoals),
            date: matchForm.date
          }
        }
      );
      setMatchForm((current) => ({ ...current, homeGoals: '', awayGoals: '', date: '' }));
      flashSuccess('Match recorded');
      if (selectedTeamId) {
        await loadTeamContext(selectedTeamId);
      }
    });
  }

  async function recordPlayerStat(event: FormEvent) {
    event.preventDefault();
    if (!statForm.playerId || !statForm.matchId) {
      flashError('Select a player and match before saving performance');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ recordPlayerMatchStat: { player: Pick<Player, 'name'> } }, { input: Record<string, unknown> }>(
        `
          mutation RecordPlayerStat($input: PlayerMatchStatInput!) {
            recordPlayerMatchStat(input: $input) { id player { name } }
          }
        `,
        { input: statForm }
      );
      setStatForm((current) => ({ ...current, goals: 0, assists: 0, yellowCards: 0, redCard: false }));
      flashSuccess(`Recorded match stat for ${data.recordPlayerMatchStat.player.name}`);
    });
  }

  async function generateMatchPrediction() {
    if (!predictionMatchId) {
      flashError('Select a match before generating a prediction');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ generateMatchPrediction: Prediction }, { matchId: string }>(
        `
          mutation GeneratePrediction($matchId: ID!) {
            generateMatchPrediction(matchId: $matchId) {
              id matchId homeWinProbability drawProbability awayWinProbability expectedHomeGoals expectedAwayGoals
              bothTeamsToScoreProbability over25GoalsProbability under25GoalsProbability mostLikelyScore
              confidenceLevel uncertaintyLevel dataQualityStatus explanationFactors warnings modelName modelVersion
              predictionVersion featureSummary
            }
          }
        `,
        { matchId: predictionMatchId }
      );
      setPrediction(data.generateMatchPrediction);
      setMarketEvaluation(null);
      await loadEvaluationSummary();
      flashSuccess('Prediction generated');
    });
  }

  async function loadPrediction() {
    if (!predictionMatchId) {
      flashError('Select a match before loading a prediction');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ matchPrediction: Prediction | null }, { matchId: string }>(
        `
          query MatchPrediction($matchId: ID!) {
            matchPrediction(matchId: $matchId) {
              id matchId homeWinProbability drawProbability awayWinProbability expectedHomeGoals expectedAwayGoals
              bothTeamsToScoreProbability over25GoalsProbability under25GoalsProbability mostLikelyScore
              confidenceLevel uncertaintyLevel dataQualityStatus explanationFactors warnings modelName modelVersion
              predictionVersion featureSummary
            }
          }
        `,
        { matchId: predictionMatchId }
      );
      setPrediction(data.matchPrediction);
      setMarketEvaluation(null);
      flashSuccess(data.matchPrediction ? 'Latest prediction loaded' : 'No prediction exists for this match yet');
    });
  }

  const selectedMarketProbability = prediction
    ? {
        HOME_WIN: prediction.homeWinProbability,
        DRAW: prediction.drawProbability,
        AWAY_WIN: prediction.awayWinProbability,
        OVER_2_5: prediction.over25GoalsProbability,
        UNDER_2_5: prediction.under25GoalsProbability,
        BOTH_TEAMS_TO_SCORE: prediction.bothTeamsToScoreProbability
      }[market]
    : null;

  async function evaluateMarketValue() {
    if (!prediction) {
      flashError('Generate or load a prediction first');
      return;
    }
    if (!selectedMarketProbability || selectedMarketProbability <= 0) {
      flashError('Selected market has no usable model probability');
      return;
    }
    if (decimalOdds <= 1) {
      flashError('Decimal odds must be greater than 1');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ evaluateMarketValue: MarketEvaluation }, { input: Record<string, unknown> }>(
        `
          mutation EvaluateMarket($input: MarketValueInput!) {
            evaluateMarketValue(input: $input) {
              predictionId market modelProbability decimalOdds fairOdds rawImpliedProbability expectedValue
              classification validationWarnings evaluatedAt
            }
          }
        `,
        {
          input: {
            predictionId: prediction.id,
            market,
            modelProbability: selectedMarketProbability,
            decimalOdds
          }
        }
      );
      setMarketEvaluation(data.evaluateMarketValue);
      flashSuccess('Market value evaluated');
    });
  }

  async function generateAnalysis(event: FormEvent) {
    event.preventDefault();
    if (!analysisForm.matchId) {
      flashError('Select a match before generating analysis');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ generateMatchAnalysis: Omit<MatchAnalysisResponse, 'type'> }, { input: typeof analysisForm }>(
        `
          mutation GenerateAnalysis($input: MatchAnalysisInput!) {
            generateMatchAnalysis(input: $input) { id summary keyFactors focusArea style riskLevel }
          }
        `,
        { input: analysisForm }
      );
      setAiResponse({ type: 'analysis', ...data.generateMatchAnalysis });
      flashSuccess('Tactical analysis generated');
    });
  }

  async function generateTrainingPlan(event: FormEvent) {
    event.preventDefault();
    if (!trainingForm.teamId || !trainingForm.weekStart || !trainingForm.weekEnd) {
      flashError('Select a team and both training dates');
      return;
    }
    const start = new Date(`${trainingForm.weekStart}T00:00:00Z`);
    const end = new Date(`${trainingForm.weekEnd}T00:00:00Z`);
    const days = Math.round((end.getTime() - start.getTime()) / 86400000);
    if (days < 0 || days > 7) {
      flashError('Training range must end after start and span no more than 7 days');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ generateTrainingPlan: Omit<TrainingPlanResponse, 'type'> }, { input: typeof trainingForm }>(
        `
          mutation GenerateTrainingPlan($input: TrainingPlanInput!) {
            generateTrainingPlan(input: $input) {
              id summary weekStart weekEnd sessions { date focusArea intensity durationMinutes notes }
            }
          }
        `,
        { input: trainingForm }
      );
      setAiResponse({ type: 'training', ...data.generateTrainingPlan });
      flashSuccess('Training plan generated');
    });
  }

  async function generateSeasonPlan(event: FormEvent) {
    event.preventDefault();
    if (!seasonForm.teamId) {
      flashError('Select a team before generating a season plan');
      return;
    }
    if (!/^\d{4}\/\d{2,4}$/.test(seasonForm.season.trim())) {
      flashError('Season must use a format such as 2026/27');
      return;
    }

    await withStatus(async () => {
      const data = await graphQLClient.request<{ generateSeasonPlan: Omit<SeasonPlanResponse, 'type'> }, { input: typeof seasonForm }>(
        `
          mutation GenerateSeasonPlan($input: SeasonPlanInput!) {
            generateSeasonPlan(input: $input) {
              id summary season objectives
              workloadSnapshots {
                player { name position }
                matchesLast28Days minutesLast28Days fatigueLevel injuryRisk comment
              }
            }
          }
        `,
        { input: seasonForm }
      );
      setAiResponse({ type: 'season', ...data.generateSeasonPlan });
      flashSuccess('Season plan generated');
    });
  }

  return (
    <div className="app-shell">
      {success && (
        <div className="toast success">
          <CheckCircle2 size={18} aria-hidden="true" />
          {success}
        </div>
      )}
      {error && (
        <div className="toast error">
          <AlertTriangle size={18} aria-hidden="true" />
          {error}
        </div>
      )}
      {loading && (
        <div className="loading-overlay">
          <Loader2 className="spin" size={24} aria-hidden="true" />
          Consulting PitchMind intelligence engine
        </div>
      )}

      <aside className="sidebar" aria-label="Primary">
        <div className="brand-block">
          <img src="/logo-ai-coach.svg" alt="PitchMind logo" />
        </div>
        <div className="identity-card">
          <div className="identity-avatar">
            <ShieldCheck size={18} aria-hidden="true" />
          </div>
          <div>
            <strong>{user?.displayName ?? user?.email ?? 'Analyst'}</strong>
            <small>{user?.email ?? 'Google account'}</small>
            <span>{user?.role ?? 'COACH'}</span>
          </div>
        </div>
        <nav className="nav-stack">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={activeTab === item.id ? 'nav-item active' : 'nav-item'}
              type="button"
              onClick={() => setActiveTab(item.id)}
            >
              <item.icon size={18} aria-hidden="true" />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <button className="logout-button" type="button" onClick={logout}>
          <LogOut size={17} aria-hidden="true" />
          <span>Log out</span>
        </button>
      </aside>

      <main className="content">
        {activeTab === 'dashboard' && (
          <section className="page-grid">
            <PageHeader
              eyebrow="Operations"
              title="Football Intelligence Dashboard"
              description="Squad context, fixture coverage, and prediction readiness from the live GraphQL API."
              action={<RefreshButton onClick={() => selectedTeamId && loadTeamContext(selectedTeamId)} />}
            />
            <TeamSelector teams={teams} value={selectedTeamId} onChange={setSelectedTeamId} />
            <div className="metric-grid">
              <Metric label="Selected Team" value={selectedTeam?.name ?? 'None'} detail={selectedTeam?.league ?? 'League not set'} />
              <Metric label="Roster Size" value={players.length} detail="Players in active squad" />
              <Metric label="Recorded Fixtures" value={matches.length} detail="Loaded for selected team" />
              <Metric label="Prediction Status" value={predictionReadiness} detail={`${completedMatches.length} completed, ${scheduledMatches.length} scheduled`} />
            </div>
            <div className="work-grid">
              <Panel title="Recent Matches">
                <MatchTable matches={matches.slice(0, 5)} />
              </Panel>
              <Panel title="Intelligence Readiness">
                <p className="muted">
                  Use Prediction Lab for transparent probabilities, uncertainty, fair odds, and expected-value math.
                  PitchMind does not promise guaranteed match results or betting returns.
                </p>
                <button className="secondary-button fit" type="button" onClick={() => setActiveTab('prediction')}>
                  Open Prediction Lab
                </button>
              </Panel>
            </div>
            <ModelEvaluationPanel summary={evaluationSummary} />
          </section>
        )}

        {activeTab === 'teams' && (
          <section className="page-grid">
            <PageHeader eyebrow="Squads" title="Teams & Roster Management" />
            <div className="work-grid">
              <Panel title="Register Team">
                <form className="form-stack" onSubmit={createTeam}>
                  <label>
                    Team Name
                    <input value={teamForm.name} onChange={(event) => setTeamForm({ ...teamForm, name: event.target.value })} required />
                  </label>
                  <label>
                    League
                    <input value={teamForm.league} onChange={(event) => setTeamForm({ ...teamForm, league: event.target.value })} />
                  </label>
                  <label>
                    Formation
                    <select value={teamForm.formation} onChange={(event) => setTeamForm({ ...teamForm, formation: event.target.value })}>
                      {['4-3-3', '4-4-2', '3-5-2', '4-2-3-1', '5-3-2'].map((formation) => (
                        <option key={formation} value={formation}>{formation}</option>
                      ))}
                    </select>
                  </label>
                  <button className="primary-button" type="submit">Create Team</button>
                </form>
                <DataTable
                  headers={['Name', 'League', 'Formation']}
                  rows={teams.map((team) => [team.name, team.league ?? '-', team.formation ?? '-'])}
                  empty="No teams registered."
                />
              </Panel>
              <Panel title={`Squad Roster: ${selectedTeam?.name ?? 'None'}`}>
                <form className="form-stack" onSubmit={createPlayer}>
                  <div className="form-row">
                    <label>
                      Player Name
                      <input value={playerForm.name} onChange={(event) => setPlayerForm({ ...playerForm, name: event.target.value })} required />
                    </label>
                    <label>
                      Position
                      <select value={playerForm.position} onChange={(event) => setPlayerForm({ ...playerForm, position: event.target.value })}>
                        {['Goalkeeper', 'Defender', 'Midfielder', 'Forward'].map((position) => (
                          <option key={position} value={position}>{position}</option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Rating
                      <input
                        type="number"
                        min="1"
                        max="10"
                        step="0.1"
                        value={playerForm.rating}
                        onChange={(event) => setPlayerForm({ ...playerForm, rating: Number(event.target.value) })}
                        required
                      />
                    </label>
                  </div>
                  <button className="primary-button" type="submit">Add Player</button>
                </form>
                <DataTable
                  headers={['Name', 'Position', 'Rating']}
                  rows={players.map((player) => [player.name, player.position, player.rating?.toFixed(1) ?? '-'])}
                  empty="No players registered in this squad."
                />
              </Panel>
            </div>
          </section>
        )}

        {activeTab === 'matches' && (
          <section className="page-grid">
            <PageHeader eyebrow="Fixtures" title="Match Fixtures & Player Statistics" />
            <div className="work-grid">
              <Panel title="Record Match Result">
                <form className="form-stack" onSubmit={recordMatch}>
                  <div className="form-row">
                    <label>
                      Home Team
                      <select value={matchForm.homeTeamId} onChange={(event) => setMatchForm({ ...matchForm, homeTeamId: event.target.value })}>
                        {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
                      </select>
                    </label>
                    <label>
                      Away Team
                      <select value={matchForm.awayTeamId} onChange={(event) => setMatchForm({ ...matchForm, awayTeamId: event.target.value })}>
                        {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
                      </select>
                    </label>
                  </div>
                  <div className="form-row">
                    <label>
                      Home Goals
                      <input type="number" min="0" value={matchForm.homeGoals} onChange={(event) => setMatchForm({ ...matchForm, homeGoals: event.target.value })} />
                    </label>
                    <label>
                      Away Goals
                      <input type="number" min="0" value={matchForm.awayGoals} onChange={(event) => setMatchForm({ ...matchForm, awayGoals: event.target.value })} />
                    </label>
                    <label>
                      Match Date
                      <input type="date" value={matchForm.date} onChange={(event) => setMatchForm({ ...matchForm, date: event.target.value })} required />
                    </label>
                  </div>
                  <button className="primary-button" type="submit">Record Match</button>
                </form>
                <MatchTable matches={matches} />
              </Panel>
              <Panel title="Record Player Match Performance">
                <form className="form-stack" onSubmit={recordPlayerStat}>
                  <label>
                    Match
                    <select value={statForm.matchId} onChange={(event) => setStatForm({ ...statForm, matchId: event.target.value })}>
                      {matches.map((match) => <option key={match.id} value={match.id}>{fixtureLabel(match)} ({formatDate(match.date)})</option>)}
                    </select>
                  </label>
                  <label>
                    Player
                    <select value={statForm.playerId} onChange={(event) => setStatForm({ ...statForm, playerId: event.target.value })}>
                      {players.map((player) => <option key={player.id} value={player.id}>{player.name} ({player.position})</option>)}
                    </select>
                  </label>
                  <div className="form-row">
                    <NumberField label="Minutes" value={statForm.minutesPlayed} min={1} max={120} onChange={(value) => setStatForm({ ...statForm, minutesPlayed: value })} />
                    <NumberField label="Goals" value={statForm.goals} min={0} onChange={(value) => setStatForm({ ...statForm, goals: value })} />
                    <NumberField label="Assists" value={statForm.assists} min={0} onChange={(value) => setStatForm({ ...statForm, assists: value })} />
                    <NumberField label="Yellow Cards" value={statForm.yellowCards} min={0} max={2} onChange={(value) => setStatForm({ ...statForm, yellowCards: value })} />
                  </div>
                  <label className="checkbox-row">
                    <input type="checkbox" checked={statForm.redCard} onChange={(event) => setStatForm({ ...statForm, redCard: event.target.checked })} />
                    Red card received
                  </label>
                  <button className="primary-button" type="submit">Save Performance Stat</button>
                </form>
              </Panel>
            </div>
          </section>
        )}

        {activeTab === 'prediction' && (
          <section className="page-grid">
            <PageHeader eyebrow="Model" title="Prediction Lab" description="Transparent statistical baseline predictions from completed pre-match history." />
            <div className="responsible-banner">
              Football outcomes are uncertain. PitchMind shows model probabilities, uncertainty, and value math; it does not promise any result or return.
            </div>
            <ReadinessPanel
              completedMatches={completedMatches.length}
              scheduledMatches={scheduledMatches.length}
              hasTeams={teams.length >= 2}
              hasPlayers={players.length > 0}
            />
            <div className="work-grid">
              <Panel title="Generate Match Prediction">
                <label>
                  Match
                  <select value={predictionMatchId} onChange={(event) => setPredictionMatchId(event.target.value)}>
                    {matches.map((match) => <option key={match.id} value={match.id}>{fixtureLabel(match)} ({formatDate(match.date)})</option>)}
                  </select>
                </label>
                <div className="button-row">
                  <button className="primary-button fit" type="button" onClick={generateMatchPrediction} disabled={!predictionMatchId}>Generate Prediction</button>
                  <button className="secondary-button fit" type="button" onClick={loadPrediction}>Load Latest</button>
                </div>
              </Panel>
              <Panel title="Market Evaluation">
                <label>
                  Market
                  <select value={market} onChange={(event) => setMarket(event.target.value)}>
                    <option value="HOME_WIN">Home Win</option>
                    <option value="DRAW">Draw</option>
                    <option value="AWAY_WIN">Away Win</option>
                    <option value="OVER_2_5">Over 2.5 Goals</option>
                    <option value="UNDER_2_5">Under 2.5 Goals</option>
                    <option value="BOTH_TEAMS_TO_SCORE">Both Teams To Score</option>
                  </select>
                </label>
                <NumberField label="Bookmaker Decimal Odds" value={decimalOdds} min={1.01} step={0.01} onChange={setDecimalOdds} />
                <p className="muted">Selected model probability: {percent(selectedMarketProbability)}</p>
                <button className="primary-button" type="button" onClick={evaluateMarketValue}>Evaluate Market Value</button>
              </Panel>
            </div>
            {prediction && <PredictionCard prediction={prediction} />}
            {marketEvaluation && <MarketCard evaluation={marketEvaluation} />}
          </section>
        )}

        {activeTab === 'studio' && (
          <section className="page-grid">
            <PageHeader eyebrow="AI" title="Intelligence Studio" />
            <div className="three-grid">
              <Panel title="Match Intelligence">
                <form className="form-stack" onSubmit={generateAnalysis}>
                  <label>
                    Match
                    <select value={analysisForm.matchId} onChange={(event) => setAnalysisForm({ ...analysisForm, matchId: event.target.value })}>
                      {matches.map((match) => <option key={match.id} value={match.id}>{fixtureLabel(match)} ({formatDate(match.date)})</option>)}
                    </select>
                  </label>
                  <SelectField label="Tactical Focus" value={analysisForm.focusArea} options={['PRESSING', 'BUILD_UP', 'DEFENCE']} onChange={(value) => setAnalysisForm({ ...analysisForm, focusArea: value })} />
                  <SelectField label="Playing Style" value={analysisForm.style} options={['POSSESSION', 'DIRECT', 'BALANCED']} onChange={(value) => setAnalysisForm({ ...analysisForm, style: value })} />
                  <SelectField label="Risk Tolerance" value={analysisForm.riskLevel} options={['LOW', 'MEDIUM', 'HIGH']} onChange={(value) => setAnalysisForm({ ...analysisForm, riskLevel: value })} />
                  <button className="primary-button" type="submit">Generate Tactical Analysis</button>
                </form>
              </Panel>
              <Panel title="Weekly Training Microcycle">
                <form className="form-stack" onSubmit={generateTrainingPlan}>
                  <TeamSelect teams={teams} value={trainingForm.teamId} onChange={(value) => setTrainingForm((current) => ({ ...current, teamId: value }))} />
                  <div className="form-row">
                    <label>
                      Week Start
                      <input
                        type="date"
                        value={trainingForm.weekStart}
                        onInput={(event) => {
                          const { value } = event.currentTarget;
                          setTrainingForm((current) => ({ ...current, weekStart: value }));
                        }}
                        onChange={(event) => {
                          const { value } = event.currentTarget;
                          setTrainingForm((current) => ({ ...current, weekStart: value }));
                        }}
                      />
                    </label>
                    <label>
                      Week End
                      <input
                        type="date"
                        value={trainingForm.weekEnd}
                        onInput={(event) => {
                          const { value } = event.currentTarget;
                          setTrainingForm((current) => ({ ...current, weekEnd: value }));
                        }}
                        onChange={(event) => {
                          const { value } = event.currentTarget;
                          setTrainingForm((current) => ({ ...current, weekEnd: value }));
                        }}
                      />
                    </label>
                  </div>
                  <SelectField label="Primary Focus" value={trainingForm.primaryFocus} options={['PRESSING', 'BUILD_UP', 'DEFENCE']} onChange={(value) => setTrainingForm((current) => ({ ...current, primaryFocus: value }))} />
                  <SelectField label="Intensity" value={trainingForm.intensity} options={['LOW', 'MEDIUM', 'HIGH']} onChange={(value) => setTrainingForm((current) => ({ ...current, intensity: value }))} />
                  <button className="primary-button" type="submit">Generate Training Schedule</button>
                </form>
              </Panel>
              <Panel title="Season Workload Auditor">
                <form className="form-stack" onSubmit={generateSeasonPlan}>
                  <TeamSelect teams={teams} value={seasonForm.teamId} onChange={(value) => setSeasonForm((current) => ({ ...current, teamId: value }))} />
                  <div className="form-row">
                    <label>
                      Season
                      <input
                        value={seasonForm.season}
                        onChange={(event) => {
                          const { value } = event.currentTarget;
                          setSeasonForm((current) => ({ ...current, season: value }));
                        }}
                      />
                    </label>
                    <label>
                      Priority
                      <select
                        value={seasonForm.priority}
                        onChange={(event) => {
                          const { value } = event.currentTarget;
                          setSeasonForm((current) => ({ ...current, priority: value }));
                        }}
                      >
                        <option value="Balanced">Balanced</option>
                        <option value="Rotation">Rotation</option>
                        <option value="Fitness">Fitness</option>
                      </select>
                    </label>
                  </div>
                  <button className="primary-button" type="submit">Generate Season Plan & Audit</button>
                </form>
              </Panel>
            </div>
            {aiResponse && <AiResponseCard response={aiResponse} />}
          </section>
        )}
      </main>
    </div>
  );
}

function PageHeader({ eyebrow, title, description, action }: { eyebrow: string; title: string; description?: string; action?: React.ReactNode }) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        {description && <p className="page-subtitle">{description}</p>}
      </div>
      {action}
    </header>
  );
}

function RefreshButton({ onClick }: { onClick: () => void }) {
  return (
    <button className="icon-button" type="button" onClick={onClick} aria-label="Refresh">
      <RefreshCcw size={18} aria-hidden="true" />
    </button>
  );
}

function Metric({ label, value, detail }: { label: string; value: React.ReactNode; detail: string }) {
  return (
    <article className="metric-card">
      <span className="metric-label">{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

function ReadinessPanel({
  completedMatches,
  scheduledMatches,
  hasTeams,
  hasPlayers
}: {
  completedMatches: number;
  scheduledMatches: number;
  hasTeams: boolean;
  hasPlayers: boolean;
}) {
  const checks = [
    { label: 'At least two teams', ok: hasTeams },
    { label: 'Roster context', ok: hasPlayers },
    { label: 'Six completed historical fixtures', ok: completedMatches >= 6 },
    { label: 'One scheduled target fixture', ok: scheduledMatches >= 1 }
  ];

  return (
    <section className="readiness-panel">
      <div className="readiness-title">
        <Target size={18} aria-hidden="true" />
        <strong>Prediction Readiness</strong>
      </div>
      <div className="readiness-checks">
        {checks.map((check) => (
          <span className={check.ok ? 'readiness-check ok' : 'readiness-check'} key={check.label}>
            {check.label}
          </span>
        ))}
      </div>
    </section>
  );
}

function ModelEvaluationPanel({ summary }: { summary: EvaluationSummary | null }) {
  if (!summary) {
    return null;
  }

  return (
    <Panel title="Model Evaluation">
      <div className="metric-grid compact">
        <Metric label="Saved Predictions" value={summary.generatedPredictions} detail="All prediction records" />
        <Metric label="Evaluated Fixtures" value={summary.evaluatedPredictions} detail="Predictions with final scores" />
        <Metric label="Winner Accuracy" value={`${(summary.winnerAccuracy * 100).toFixed(1)}%`} detail={`${summary.correctWinnerPredictions} correct outcomes`} />
        <Metric label="Brier Score" value={summary.averageBrierScore === null || summary.averageBrierScore === undefined ? '-' : summary.averageBrierScore.toFixed(3)} detail={summary.modelVersion ?? 'Current model'} />
      </div>
      <FactorList title="Evaluation Notes" items={summary.notes} />
    </Panel>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="work-panel">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function TeamSelector({ teams, value, onChange }: { teams: Team[]; value: string; onChange: (value: string) => void }) {
  return (
    <label className="compact-select">
      Active Team
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
      </select>
    </label>
  );
}

function TeamSelect({ teams, value, onChange }: { teams: Team[]; value: string; onChange: (value: string) => void }) {
  return (
    <label>
      Team
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
      </select>
    </label>
  );
}

function SelectField({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (value: string) => void }) {
  return (
    <label>
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    </label>
  );
}

function NumberField({
  label,
  value,
  min,
  max,
  step,
  onChange
}: {
  label: string;
  value: number;
  min?: number;
  max?: number;
  step?: number;
  onChange: (value: number) => void;
}) {
  return (
    <label>
      {label}
      <input type="number" min={min} max={max} step={step} value={value} onChange={(event) => onChange(Number(event.target.value))} />
    </label>
  );
}

function DataTable({ headers, rows, empty }: { headers: string[]; rows: React.ReactNode[][]; empty: string }) {
  if (!rows.length) {
    return <p className="empty-text">{empty}</p>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={index}>
              {row.map((cell, cellIndex) => <td key={cellIndex}>{cell}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MatchTable({ matches }: { matches: Match[] }) {
  return (
    <DataTable
      headers={['Fixture', 'Score', 'Date']}
      rows={matches.map((match) => [fixtureLabel(match), scoreLabel(match), formatDate(match.date)])}
      empty="No fixtures recorded."
    />
  );
}

function PredictionCard({ prediction }: { prediction: Prediction }) {
  return (
    <Panel title={`Prediction v${prediction.predictionVersion}`}>
      <div className="badge-row">
        <span className={`badge ${prediction.dataQualityStatus.toLowerCase()}`}>{prediction.dataQualityStatus}</span>
        <span className="badge">{prediction.confidenceLevel} confidence</span>
        <span className="badge">{prediction.uncertaintyLevel} uncertainty</span>
      </div>
      {prediction.dataQualityStatus !== 'INSUFFICIENT' ? (
        <>
          <div className="metric-grid compact">
            <Metric label="Home Win" value={percent(prediction.homeWinProbability)} detail="Model probability" />
            <Metric label="Draw" value={percent(prediction.drawProbability)} detail="Model probability" />
            <Metric label="Away Win" value={percent(prediction.awayWinProbability)} detail="Model probability" />
            <Metric label="Expected Goals" value={`${decimal(prediction.expectedHomeGoals)} - ${decimal(prediction.expectedAwayGoals)}`} detail="Baseline xG" />
            <Metric label="BTTS" value={percent(prediction.bothTeamsToScoreProbability)} detail="Both teams to score" />
            <Metric label="Over 2.5" value={percent(prediction.over25GoalsProbability)} detail="Goal market" />
            <Metric label="Under 2.5" value={percent(prediction.under25GoalsProbability)} detail="Goal market" />
            <Metric label="Likely Score" value={prediction.mostLikelyScore ?? '-'} detail={prediction.modelName} />
          </div>
          <FactorList title="Explanation Factors" items={prediction.explanationFactors} />
        </>
      ) : (
        <p className="muted">Historical data is insufficient for a probability prediction. Add completed matches before this fixture date for both teams.</p>
      )}
      <FactorList title="Warnings" items={prediction.warnings} />
    </Panel>
  );
}

function MarketCard({ evaluation }: { evaluation: MarketEvaluation }) {
  return (
    <Panel title="Market Value Result">
      <div className="badge-row">
        <span className="badge">{evaluation.classification}</span>
      </div>
      <div className="metric-grid compact">
        <Metric label="Model Probability" value={percent(evaluation.modelProbability)} detail="PitchMind baseline" />
        <Metric label="Bookmaker Implied" value={percent(evaluation.rawImpliedProbability)} detail="Raw implied probability" />
        <Metric label="Fair Odds" value={evaluation.fairOdds.toFixed(2)} detail="From model probability" />
        <Metric label="Expected Value" value={`${(evaluation.expectedValue * 100).toFixed(1)}%`} detail="Before risk adjustment" />
      </div>
      <FactorList title="Validation Warnings" items={evaluation.validationWarnings} />
    </Panel>
  );
}

function AiResponseCard({ response }: { response: AiResponse }) {
  if (response.type === 'analysis') {
    return (
      <Panel title="Tactical Advice">
        <p className="output-summary">{response.summary}</p>
        <div className="badge-row">
          <span className="badge">{response.focusArea}</span>
          <span className="badge">{response.style}</span>
          <span className="badge">{response.riskLevel}</span>
        </div>
        <FactorList title="Key Actionable Factors" items={response.keyFactors} />
      </Panel>
    );
  }

  if (response.type === 'training') {
    return (
      <Panel title={`Weekly Microcycle: ${formatDate(response.weekStart)} - ${formatDate(response.weekEnd)}`}>
        <p className="output-summary">{response.summary}</p>
        <div className="session-grid">
          {response.sessions.map((session) => (
            <article className="session-card" key={`${session.date}-${session.focusArea}`}>
              <strong>{formatDate(session.date)}</strong>
              <span className="badge">{session.intensity}</span>
              <p>{session.focusArea} session - {session.durationMinutes} mins</p>
              <small>{session.notes}</small>
            </article>
          ))}
        </div>
      </Panel>
    );
  }

  return (
    <Panel title={`Season Objectives & Workload Audit (${response.season})`}>
      <p className="output-summary">{response.summary}</p>
      <FactorList title="Primary Objectives" items={response.objectives} />
      <DataTable
        headers={['Player', 'Position', 'Matches', 'Minutes', 'Fatigue', 'Risk', 'Notes']}
        rows={response.workloadSnapshots.map((snapshot) => [
          snapshot.player.name,
          snapshot.player.position,
          snapshot.matchesLast28Days,
          snapshot.minutesLast28Days,
          snapshot.fatigueLevel,
          snapshot.injuryRisk,
          snapshot.comment ?? '-'
        ])}
        empty="No workload snapshots returned."
      />
    </Panel>
  );
}

function FactorList({ title, items }: { title: string; items: string[] }) {
  if (!items.length) {
    return null;
  }

  return (
    <div className="factor-list">
      <h3>{title}</h3>
      <ul>
        {items.map((item) => <li key={item}>{item}</li>)}
      </ul>
    </div>
  );
}

export function App() {
  return (
    <AuthProvider>
      <Portal />
    </AuthProvider>
  );
}
