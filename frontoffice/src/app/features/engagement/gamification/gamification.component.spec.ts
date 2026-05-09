import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GamificationComponent } from './gamification.component';
import { GamificationService } from '../services/gamification.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';

describe('GamificationComponent (AI Recommendations)', () => {
  let component: GamificationComponent;
  let fixture: ComponentFixture<GamificationComponent>;
  let mockGamService: any;
  let mockRouter: any;

  beforeEach(async () => {
    mockGamService = {
      profile$: of({ xpPoints: 1200, level: 3 }),
      badges$: of([]),
      refreshProfile: jasmine.createSpy('refreshProfile'),
      refreshBadges: jasmine.createSpy('refreshBadges'),
      getEngagementScore: jasmine.createSpy('getEngagementScore').and.returnValue(of({ engagementScore: 0.85 })),
      getAnalytics: jasmine.createSpy('getAnalytics').and.returnValue(of({
        influenceScore: 40,
        churnRisk: 10,
        recommendations: {
          events: [{ id: 1, title: 'Hackathon AI', reason: 'Recommended' }],
          challenges: [{ id: 1, title: 'Code Vert', reason: 'Recommended' }]
        }
      }))
    };

    mockRouter = {
      navigate: jasmine.createSpy('navigate')
    };

    await TestBed.configureTestingModule({
      declarations: [GamificationComponent],
      providers: [
        { provide: GamificationService, useValue: mockGamService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GamificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load AI recommendations from analytics', () => {
    expect(mockGamService.getAnalytics).toHaveBeenCalled();
    expect(component.analytics).toBeDefined();
    expect(component.analytics.recommendations.events.length).toBe(1);
    expect(component.analytics.recommendations.events[0].title).toBe('Hackathon AI');
  });

  it('should navigate to recommended event with highlight parameter', () => {
    const recommendedEvent = { id: 1, title: 'Hackathon AI', reason: 'Recommended' };
    component.goToRecommendation(recommendedEvent, 'event');
    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/app/engagement/events'], 
      { queryParams: { highlight: 1 } }
    );
  });

  it('should navigate to recommended challenge with highlight parameter', () => {
    const recommendedChallenge = { id: 2, title: 'Challenge XYZ', reason: 'Recommended' };
    component.goToRecommendation(recommendedChallenge, 'challenge');
    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/app/engagement/missions'], 
      { queryParams: { highlight: 2 } }
    );
  });
});
