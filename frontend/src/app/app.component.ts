import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'PitchMind Intelligence Portal';

  // Auth State
  isLoggedIn = false;
  token = '';
  user: any = null;
  authUsername = '';
  authPassword = '';
  authRole = 'COACH';
  authMode: 'login' | 'register' = 'login';

  // Navigation State
  activeTab = 'dashboard';

  // Data Lists
  teams: any[] = [];
  selectedTeam: any = null;
  players: any[] = [];
  matches: any[] = [];
  recentMatchesCount = 0;
  totalPlayersCount = 0;

  // Form Inputs: Creation
  teamName = '';
  teamLeague = '';
  teamFormation = '4-3-3';

  playerName = '';
  playerPosition = 'Forward';
  playerRating = 7.0;

  matchHomeId = '';
  matchAwayId = '';
  matchHomeGoals: number | null = null;
  matchAwayGoals: number | null = null;
  matchDate = '';

  statPlayerId = '';
  statMatchId = '';
  statMinutes = 90;
  statGoals = 0;
  statAssists = 0;
  statYellow = 0;
  statRed = false;

  // Form Inputs: AI Operations
  aiMatchId = '';
  aiFocus = 'PRESSING';
  aiStyle = 'POSSESSION';
  aiRisk = 'MEDIUM';

  aiTrainTeamId = '';
  aiTrainStart = '';
  aiTrainEnd = '';
  aiTrainFocus = 'BUILD_UP';
  aiTrainIntensity = 'MEDIUM';

  aiSeasonTeamId = '';
  aiSeasonYear = '2026/2027';
  aiSeasonPriority = 'Balanced';

  // Prediction Lab
  predictionMatchId = '';
  predictionResponse: any = null;
  predictionMarket = 'HOME_WIN';
  predictionDecimalOdds: number | null = 2.1;
  marketEvaluation: any = null;

  // UI Status
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  private pendingRequests = 0;

  // AI Response Holder
  aiResponse: any = null;
  aiResponseType: 'analysis' | 'training' | 'season' | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.checkLogin();
    if (this.isLoggedIn) {
      this.loadInitialData();
    }
  }

  getGqlUrl() {
    return window.location.port === '4200' ? 'http://localhost:8080/graphql' : '/graphql';
  }

  getHeaders() {
    let headers = new HttpHeaders().set('Content-Type', 'application/json');
    if (this.token) {
      headers = headers.set('Authorization', `Bearer ${this.token}`);
    }
    return headers;
  }

  checkLogin() {
    const savedToken = localStorage.getItem('jwt_token');
    const savedUser = localStorage.getItem('jwt_user');
    if (savedToken && savedUser) {
      this.token = savedToken;
      this.user = JSON.parse(savedUser);
      this.isLoggedIn = true;
    }
  }

  loadInitialData() {
    this.loadTeams();
  }

  showSuccess(msg: string) {
    this.successMessage = msg;
    this.errorMessage = '';
    setTimeout(() => this.successMessage = '', 5000);
  }

  showError(msg: string) {
    this.errorMessage = msg;
    this.successMessage = '';
    setTimeout(() => this.errorMessage = '', 7000);
  }

  queryGql(query: string, variables: any = {}): Promise<any> {
    this.pendingRequests++;
    this.isLoading = true;
    return firstValueFrom(this.http.post<any>(
      this.getGqlUrl(),
      JSON.stringify({ query, variables }),
      { headers: this.getHeaders() }
    ))
      .then(res => {
        if (res.errors && res.errors.length > 0) {
          throw new Error(res.errors[0].message);
        }
        return res.data;
      })
      .catch(err => {
        this.showError(err.message || 'GraphQL operation failed');
        throw err;
      })
      .finally(() => {
        this.pendingRequests--;
        this.isLoading = this.pendingRequests > 0;
      });
  }

  // Auth Functions
  handleAuth() {
    if (this.authMode === 'login') {
      const q = `
        mutation($u: String!, $p: String!) {
          login(username: $u, password: $p) {
            token
            user { id username role }
          }
        }
      `;
      this.queryGql(q, { u: this.authUsername, p: this.authPassword })
        .then(data => {
          this.token = data.login.token;
          this.user = data.login.user;
          localStorage.setItem('jwt_token', this.token);
          localStorage.setItem('jwt_user', JSON.stringify(this.user));
          this.isLoggedIn = true;
          this.showSuccess(`Welcome back, ${this.user.username}!`);
          this.loadInitialData();
        })
        .catch(() => {});
    } else {
      const q = `
        mutation($u: String!, $p: String!, $r: String) {
          register(username: $u, password: $p, role: $r) {
            token
            user { id username role }
          }
        }
      `;
      this.queryGql(q, { u: this.authUsername, p: this.authPassword, r: this.authRole })
        .then(data => {
          this.token = data.register.token;
          this.user = data.register.user;
          localStorage.setItem('jwt_token', this.token);
          localStorage.setItem('jwt_user', JSON.stringify(this.user));
          this.isLoggedIn = true;
          this.showSuccess(`Account registered successfully!`);
          this.loadInitialData();
        })
        .catch(() => {});
    }
  }

  logout() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('jwt_user');
    this.isLoggedIn = false;
    this.token = '';
    this.user = null;
    this.teams = [];
    this.selectedTeam = null;
    this.players = [];
    this.matches = [];
    this.aiResponse = null;
    this.aiResponseType = null;
  }

  // Loaders
  loadTeams() {
    const q = `
      query {
        teams {
          id
          name
          league
          formation
        }
      }
    `;
    this.queryGql(q).then(data => {
      this.teams = data.teams;
      const firstTeam = this.teams[0];
      if (firstTeam) {
        this.aiTrainTeamId ||= firstTeam.id;
        this.aiSeasonTeamId ||= firstTeam.id;
        this.matchHomeId ||= firstTeam.id;
        this.matchAwayId ||= this.teams.find((team: any) => team.id !== firstTeam.id)?.id || '';
      }
      if (this.teams.length > 0 && !this.selectedTeam) {
        this.selectTeam(firstTeam);
      }
    }).catch(() => {});
  }

  selectTeam(team: any) {
    this.selectedTeam = team;
    this.loadPlayers(team.id);
    this.loadMatches(team.id);
  }

  loadPlayers(teamId: string) {
    const q = `
      query($teamId: ID!) {
        playersByTeam(teamId: $teamId) {
          id
          name
          position
          rating
        }
      }
    `;
    this.queryGql(q, { teamId }).then(data => {
      this.players = data.playersByTeam;
      this.totalPlayersCount = this.players.length;
      const playerIds = new Set(this.players.map((player: any) => player.id));
      if (!this.statPlayerId || !playerIds.has(this.statPlayerId)) {
        this.statPlayerId = this.players[0]?.id || '';
      }
    }).catch(() => {});
  }

  loadMatches(teamId: string) {
    const q = `
      query($teamId: ID!) {
        matchesByTeam(teamId: $teamId, first: 50) {
          edges {
            node {
              id
              homeTeam { id name }
              awayTeam { id name }
              homeGoals
              awayGoals
              date
            }
          }
          totalCount
        }
      }
    `;
    this.queryGql(q, { teamId }).then(data => {
      this.matches = data.matchesByTeam.edges.map((e: any) => e.node);
      this.recentMatchesCount = data.matchesByTeam.totalCount;
      const matchIds = new Set(this.matches.map((match: any) => match.id));
      const firstMatchId = this.matches[0]?.id || '';
      if (!this.aiMatchId || !matchIds.has(this.aiMatchId)) {
        this.aiMatchId = firstMatchId;
      }
      if (!this.statMatchId || !matchIds.has(this.statMatchId)) {
        this.statMatchId = firstMatchId;
      }
      if (!this.predictionMatchId || !matchIds.has(this.predictionMatchId)) {
        this.predictionMatchId = firstMatchId;
      }
      this.predictionResponse = null;
      this.marketEvaluation = null;
    }).catch(() => {});
  }

  // Creation Actions
  createTeam() {
    const q = `
      mutation($n: String!, $l: String, $f: String) {
        createTeam(name: $n, league: $l, formation: $f) {
          id name league formation
        }
      }
    `;
    this.queryGql(q, { n: this.teamName, l: this.teamLeague, f: this.teamFormation })
      .then(data => {
        this.showSuccess(`Team "${data.createTeam.name}" created!`);
        this.teamName = '';
        this.teamLeague = '';
        this.loadTeams();
      })
      .catch(() => {});
  }

  createPlayer() {
    if (!this.selectedTeam) {
      this.showError('Please select a team first');
      return;
    }
    const q = `
      mutation($input: CreatePlayerInput!) {
        createPlayer(input: $input) {
          id name position rating
        }
      }
    `;
    const input = {
      teamId: this.selectedTeam.id,
      name: this.playerName,
      position: this.playerPosition,
      rating: this.playerRating
    };
    this.queryGql(q, { input })
      .then(data => {
        this.showSuccess(`Player "${data.createPlayer.name}" added to roster!`);
        this.playerName = '';
        this.playerRating = 7.0;
        this.loadPlayers(this.selectedTeam.id);
      })
      .catch(() => {});
  }

  recordMatch() {
    if (!this.matchHomeId || !this.matchAwayId) {
      this.showError('Select both teams before recording a match.');
      return;
    }
    if (this.matchHomeId === this.matchAwayId) {
      this.showError('Home and away teams must be different.');
      return;
    }
    const q = `
      mutation($input: MatchInput!) {
        recordMatch(input: $input) {
          id
          homeTeam { name }
          awayTeam { name }
          homeGoals
          awayGoals
        }
      }
    `;
    const input = {
      homeTeamId: this.matchHomeId,
      awayTeamId: this.matchAwayId,
      homeGoals: this.matchHomeGoals,
      awayGoals: this.matchAwayGoals,
      date: this.matchDate
    };
    this.queryGql(q, { input })
      .then(data => {
        this.showSuccess(`Match recorded successfully!`);
        this.matchHomeGoals = null;
        this.matchAwayGoals = null;
        this.matchDate = '';
        if (this.selectedTeam) {
          this.loadMatches(this.selectedTeam.id);
        }
      })
      .catch(() => {});
  }

  recordPlayerStat() {
    if (!this.statPlayerId || !this.statMatchId) {
      this.showError('Select a match and player before saving performance.');
      return;
    }
    const q = `
      mutation($input: PlayerMatchStatInput!) {
        recordPlayerMatchStat(input: $input) {
          id
          player { name }
        }
      }
    `;
    const input = {
      playerId: this.statPlayerId,
      matchId: this.statMatchId,
      minutesPlayed: this.statMinutes,
      goals: this.statGoals,
      assists: this.statAssists,
      yellowCards: this.statYellow,
      redCard: this.statRed
    };
    this.queryGql(q, { input })
      .then(data => {
        this.showSuccess(`Recorded match stat for player "${data.recordPlayerMatchStat.player.name}"`);
        this.statGoals = 0;
        this.statAssists = 0;
        this.statYellow = 0;
        this.statRed = false;
      })
      .catch(() => {});
  }

  // AI Actions
  generateMatchAnalysis() {
    if (!this.aiMatchId) {
      this.showError('Select a match before generating tactics.');
      return;
    }
    const q = `
      mutation($input: MatchAnalysisInput!) {
        generateMatchAnalysis(input: $input) {
          id
          summary
          keyFactors
          focusArea
          style
          riskLevel
        }
      }
    `;
    const input = {
      matchId: this.aiMatchId,
      focusArea: this.aiFocus,
      style: this.aiStyle,
      riskLevel: this.aiRisk
    };
    this.queryGql(q, { input })
      .then(data => {
        this.aiResponse = data.generateMatchAnalysis;
        this.aiResponseType = 'analysis';
        this.showSuccess('PitchMind tactical analysis generated successfully!');
      })
      .catch(() => {});
  }

  generateTrainingPlan() {
    if (!this.aiTrainTeamId) {
      this.showError('Select a team before generating a training plan.');
      return;
    }
    if (!this.aiTrainStart || !this.aiTrainEnd) {
      this.showError('Choose both training week dates.');
      return;
    }
    const start = new Date(`${this.aiTrainStart}T00:00:00Z`);
    const end = new Date(`${this.aiTrainEnd}T00:00:00Z`);
    const rangeDays = Math.round((end.getTime() - start.getTime()) / 86400000);
    if (rangeDays < 0 || rangeDays > 7) {
      this.showError('Training range must end on or after the start date and span no more than 7 days.');
      return;
    }
    const q = `
      mutation($input: TrainingPlanInput!) {
        generateTrainingPlan(input: $input) {
          id
          summary
          weekStart
          weekEnd
          sessions {
            date
            focusArea
            intensity
            durationMinutes
            notes
          }
        }
      }
    `;
    const input = {
      teamId: this.aiTrainTeamId,
      weekStart: this.aiTrainStart,
      weekEnd: this.aiTrainEnd,
      primaryFocus: this.aiTrainFocus,
      intensity: this.aiTrainIntensity
    };
    this.queryGql(q, { input })
      .then(data => {
        this.aiResponse = data.generateTrainingPlan;
        this.aiResponseType = 'training';
        this.showSuccess('Training microcycle generated successfully!');
      })
      .catch(() => {});
  }

  generateSeasonPlan() {
    if (!this.aiSeasonTeamId) {
      this.showError('Select a team before generating a season plan.');
      return;
    }
    if (!/^\d{4}\/\d{2,4}$/.test(this.aiSeasonYear.trim())) {
      this.showError('Season must use a format such as 2026/27.');
      return;
    }
    const q = `
      mutation($input: SeasonPlanInput!) {
        generateSeasonPlan(input: $input) {
          id
          summary
          season
          objectives
          workloadSnapshots {
            player { name position }
            matchesLast28Days
            minutesLast28Days
            fatigueLevel
            injuryRisk
            comment
          }
        }
      }
    `;
    const input = {
      teamId: this.aiSeasonTeamId,
      season: this.aiSeasonYear,
      priority: this.aiSeasonPriority
    };
    this.queryGql(q, { input })
      .then(data => {
        this.aiResponse = data.generateSeasonPlan;
        this.aiResponseType = 'season';
        this.showSuccess('Season plan and workload report generated successfully!');
      })
      .catch(() => {});
  }

  loadMatchPrediction() {
    if (!this.predictionMatchId) {
      this.showError('Select a match before loading a prediction.');
      return;
    }
    const q = `
      query($matchId: ID!) {
        matchPrediction(matchId: $matchId) {
          id
          matchId
          homeTeamId
          awayTeamId
          homeWinProbability
          drawProbability
          awayWinProbability
          expectedHomeGoals
          expectedAwayGoals
          bothTeamsToScoreProbability
          over25GoalsProbability
          under25GoalsProbability
          mostLikelyScore
          confidenceLevel
          uncertaintyLevel
          dataQualityStatus
          explanationFactors
          warnings
          modelName
          modelVersion
          predictionVersion
          featureCutoffTimestamp
          featureSummary
          generatedAt
        }
      }
    `;
    this.queryGql(q, { matchId: this.predictionMatchId })
      .then(data => {
        this.predictionResponse = data.matchPrediction;
        this.marketEvaluation = null;
        if (!this.predictionResponse) {
          this.showSuccess('No prediction has been generated for this match yet.');
        }
      })
      .catch(() => {});
  }

  generateMatchPrediction() {
    if (!this.predictionMatchId) {
      this.showError('Select a match before generating a prediction.');
      return;
    }
    const q = `
      mutation($matchId: ID!) {
        generateMatchPrediction(matchId: $matchId) {
          id
          matchId
          homeTeamId
          awayTeamId
          homeWinProbability
          drawProbability
          awayWinProbability
          expectedHomeGoals
          expectedAwayGoals
          bothTeamsToScoreProbability
          over25GoalsProbability
          under25GoalsProbability
          mostLikelyScore
          confidenceLevel
          uncertaintyLevel
          dataQualityStatus
          explanationFactors
          warnings
          modelName
          modelVersion
          predictionVersion
          featureCutoffTimestamp
          featureSummary
          generatedAt
        }
      }
    `;
    this.queryGql(q, { matchId: this.predictionMatchId })
      .then(data => {
        this.predictionResponse = data.generateMatchPrediction;
        this.marketEvaluation = null;
        this.showSuccess('Prediction generated with transparent baseline model.');
      })
      .catch(() => {});
  }

  evaluateMarketValue() {
    if (!this.predictionResponse) {
      this.showError('Generate or load a prediction before evaluating odds.');
      return;
    }
    const probability = this.selectedMarketProbability;
    if (!probability || probability <= 0) {
      this.showError('The selected market has no model probability. Historical data may be insufficient.');
      return;
    }
    if (!this.predictionDecimalOdds || this.predictionDecimalOdds <= 1) {
      this.showError('Decimal odds must be greater than 1.');
      return;
    }
    const q = `
      mutation($input: MarketValueInput!) {
        evaluateMarketValue(input: $input) {
          predictionId
          market
          modelProbability
          decimalOdds
          fairOdds
          rawImpliedProbability
          expectedValue
          classification
          validationWarnings
          evaluatedAt
        }
      }
    `;
    const input = {
      predictionId: this.predictionResponse.id,
      market: this.predictionMarket,
      modelProbability: probability,
      decimalOdds: this.predictionDecimalOdds
    };
    this.queryGql(q, { input })
      .then(data => {
        this.marketEvaluation = data.evaluateMarketValue;
        this.showSuccess('Market value evaluated by backend service.');
      })
      .catch(() => {});
  }

  formatDate(dateStr: string) {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  get selectedMarketProbability() {
    if (!this.predictionResponse) {
      return null;
    }
    const probabilityByMarket: Record<string, number | null> = {
      HOME_WIN: this.predictionResponse.homeWinProbability,
      DRAW: this.predictionResponse.drawProbability,
      AWAY_WIN: this.predictionResponse.awayWinProbability,
      OVER_2_5: this.predictionResponse.over25GoalsProbability,
      UNDER_2_5: this.predictionResponse.under25GoalsProbability,
      BOTH_TEAMS_TO_SCORE: this.predictionResponse.bothTeamsToScoreProbability
    };
    return probabilityByMarket[this.predictionMarket] ?? null;
  }
}
