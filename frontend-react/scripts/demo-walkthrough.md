# PitchMind Demo Walkthrough

## Setup

Run the app with Neo4j available, then seed the reusable demo dataset:

```powershell
cd C:\Users\Msi\Downloads\ai-coach-backend\frontend-react
npm run demo:seed
```

Default demo login:

- Username: `pitchmind_demo_admin`
- Password: `PitchMindDemo123!`

## Talk Track

1. Open `http://localhost:8080/` and sign in with the demo account.
2. Start on the dashboard with `Warsaw Athletic` selected.
3. Show roster size, fixture history, scheduled fixtures, and prediction readiness.
4. Open Prediction Lab and select `Warsaw Athletic vs Krakow United`.
5. Generate or load the prediction and call out probability, uncertainty, and warnings.
6. Enter decimal odds for Home Win and evaluate market value.
7. Open AI Studio and show the weekly training microcycle for Warsaw Athletic.
8. Close with the model-evaluation panel: saved predictions, evaluated fixtures, winner accuracy, and Brier score.

## Demo Principles

- Use the named demo teams, not timestamped smoke-test teams.
- Keep the message transparent: PitchMind provides probabilities and decision support, not guaranteed outcomes.
- Treat market value as an analytical signal, not betting advice.
