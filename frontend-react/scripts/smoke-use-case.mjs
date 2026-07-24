const graphqlUrl = process.env.PITCHMIND_GRAPHQL_URL ?? 'http://localhost:8080/graphql';
const frontendUrl = process.env.PITCHMIND_FRONTEND_URL ?? 'http://127.0.0.1:5173/';
const stamp = new Date().toISOString().replace(/\D/g, '').slice(0, 14);
const username = `smoke_admin_${stamp}`;
const password = 'SmokePass123!';

let token = '';

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

const register = await gql(
  `
    mutation Register($username: String!, $password: String!, $role: String) {
      register(username: $username, password: $password, role: $role) {
        token
        user { id username role }
      }
    }
  `,
  { username, password, role: 'ADMIN' }
);

token = register.register.token;

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

const homeTeam = (
  await gql(createTeamMutation, {
    name: `Smoke FC ${stamp}`,
    league: 'Smoke League',
    formation: '4-3-3'
  })
).createTeam;

const awayTeam = (
  await gql(createTeamMutation, {
    name: `Control United ${stamp}`,
    league: 'Smoke League',
    formation: '4-2-3-1'
  })
).createTeam;

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

const match = (
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
        homeTeamId: homeTeam.id,
        awayTeamId: awayTeam.id,
        homeGoals: 2,
        awayGoals: 1,
        date: '2026-07-24'
      }
    }
  )
).recordMatch;

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
        matchId: match.id,
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

await assertFrontendAvailable();

console.log(
  JSON.stringify(
    {
      status: 'passed',
      frontendUrl,
      graphqlUrl,
      credentials: { username, password },
      team: homeTeam.name,
      opponent: awayTeam.name,
      player: `${player.name} (${player.position}, rating ${player.rating})`,
      match: `${match.homeTeam.name} ${match.homeGoals}-${match.awayGoals} ${match.awayTeam.name}`,
      stat: `${stat.player.name}: ${stat.minutesPlayed} min, ${stat.goals} goal, ${stat.assists} assist`,
      trainingPlan: trainingPlan.summary,
      sessionCount: trainingPlan.sessions.length
    },
    null,
    2
  )
);
