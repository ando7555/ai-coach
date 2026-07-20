import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ],
    }).compileComponents();

    localStorage.clear();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have the app title`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('PitchMind Intelligence Portal');
  });

  it('should render the authentication view', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h2')?.textContent).toContain('PitchMind Intelligence Portal');
    expect(compiled.querySelector('button')?.textContent).toContain('Login to Portal');
  });

  it('should render the Prediction Lab for an authenticated user', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.isLoggedIn = true;
    app.user = { username: 'analyst', role: 'COACH' };
    app.activeTab = 'prediction';

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Prediction Lab');
    expect(compiled.textContent).toContain('Generate Match Prediction');
    expect(compiled.textContent).toContain('Market Evaluation');
  });

  it('should call GraphQL when generating a match prediction', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.predictionMatchId = '42';
    const querySpy = spyOn(app, 'queryGql').and.returnValue(Promise.resolve({
      generateMatchPrediction: { id: '1', dataQualityStatus: 'INSUFFICIENT' }
    }));

    app.generateMatchPrediction();

    expect(querySpy).toHaveBeenCalled();
    expect(querySpy.calls.mostRecent().args[1]).toEqual({ matchId: '42' });
  });

  it('should reset stale prediction match selection when team matches change', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.predictionMatchId = 'old-match';
    app.predictionResponse = { id: 'old-prediction' };
    app.marketEvaluation = { id: 'old-market' };
    spyOn(app, 'queryGql').and.returnValue(Promise.resolve({
      matchesByTeam: {
        edges: [
          { node: { id: 'new-match', homeTeam: { id: '1', name: 'Home' }, awayTeam: { id: '2', name: 'Away' }, homeGoals: null, awayGoals: null, date: '2026-07-10' } }
        ],
        totalCount: 1
      }
    }));

    app.loadMatches('1');
    await fixture.whenStable();

    expect(app.predictionMatchId).toBe('new-match');
    expect(app.predictionResponse).toBeNull();
    expect(app.marketEvaluation).toBeNull();
  });

  it('should show insufficient-data prediction state', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.isLoggedIn = true;
    app.user = { username: 'analyst', role: 'COACH' };
    app.activeTab = 'prediction';
    app.predictionResponse = {
      predictionVersion: 1,
      dataQualityStatus: 'INSUFFICIENT',
      warnings: ['Home needs more completed matches']
    };

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Historical data is insufficient');
    expect(compiled.textContent).toContain('Home needs more completed matches');
  });

  it('should reject invalid decimal odds before market evaluation API call', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.predictionResponse = { id: '1', homeWinProbability: 0.52 };
    app.predictionMarket = 'HOME_WIN';
    app.predictionDecimalOdds = 1;
    const querySpy = spyOn(app, 'queryGql');

    app.evaluateMarketValue();

    expect(querySpy).not.toHaveBeenCalled();
    expect(app.errorMessage).toContain('greater than 1');
  });

  it('should render backend market-evaluation result', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.isLoggedIn = true;
    app.user = { username: 'analyst', role: 'COACH' };
    app.activeTab = 'prediction';
    app.marketEvaluation = {
      classification: 'POTENTIAL_VALUE',
      modelProbability: 0.52,
      rawImpliedProbability: 0.476,
      fairOdds: 1.923,
      expectedValue: 0.092
    };

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Market Value Result');
    expect(compiled.textContent).toContain('POTENTIAL_VALUE');
  });

  it('should reject a reversed training date range without an API call', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.aiTrainTeamId = '1';
    app.aiTrainStart = '2026-07-12';
    app.aiTrainEnd = '2026-07-06';
    const querySpy = spyOn(app, 'queryGql');

    app.generateTrainingPlan();

    expect(querySpy).not.toHaveBeenCalled();
    expect(app.errorMessage).toContain('no more than 7 days');
  });

  it('should reject an invalid season format without an API call', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.aiSeasonTeamId = '1';
    app.aiSeasonYear = 'next season';
    const querySpy = spyOn(app, 'queryGql');

    app.generateSeasonPlan();

    expect(querySpy).not.toHaveBeenCalled();
    expect(app.errorMessage).toContain('2026/27');
  });

  it('should reject a match with the same home and away team', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.matchHomeId = '1';
    app.matchAwayId = '1';
    const querySpy = spyOn(app, 'queryGql');

    app.recordMatch();

    expect(querySpy).not.toHaveBeenCalled();
    expect(app.errorMessage).toContain('must be different');
  });
});
