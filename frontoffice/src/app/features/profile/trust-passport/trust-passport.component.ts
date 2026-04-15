import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';

interface PassportStat {
  label: string;
  value: string;
  tone?: 'default' | 'accent' | 'success' | 'warning';
}

interface PassportPillar {
  title: string;
  description: string;
  score: number;
  status: 'Strong' | 'Good' | 'Pending';
  icon: string;
}

interface PassportBadge {
  title: string;
  category: string;
  unlocked: boolean;
}

interface TimelineEvent {
  title: string;
  description: string;
  date: string;
  status: 'Completed' | 'In Review' | 'Upcoming';
}

@Component({
  selector: 'app-trust-passport',
  templateUrl: './trust-passport.component.html',
  styleUrls: ['./trust-passport.component.css']
})
export class TrustPassportComponent implements OnInit {

  loading = true;
  trustLevel = 1;
  kycStatus = 'PENDING';
  twoFactorEnabled = false;
  livenessPassed = false;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getMyProfile().pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: (data: UserProfileResponse) => {
        this.trustLevel       = (data as any).trustLevel ?? 1;
        this.kycStatus        = data.kycStatus || 'PENDING';
        this.twoFactorEnabled = data.twoFactorEnabled || false;
        this.livenessPassed   = (data as any).livenessPassed || false;
      },
      error: err => console.error(err)
    });
  }

  // ── Propriétés calculées depuis les données réelles ──

  get stats(): PassportStat[] {
    return [
      { label: 'Trust Level',         value: `${this.trustLevel}/5`,         tone: 'accent' },
      { label: 'Niveau vérification', value: this.verificationLabel,          tone: 'success' },
      { label: 'KYC Status',          value: this.kycStatus,                  tone: this.kycStatus === 'APPROVED' ? 'success' : 'warning' },
      { label: '2FA',                 value: this.twoFactorEnabled ? 'Actif' : 'Inactif', tone: this.twoFactorEnabled ? 'success' : 'warning' }
    ];
  }

  get verificationLabel(): string {
    const labels: Record<number, string> = { 1: 'Non vérifié', 2: 'Basique', 3: 'Vérifié', 4: 'Avancé', 5: 'Premium' };
    return labels[this.trustLevel] || 'Niveau ' + this.trustLevel;
  }

  get pillars(): PassportPillar[] {
    return [
      {
        title: 'Identité & KYC',
        description: 'Vérification CIN, liveness detection et validation admin.',
        score: this.kycStatus === 'APPROVED' ? (this.livenessPassed ? 100 : 70) : 20,
        status: this.kycStatus === 'APPROVED' ? 'Strong' : 'Pending',
        icon: 'fa-id-card'
      },
      {
        title: 'Sécurité du compte',
        description: 'Double authentification et protection des accès.',
        score: this.twoFactorEnabled ? 100 : 30,
        status: this.twoFactorEnabled ? 'Strong' : 'Good',
        icon: 'fa-lock'
      },
      {
        title: 'Liveness Detection',
        description: 'Correspondance selfie / photo CIN vérifiée par IA.',
        score: this.livenessPassed ? 100 : 0,
        status: this.livenessPassed ? 'Strong' : 'Pending',
        icon: 'fa-face-smile'
      },
      {
        title: 'Fiabilité du profil',
        description: 'Ancienneté du compte et activité sur la plateforme.',
        score: this.trustLevel >= 3 ? 80 : 40,
        status: this.trustLevel >= 3 ? 'Good' : 'Pending',
        icon: 'fa-briefcase'
      }
    ];
  }

  get badges(): PassportBadge[] {
    return [
      { title: 'Identité Vérifiée', category: 'KYC',        unlocked: this.kycStatus === 'APPROVED' },
      { title: '2FA Activé',        category: 'Sécurité',   unlocked: this.twoFactorEnabled },
      { title: 'Liveness Validé',   category: 'KYC',        unlocked: this.livenessPassed },
      { title: 'Trust Niveau 3+',   category: 'Trust',      unlocked: this.trustLevel >= 3 },
      { title: 'Profil Premium',    category: 'Visibilité', unlocked: this.trustLevel >= 4 },
      { title: 'Trust Leader',      category: 'Autorité',   unlocked: this.trustLevel === 5 }
    ];
  }

  get timeline(): TimelineEvent[] {
    return [
      {
        title: 'Compte créé',
        description: 'Inscription et validation de l\'email.',
        date: 'Étape 1',
        status: 'Completed'
      },
      {
        title: 'KYC soumis',
        description: 'Documents CIN et selfie envoyés pour validation.',
        date: 'Étape 2',
        status: this.kycStatus === 'PENDING' ? 'Upcoming' : this.kycStatus === 'IN_REVIEW' ? 'In Review' : 'Completed'
      },
      {
        title: 'KYC approuvé',
        description: 'Identité validée par l\'équipe TrustedWork.',
        date: 'Étape 3',
        status: this.kycStatus === 'APPROVED' ? 'Completed' : 'Upcoming'
      },
      {
        title: '2FA activé',
        description: 'Double authentification configurée pour sécuriser les accès.',
        date: 'Étape 4',
        status: this.twoFactorEnabled ? 'Completed' : 'Upcoming'
      }
    ];
  }

  get advantages(): string[] {
    return [
      'Meilleure visibilité pour les missions premium',
      'Confiance accrue lors du premier contact client',
      'Éligibilité aux contrats escrow sécurisés',
      'Autorité renforcée dans le marché freelance tunisien'
    ];
  }

  getStatToneClass(tone?: PassportStat['tone']): string {
    const map: Record<string, string> = {
      accent: 'stat-value stat-value--accent',
      success: 'stat-value stat-value--success',
      warning: 'stat-value stat-value--warning'
    };
    return map[tone || ''] || 'stat-value';
  }

  getPillarStatusClass(status: PassportPillar['status']): string {
    const map = { Strong: 'status-badge status-badge--success', Good: 'status-badge status-badge--warning', Pending: 'status-badge status-badge--neutral' };
    return map[status];
  }

  getTimelineStatusClass(status: TimelineEvent['status']): string {
    const map = { Completed: 'status-badge status-badge--success', 'In Review': 'status-badge status-badge--warning', Upcoming: 'status-badge status-badge--neutral' };
    return map[status];
  }
}