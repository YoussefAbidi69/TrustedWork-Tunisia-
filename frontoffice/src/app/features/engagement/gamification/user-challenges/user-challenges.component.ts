import { Component, OnInit } from '@angular/core';
import { ChallengeService } from '../../services/challenge.service';
import { ChallengeDTO } from '../../models/engagement.models';

@Component({
  selector: 'app-user-challenges',
  template: `
    <div class="neon-bg">
      <div class="challenges-hero mb-4">
        <div class="hero-content">
          <div class="hero-badge">MISSIONS ACTIVES</div>
          <h1>Centre de Commando</h1>
          <p>Accomplissez des missions réelles pour obtenir des récompenses légendaires.</p>
        </div>
      </div>

      <div class="container pb-5">
        <div class="row" *ngIf="!loading">
          <div class="col-md-6 col-lg-4 mb-4" *ngFor="let c of challenges">
            <div class="card mission-card" [class.claimed]="c.currentParticipation?.status === 'CLAIMED'">
              
              <!-- Card Border Glow -->
              <div class="card-glow" [ngClass]="getCardTypeClass(c)"></div>

              <div class="card-body">
                <!-- XP Badge -->
                <div class="d-flex justify-content-between align-items-center mb-4">
                  <div class="xp-badge-premium">
                    <i class="fas fa-bolt mr-1"></i> +{{ c.xpReward }} XP
                  </div>
                  <div class="status-indicator">
                    <span *ngIf="!c.currentParticipation" class="badge-neon badge-neon--available">Disponible</span>
                    <span *ngIf="c.currentParticipation?.status === 'JOINED'" class="badge-neon badge-neon--active animate-flicker">En Mission</span>
                    <span *ngIf="c.currentParticipation?.status === 'SUCCESS'" class="badge-neon badge-neon--success">Prêt</span>
                    <span *ngIf="c.currentParticipation?.status === 'CLAIMED'" class="badge-neon badge-neon--claimed">Archivé</span>
                  </div>
                </div>
                
                <h3 class="mission-title">{{ c.title }}</h3>
                <p class="mission-desc">{{ c.description }}</p>

                <!-- REAL MISSION OBJECTIVES -->
                <div class="mission-objectives mt-4" *ngIf="c.currentParticipation">
                    <div class="obj-item" [class.done]="c.currentParticipation.status !== 'JOINED'">
                        <div class="obj-dot"></div>
                        <span *ngIf="c.challengeTypeCode === 'REG_EVENT'">S'inscrire à 1 événement</span>
                        <span *ngIf="c.challengeTypeCode === 'FIRST_BADGE'">Gagner son 1er badge</span>
                        <span *ngIf="!c.challengeTypeCode || c.challengeTypeCode === 'MANUAL'">Action manuelle requise</span>
                    </div>
                </div>

                <!-- Footer Actions -->
                <div class="mt-4 pt-2">
                  <!-- STATE: NOT JOINED -->
                  <button *ngIf="!c.currentParticipation" 
                    class="btn-neon btn-neon--primary w-100" 
                    (click)="join(c.id!)" 
                    [disabled]="processing === c.id">
                    ACCEPTER LA MISSION
                  </button>

                  <!-- STATE: JOINED (Verification Needed) -->
                  <div *ngIf="c.currentParticipation?.status === 'JOINED'">
                    <button 
                      class="btn-neon btn-neon--warning w-100 mb-2" 
                      (click)="succeed(c.id!)" 
                      [disabled]="processing === c.id">
                      <span *ngIf="processing !== c.id">VÉRIFIER MES ACTIONS</span>
                      <span *ngIf="processing === c.id"><i class="fas fa-satellite fa-spin"></i> ANALYSE EN COURS...</span>
                    </button>
                    <div class="hint-text">
                        <i class="fas fa-info-circle mr-1"></i>
                        Vérifie si vous avez rempli les conditions réelles.
                    </div>
                  </div>

                  <!-- STATE: SUCCESS (Claimable) -->
                  <button *ngIf="c.currentParticipation?.status === 'SUCCESS'" 
                    class="btn-neon btn-neon--success w-100 shine-anim" 
                    (click)="claim(c.id!)" 
                    [disabled]="processing === c.id">
                    RÉCUPÉRER LA RÉCOMPENSE
                  </button>

                  <!-- STATE: CLAIMED -->
                  <div *ngIf="c.currentParticipation?.status === 'CLAIMED'" class="mission-completed">
                    <i class="fas fa-certificate mr-2"></i> RÉCOMPENSE OBTENUE
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div class="col-12 text-center py-5" *ngIf="challenges.length === 0">
            <div class="neon-empty">
              <i class="fas fa-radar mb-4"></i>
              <h3>Aucun signal détecté...</h3>
              <p>Relancez le radar plus tard pour de nouvelles missions.</p>
            </div>
          </div>
        </div>

        <!-- Spinner -->
        <div class="text-center py-5" *ngIf="loading">
          <div class="scanner-bar"></div>
          <p class="mt-4 neon-text">SCANNER DE RÉSEAU EN COURS...</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Inter:wght@300;400;600;800&display=swap');

    .neon-bg {
      min-height: 100vh;
      background: #0f172a;
      color: #e2e8f0;
      font-family: 'Inter', sans-serif;
    }

    /* Hero Section */
    .challenges-hero {
      background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
      padding: 6rem 1rem 4rem;
      border-bottom: 1px solid rgba(99, 102, 241, 0.2);
      text-align: center;
      position: relative;
    }
    .hero-badge {
      display: inline-block;
      padding: 0.4rem 1.2rem;
      background: rgba(99, 102, 241, 0.1);
      border: 1px solid #6366f1;
      color: #818cf8;
      border-radius: 50px;
      font-family: 'Orbitron', sans-serif;
      font-size: 0.7rem;
      letter-spacing: 2px;
      margin-bottom: 1.5rem;
    }
    .hero-content h1 {
      font-family: 'Orbitron', sans-serif;
      font-weight: 900;
      font-size: 3.5rem;
      margin-bottom: 1rem;
      background: linear-gradient(to right, #fff, #94a3b8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      letter-spacing: -2px;
    }
    .hero-content p { font-size: 1.25rem; color: #94a3b8; max-width: 600px; margin: 0 auto; opacity: 0.8; }

    /* Mission Cards */
    .mission-card {
      background: rgba(30, 41, 59, 0.7);
      backdrop-filter: blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 20px;
      transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      overflow: hidden;
      position: relative;
      z-index: 1;
    }
    .mission-card:hover { transform: translateY(-10px) scale(1.02); border-color: rgba(99, 102, 241, 0.4); }
    
    .card-glow {
        position: absolute;
        top: 0; left: 0; right: 0; height: 4px;
        background: linear-gradient(to right, #6366f1, #a855f7);
        filter: blur(2px);
    }
    .card-glow.reg-event { background: linear-gradient(to right, #22d3ee, #0ea5e9); }
    .card-glow.badge-ear { background: linear-gradient(to right, #f59e0b, #ec4899); }

    .xp-badge-premium {
      background: rgba(16, 185, 129, 0.1);
      color: #10b981;
      border: 1px solid rgba(16, 185, 129, 0.2);
      padding: 0.4rem 0.8rem;
      border-radius: 10px;
      font-weight: 800;
      font-size: 0.8rem;
      font-family: 'Orbitron', sans-serif;
    }

    .badge-neon {
        padding: 0.4rem 0.8rem;
        border-radius: 8px;
        font-size: 0.65rem;
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 1px;
    }
    .badge-neon--available { background: rgba(148, 163, 184, 0.1); color: #94a3b8; }
    .badge-neon--active    { background: rgba(59, 130, 246, 0.1); color: #3b82f6; box-shadow: 0 0 15px rgba(59, 130, 246, 0.3); }
    .badge-neon--success   { background: rgba(16, 185, 129, 0.1); color: #10b981; box-shadow: 0 0 15px rgba(16, 185, 129, 0.3); }
    .badge-neon--claimed   { background: rgba(0, 0, 0, 0.2); color: #475569; }

    .mission-title { font-family: 'Orbitron', sans-serif; font-size: 1.25rem; font-weight: 700; color: #fff; margin-bottom: 0.75rem; }
    .mission-desc { font-size: 0.9rem; color: #94a3b8; line-height: 1.6; min-height: 48px; }

    /* Objectives */
    .mission-objectives { background: rgba(15, 23, 42, 0.4); border-radius: 12px; padding: 1.25rem; border: 1px solid rgba(255,255,255,0.03); }
    .obj-item { display: flex; align-items: center; gap: 0.75rem; font-size: 0.8rem; color: #94a3b8; margin-bottom: 0.5rem; }
    .obj-item:last-child { margin-bottom: 0; }
    .obj-dot { width: 8px; height: 8px; border-radius: 50%; background: #475569; position: relative; }
    .obj-item.done { color: #10b981; }
    .obj-item.done .obj-dot { background: #10b981; box-shadow: 0 0 8px #10b981; }

    /* Buttons */
    .btn-neon {
      border: none;
      border-radius: 12px;
      padding: 0.9rem;
      font-weight: 900;
      font-family: 'Orbitron', sans-serif;
      font-size: 0.8rem;
      letter-spacing: 1px;
      transition: all 0.3s;
      cursor: pointer;
    }
    .btn-neon--primary { background: #6366f1; color: #fff; box-shadow: 0 5px 15px rgba(99, 102, 241, 0.3); }
    .btn-neon--primary:hover { transform: scale(1.02); box-shadow: 0 0 25px rgba(99, 102, 241, 0.5); }
    
    .btn-neon--warning { background: #f59e0b; color: #1e1b4b; }
    .btn-neon--success { background: #10b981; color: #fff; box-shadow: 0 5px 15px rgba(16, 185, 129, 0.3); }
    
    .hint-text { font-size: 0.7rem; color: #64748b; margin-top: 0.5rem; text-align: center; font-style: italic; }
    .mission-completed { text-align: center; color: #475569; font-family: 'Orbitron', sans-serif; font-weight: 800; font-size: 0.8rem; }

    /* Animations */
    .animate-flicker { animation: flicker 2s infinite; }
    @keyframes flicker {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.6; }
    }

    .scanner-bar {
        width: 100%; height: 2px; background: #6366f1;
        box-shadow: 0 0 15px #6366f1;
        animation: scan 2s linear infinite;
    }
    @keyframes scan {
        0% { transform: translateY(-50px); opacity: 0; }
        50% { opacity: 1; }
        100% { transform: translateY(50px); opacity: 0; }
    }

    .neon-text { font-family: 'Orbitron', sans-serif; color: #6366f1; text-shadow: 0 0 10px rgba(99, 102, 241, 0.5); font-size: 0.8rem; }
  `]
})
export class UserChallengesComponent implements OnInit {
  challenges: ChallengeDTO[] = [];
  loading = true;
  processing: number | null = null;

  constructor(private challengeService: ChallengeService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.challengeService.getActiveChallenges().subscribe({
      next: (data: ChallengeDTO[]) => {
        this.challenges = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  getCardTypeClass(c: ChallengeDTO): string {
      if (c.challengeTypeCode === 'REG_EVENT') return 'reg-event';
      if (c.challengeTypeCode === 'FIRST_BADGE') return 'badge-ear';
      return '';
  }

  join(id: number): void {
    this.processing = id;
    this.challengeService.joinChallenge(id).subscribe({
      next: () => {
        this.processing = null;
        this.load();
      },
      error: (err: any) => {
        alert(err.error?.message || 'Erreur lors de l\'activation');
        this.processing = null;
      }
    });
  }

  succeed(id: number): void {
    this.processing = id;
    this.challengeService.succeedChallenge(id).subscribe({
      next: () => {
        this.processing = null;
        this.load();
      },
      error: (err: any) => {
        alert(err.error?.message || "Action non vérifiée ! Assurez-vous d'avoir rempli la condition.");
        this.processing = null;
      }
    });
  }

  claim(id: number): void {
    this.processing = id;
    this.challengeService.claimReward(id).subscribe({
      next: (res: any) => {
        alert(res.message);
        this.processing = null;
        this.load();
      },
      error: (err: any) => {
        alert(err.error?.message || 'Erreur lors du claim');
        this.processing = null;
      
      }
    });
  }
}
