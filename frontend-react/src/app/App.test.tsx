import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

function jsonResponse(payload: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => payload
  } as Response;
}

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders the auth screen when no token exists', () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ googleClientId: '' })));

    render(<App />);

    expect(screen.getByText('PitchMind Intelligence Portal')).toBeInTheDocument();
    return waitFor(() => expect(screen.getByText(/Google sign-in is not configured/)).toBeInTheDocument());
  });

  it('loads the migrated React dashboard from GraphQL when authenticated', async () => {
    localStorage.setItem('jwt_token', 'token');
    localStorage.setItem(
      'jwt_user',
      JSON.stringify({
        username: 'coach@example.com',
        email: 'coach@example.com',
        displayName: 'Coach Analyst',
        role: 'ADMIN'
      })
    );

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({
          data: {
            teams: [{ id: 'team-1', name: 'Arsenal FC', league: 'Premier League', formation: '4-3-3' }]
          }
        })
      )
      .mockResolvedValueOnce(
        jsonResponse({
          data: {
            predictionEvaluationSummary: {
              generatedPredictions: 3,
              evaluatedPredictions: 1,
              correctWinnerPredictions: 1,
              winnerAccuracy: 1,
              averageBrierScore: 0.18,
              modelVersion: 'baseline-poisson-v1',
              notes: ['Evaluation uses the latest saved prediction per match with a completed result.']
            }
          }
        })
      )
      .mockResolvedValueOnce(
        jsonResponse({
          data: {
            playersByTeam: [{ id: 'player-1', name: 'Bukayo Saka', position: 'Forward', rating: 8.4 }],
            matchesByTeam: {
              totalCount: 1,
              edges: [
                {
                  node: {
                    id: 'match-1',
                    date: '2026-07-24',
                    homeGoals: 2,
                    awayGoals: 1,
                    homeTeam: { id: 'team-1', name: 'Arsenal FC' },
                    awayTeam: { id: 'team-2', name: 'Chelsea FC' }
                  }
                }
              ]
            }
          }
        })
      );

    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    await waitFor(() => expect(screen.getAllByText('Arsenal FC').length).toBeGreaterThan(0));

    expect(screen.getByText('Football Intelligence Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Roster Size')).toBeInTheDocument();
    expect(screen.getByText('Model Evaluation')).toBeInTheDocument();

    await waitFor(() => expect(screen.getByText('Arsenal FC vs Chelsea FC')).toBeInTheDocument());
  });
});
