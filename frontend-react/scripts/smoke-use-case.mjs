const graphqlUrl = process.env.PITCHMIND_GRAPHQL_URL ?? 'http://localhost:8080/graphql';
const frontendUrl = process.env.PITCHMIND_FRONTEND_URL ?? 'http://127.0.0.1:8080/';
const stamp = new Date().toISOString().replace(/\D/g, '').slice(0, 14);

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

async function assertFrontendAvailable() {
  const response = await fetch(frontendUrl);

  if (!response.ok) {
    throw new Error(`Frontend HTTP ${response.status}`);
  }
}

async function authenticate() {
  if (token) {
    return;
  }

  const googleIdToken = process.env.PITCHMIND_GOOGLE_ID_TOKEN;
  if (!googleIdToken) {
    throw new Error('Set PITCHMIND_AUTH_TOKEN or PITCHMIND_GOOGLE_ID_TOKEN to run the authenticated smoke flow.');
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

await authenticate();

const createTeamMutation = `
  mutation CreateTeam($name: String!, $league: String, $formation: String) {
    createTeam(name: $name, league: $league, formation: $formation) {
      id
      name
      league
      formation
    }
  }
`;

async function createTeam(name, formation) {
  return (
    await gql(createTeamMutation, {
      name,
      league: 'Smoke Premier League',
      formation
    })
  ).createTeam;
}

async function recordMatch(homeTeamId, awayTeamId, homeGoals, awayGoals, date) {
  return (
    await gql(
      `
        mutation RecordMatch($input: MatchInput!) {
          recordMatch(input: $input) {
            id
            homeTeam { id name }
            awayTeam { id name }
            homeGoals
            awayGoals
            date
          }
        }
      `,
      {
        input: {
          homeTeamId,
          awayTeamId,
          homeGoals,
          awayGoals,
          date
        }
      }
    )
  ).recordMatch;
}

const homeTeam = (
  await createTeam(`Smoke FC ${stamp}`, '4-3-3')
);

const awayTeam = await createTeam(`Control United ${stamp}`, '4-2-3-1');
const opponentOne = await createTeam(`Northbridge Athletic ${stamp}`, '4-4-2');
const opponentTwo = await createTeam(`Riverside City ${stamp}`, '3-5-2');
const opponentThree = await createTeam(`Harbour Rangers ${stamp}`, '4-1-4-1');

const player = (
  await gql(
    `
      mutation CreatePlayer($input: CreatePlayerInput!) {
        createPlayer(input: $input) { id name position rating }
      }
    `,
    {
      input: {
        teamId: homeTeam.id,
        name: 'Smoke Forward',
        position: 'Forward',
        rating: 8.1
      }
    }
  )
).createPlayer;

const completedMatch = await recordMatch(homeTeam.id, opponentOne.id, 2, 0, '2026-07-01');
await recordMatch(homeTeam.id, opponentTwo.id, 1, 1, '2026-07-05');
await recordMatch(opponentThree.id, homeTeam.id, 1, 3, '2026-07-09');
await recordMatch(opponentOne.id, awayTeam.id, 0, 1, '2026-07-02');
await recordMatch(opponentTwo.id, awayTeam.id, 2, 2, '2026-07-06');
await recordMatch(awayTeam.id, opponentThree.id, 2, 0, '2026-07-10');

const match = await recordMatch(homeTeam.id, awayTeam.id, null, null, '2026-07-24');

const stat = (
  await gql(
    `
      mutation RecordStat($input: PlayerMatchStatInput!) {
        recordPlayerMatchStat(input: $input) {
          id
          player { name }
          minutesPlayed
          goals
          assists
        }
      }
    `,
    {
      input: {
        playerId: player.id,
        matchId: completedMatch.id,
        minutesPlayed: 90,
        goals: 1,
        assists: 1,
        yellowCards: 0,
        redCard: false
      }
    }
  )
).recordPlayerMatchStat;

const trainingPlan = (
  await gql(
    `
      mutation Training($input: TrainingPlanInput!) {
        generateTrainingPlan(input: $input) {
          id
          summary
          sessions { date focusArea intensity durationMinutes }
        }
      }
    `,
    {
      input: {
        teamId: homeTeam.id,
        weekStart: '2026-07-27',
        weekEnd: '2026-08-02',
        primaryFocus: 'BUILD_UP',
        intensity: 'MEDIUM'
      }
    }
  )
).generateTrainingPlan;

const prediction = (
  await gql(
    `
      mutation Predict($matchId: ID!) {
        generateMatchPrediction(matchId: $matchId) {
          id
          matchId
          homeWinProbability
          drawProbability
          awayWinProbability
          over25GoalsProbability
          dataQualityStatus
          confidenceLevel
          uncertaintyLevel
          warnings
        }
      }
    `,
    { matchId: match.id }
  )
).generateMatchPrediction;

if (prediction.dataQualityStatus !== 'SUFFICIENT') {
  throw new Error(`Expected SUFFICIENT prediction data, got ${prediction.dataQualityStatus}: ${prediction.warnings.join('; ')}`);
}

const marketValue = (
  await gql(
    `
      mutation EvaluateMarket($input: MarketValueInput!) {
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
        }
      }
    `,
    {
      input: {
        predictionId: prediction.id,
        market: 'HOME_WIN',
        modelProbability: prediction.homeWinProbability,
        decimalOdds: 2.1
      }
    }
  )
).evaluateMarketValue;

await assertFrontendAvailable();

console.log(
  JSON.stringify(
    {
      status: 'passed',
      frontendUrl,
      graphqlUrl,
      auth: process.env.PITCHMIND_AUTH_TOKEN ? 'provided-token' : 'google-id-token',
      team: homeTeam.name,
      opponent: awayTeam.name,
      player: `${player.name} (${player.position}, rating ${player.rating})`,
      match: `${match.homeTeam.name} vs ${match.awayTeam.name} on ${match.date}`,
      stat: `${stat.player.name}: ${stat.minutesPlayed} min, ${stat.goals} goal, ${stat.assists} assist`,
      trainingPlan: trainingPlan.summary,
      sessionCount: trainingPlan.sessions.length,
      prediction: {
        dataQualityStatus: prediction.dataQualityStatus,
        confidenceLevel: prediction.confidenceLevel,
        uncertaintyLevel: prediction.uncertaintyLevel,
        homeWinProbability: prediction.homeWinProbability,
        drawProbability: prediction.drawProbability,
        awayWinProbability: prediction.awayWinProbability,
        over25GoalsProbability: prediction.over25GoalsProbability
      },
      marketValue: {
        market: marketValue.market,
        decimalOdds: marketValue.decimalOdds,
        fairOdds: marketValue.fairOdds,
        expectedValue: marketValue.expectedValue,
        classification: marketValue.classification,
        validationWarnings: marketValue.validationWarnings
      }
    },
    null,
    2
  )
);
