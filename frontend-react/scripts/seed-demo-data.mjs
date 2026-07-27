const graphqlUrl = process.env.PITCHMIND_GRAPHQL_URL ?? 'http://localhost:8080/graphql';

let token = process.env.PITCHMIND_AUTH_TOKEN ?? '';

async function gql(query, variables = {}) {
  const response = await fetch(graphqlUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ query, variables })
  });

  if (!response.ok) {
    throw new Error(`GraphQL HTTP ${response.status}`);
  }

  const payload = await response.json();
  if (payload.errors?.length) {
    throw new Error(payload.errors.map((error) => error.message).join('; '));
  }
  return payload.data;
}

async function authenticate() {
  if (token) {
    return;
  }

  const googleIdToken = process.env.PITCHMIND_GOOGLE_ID_TOKEN;
  if (!googleIdToken) {
    throw new Error('Set PITCHMIND_AUTH_TOKEN or PITCHMIND_GOOGLE_ID_TOKEN to seed demo data.');
  }

  const auth = await gql(
    `
      mutation AuthenticateWithGoogle($idToken: String!) {
        authenticateWithGoogle(idToken: $idToken) {
          token
          user { id email displayName role }
        }
      }
    `,
    { idToken: googleIdToken }
  );

  token = auth.authenticateWithGoogle.token;
}

async function allTeams() {
  return (await gql(`query Teams { teams { id name league formation } }`)).teams;
}

async function ensureTeam(name, formation) {
  const existing = (await allTeams()).find((team) => team.name === name);
  if (existing) {
    return existing;
  }
  return (
    await gql(
      `
        mutation CreateTeam($name: String!, $league: String, $formation: String) {
          createTeam(name: $name, league: $league, formation: $formation) { id name league formation }
        }
      `,
      { name, league: 'PitchMind Demo League', formation }
    )
  ).createTeam;
}

async function playersByTeam(teamId) {
  return (await gql(`query Players($teamId: ID!) { playersByTeam(teamId: $teamId) { id name position rating } }`, { teamId })).playersByTeam;
}

async function ensurePlayer(team, name, position, rating) {
  const existing = (await playersByTeam(team.id)).find((player) => player.name === name);
  if (existing) {
    return existing;
  }
  return (
    await gql(
      `
        mutation CreatePlayer($input: CreatePlayerInput!) {
          createPlayer(input: $input) { id name position rating }
        }
      `,
      { input: { teamId: team.id, name, position, rating } }
    )
  ).createPlayer;
}

async function matchesByTeam(teamId) {
  return (
    await gql(
      `
        query Matches($teamId: ID!) {
          matchesByTeam(teamId: $teamId, first: 100) {
            edges {
              node {
                id date homeGoals awayGoals
                homeTeam { id name }
                awayTeam { id name }
              }
            }
          }
        }
      `,
      { teamId }
    )
  ).matchesByTeam.edges.map((edge) => edge.node);
}

async function ensureMatch(homeTeam, awayTeam, homeGoals, awayGoals, date) {
  const existing = (await matchesByTeam(homeTeam.id)).find((match) =>
    match.homeTeam.id === homeTeam.id &&
    match.awayTeam.id === awayTeam.id &&
    match.date === date
  );
  if (existing) {
    return existing;
  }
  return (
    await gql(
      `
        mutation RecordMatch($input: MatchInput!) {
          recordMatch(input: $input) {
            id date homeGoals awayGoals
            homeTeam { id name }
            awayTeam { id name }
          }
        }
      `,
      { input: { homeTeamId: homeTeam.id, awayTeamId: awayTeam.id, homeGoals, awayGoals, date } }
    )
  ).recordMatch;
}

async function statsByMatch(matchId) {
  return (
    await gql(
      `
        query Stats($matchId: ID!) {
          statsByMatch(matchId: $matchId) {
            id
            player { id name }
            goals
            assists
            minutesPlayed
          }
        }
      `,
      { matchId }
    )
  ).statsByMatch;
}

async function ensureStat(player, match, goals, assists) {
  const existing = (await statsByMatch(match.id)).find((stat) => stat.player.id === player.id);
  if (existing) {
    return existing;
  }
  return (
    await gql(
      `
        mutation RecordStat($input: PlayerMatchStatInput!) {
          recordPlayerMatchStat(input: $input) {
            id
            player { id name }
            goals
            assists
            minutesPlayed
          }
        }
      `,
      {
        input: {
          playerId: player.id,
          matchId: match.id,
          minutesPlayed: 90,
          goals,
          assists,
          yellowCards: 0,
          redCard: false
        }
      }
    )
  ).recordPlayerMatchStat;
}

async function ensureTrainingPlan(team) {
  const plans = (
    await gql(
      `
        query Plans($teamId: ID!) {
          trainingPlansByTeam(teamId: $teamId) {
            id
            weekStart
            weekEnd
            summary
            sessions { date focusArea intensity durationMinutes }
          }
        }
      `,
      { teamId: team.id }
    )
  ).trainingPlansByTeam;

  const existing = plans.find((plan) => plan.weekStart?.startsWith('2026-08-03'));
  if (existing) {
    return existing;
  }

  return (
    await gql(
      `
        mutation Training($input: TrainingPlanInput!) {
          generateTrainingPlan(input: $input) {
            id
            weekStart
            weekEnd
            summary
            sessions { date focusArea intensity durationMinutes }
          }
        }
      `,
      {
        input: {
          teamId: team.id,
          weekStart: '2026-08-03',
          weekEnd: '2026-08-09',
          primaryFocus: 'BUILD_UP',
          intensity: 'MEDIUM'
        }
      }
    )
  ).generateTrainingPlan;
}

async function ensurePrediction(match) {
  const latest = (
    await gql(
      `
        query LatestPrediction($matchId: ID!) {
          matchPrediction(matchId: $matchId) {
            id
            homeWinProbability
            drawProbability
            awayWinProbability
            over25GoalsProbability
            dataQualityStatus
            confidenceLevel
            uncertaintyLevel
          }
        }
      `,
      { matchId: match.id }
    )
  ).matchPrediction;

  if (latest) {
    return latest;
  }

  return (
    await gql(
      `
        mutation Predict($matchId: ID!) {
          generateMatchPrediction(matchId: $matchId) {
            id
            homeWinProbability
            drawProbability
            awayWinProbability
            over25GoalsProbability
            dataQualityStatus
            confidenceLevel
            uncertaintyLevel
          }
        }
      `,
      { matchId: match.id }
    )
  ).generateMatchPrediction;
}

async function evaluateMarket(prediction) {
  return (
    await gql(
      `
        mutation EvaluateMarket($input: MarketValueInput!) {
          evaluateMarketValue(input: $input) {
            market
            decimalOdds
            fairOdds
            expectedValue
            classification
            validationWarnings
          }
        }
      `,
      {
        input: {
          predictionId: prediction.id,
          market: 'HOME_WIN',
          modelProbability: prediction.homeWinProbability,
          decimalOdds: 2.25
        }
      }
    )
  ).evaluateMarketValue;
}

await authenticate();

const warsaw = await ensureTeam('Warsaw Athletic', '4-3-3');
const krakow = await ensureTeam('Krakow United', '4-2-3-1');
const gdansk = await ensureTeam('Gdansk Mariners', '4-4-2');
const wroclaw = await ensureTeam('Wroclaw Forge', '3-5-2');
const poznan = await ensureTeam('Poznan Rangers', '4-1-4-1');

const striker = await ensurePlayer(warsaw, 'Marek Zielinski', 'Forward', 8.4);
await ensurePlayer(warsaw, 'Jan Nowak', 'Midfielder', 7.8);
await ensurePlayer(warsaw, 'Piotr Lewandowski', 'Defender', 7.3);
await ensurePlayer(krakow, 'Adam Kowalski', 'Forward', 8.0);
await ensurePlayer(krakow, 'Tomasz Mazur', 'Midfielder', 7.5);

const completed = await ensureMatch(warsaw, gdansk, 2, 0, '2026-07-01');
await ensureMatch(warsaw, wroclaw, 1, 1, '2026-07-05');
await ensureMatch(poznan, warsaw, 1, 3, '2026-07-09');
await ensureMatch(warsaw, poznan, 2, 1, '2026-07-13');
await ensureMatch(wroclaw, warsaw, 0, 2, '2026-07-17');
await ensureMatch(gdansk, krakow, 0, 1, '2026-07-02');
await ensureMatch(wroclaw, krakow, 2, 2, '2026-07-06');
await ensureMatch(krakow, poznan, 2, 0, '2026-07-10');
const evaluatedFixture = await ensureMatch(warsaw, krakow, 2, 1, '2026-07-24');
const target = await ensureMatch(warsaw, krakow, null, null, '2026-08-15');

await ensureStat(striker, completed, 1, 1);
const trainingPlan = await ensureTrainingPlan(warsaw);
await ensurePrediction(evaluatedFixture);
const prediction = await ensurePrediction(target);
const marketValue = await evaluateMarket(prediction);
const evaluation = (await gql(`query Evaluation { predictionEvaluationSummary { generatedPredictions evaluatedPredictions winnerAccuracy averageBrierScore notes } }`)).predictionEvaluationSummary;
const featuredMatches = await matchesByTeam(warsaw.id);
const featuredCompletedMatches = featuredMatches.filter((match) => match.homeGoals !== null && match.awayGoals !== null).length;
const featuredScheduledMatches = featuredMatches.length - featuredCompletedMatches;

console.log(JSON.stringify({
  status: 'demo-ready',
  graphqlUrl,
  auth: process.env.PITCHMIND_AUTH_TOKEN ? 'provided-token' : 'google-id-token',
  featuredTeam: warsaw.name,
  featuredFixture: `${target.homeTeam.name} vs ${target.awayTeam.name} on ${target.date}`,
  readiness: {
    completedMatches: featuredCompletedMatches,
    scheduledMatches: featuredScheduledMatches
  },
  trainingPlan: {
    weekStart: trainingPlan.weekStart,
    weekEnd: trainingPlan.weekEnd,
    sessions: trainingPlan.sessions.length
  },
  prediction,
  marketValue,
  evaluation
}, null, 2));
