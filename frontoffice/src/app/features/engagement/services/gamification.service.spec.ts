/// <reference types="jasmine" />
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GamificationService } from './gamification.service';

describe('GamificationService (AI API)', () => {
  let service: GamificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GamificationService]
    });
    service = TestBed.inject(GamificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // S'assure qu'il n'y a pas de requêtes HTTP en attente
  });

  it('should fetch AI analytics and recommendations', () => {
    const dummyAnalytics = {
      influenceScore: 40,
      churnRisk: 10,
      recommendations: {
        events: [{ id: 1, title: 'Hackathon AI', reason: 'IA Rec' }],
        challenges: [{ id: 2, title: 'Code Vert', reason: 'IA Rec' }]
      }
    };

    service.getAnalytics().subscribe(data => {
      expect(data).toBeDefined();
      expect(data.influenceScore).toBe(40);
      expect(data.recommendations.events.length).toBe(1);
      expect(data.recommendations.events[0].title).toBe('Hackathon AI');
    });

    const req = httpMock.expectOne('http://localhost:8086/api/analytics/me');
    expect(req.request.method).toBe('GET');
    req.flush(dummyAnalytics); // Simule la réponse du Backend Java
  });
});
