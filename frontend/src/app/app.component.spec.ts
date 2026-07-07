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
    expect(app.title).toEqual('AI Coach Portal');
  });

  it('should render the authentication view', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h2')?.textContent).toContain('AI Coaching Portal');
    expect(compiled.querySelector('button')?.textContent).toContain('Login to Portal');
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
