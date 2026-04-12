import { Component } from '@angular/core';

interface TrustMetric {
  label: string;
  value: string;
  helper: string;
  trend: 'up' | 'down' | 'stable';
}

interface TrustPillar {
  label: string;
  score: number;
  description: string;
  tone: 'excellent' | 'good' | 'watch';
}

interface TrustTimelineItem {
  month: string;
  score: number;
}

interface ReviewImpactItem {
  title: string;
  meta: string;
  impact: string;
  type: 'positive' | 'neutral' | 'warning';
}

interface TrustActionItem {
  title: string;
  description: string;
  priority: 'Haute' | 'Moyenne' | 'Optimisation';
}

@Component({
  selector: 'app-trust-score',
  templateUrl: './trust-score.component.html',
  styleUrls: ['./trust-score.component.css']
})
export class TrustScoreComponent {
  trustScore = 87;
  trustCategory = 'Excellent';
  trustTrend = '+4 pts ce trimestre';
  trustTrendLabel = 'Tendance positive';
  trustPercent = 87;

  scoreSummary =
    'Votre réputation est solide sur la plateforme. Votre historique de missions, la régularité de vos évaluations et la qualité perçue de vos collaborations renforcent fortement votre crédibilité.';

  metrics: TrustMetric[] = [
    {
      label: 'Note moyenne',
      value: '4.8 / 5',
      helper: 'Basée sur les avis confirmés',
      trend: 'up'
    },
    {
      label: 'Avis validés',
      value: '28',
      helper: 'Après modération & contrôles',
      trend: 'up'
    },
    {
      label: 'Contrats complétés',
      value: '19',
      helper: 'Missions finalisées sans litige',
      trend: 'stable'
    },
    {
      label: 'Taux de fiabilité',
      value: '96%',
      helper: 'Délais, communication, conformité',
      trend: 'up'
    }
  ];

  pillars: TrustPillar[] = [
    {
      label: 'Qualité des livrables',
      score: 92,
      description: 'Très bonnes notes sur la qualité finale et la satisfaction client.',
      tone: 'excellent'
    },
    {
      label: 'Respect des délais',
      score: 84,
      description: 'Historique globalement fiable, avec quelques marges d’amélioration.',
      tone: 'good'
    },
    {
      label: 'Communication',
      score: 89,
      description: 'Réactivité et clarté appréciées par les clients et recruteurs.',
      tone: 'excellent'
    },
    {
      label: 'Risque réputationnel',
      score: 18,
      description: 'Peu de signaux faibles, aucun incident majeur récent détecté.',
      tone: 'watch'
    }
  ];

  timeline: TrustTimelineItem[] = [
    { month: 'Jan', score: 74 },
    { month: 'Fév', score: 76 },
    { month: 'Mar', score: 79 },
    { month: 'Avr', score: 81 },
    { month: 'Mai', score: 82 },
    { month: 'Juin', score: 84 },
    { month: 'Juil', score: 85 },
    { month: 'Août', score: 83 },
    { month: 'Sep', score: 84 },
    { month: 'Oct', score: 85 },
    { month: 'Nov', score: 86 },
    { month: 'Déc', score: 87 }
  ];

  impactReviews: ReviewImpactItem[] = [
    {
      title: 'Mission UI/UX Dashboard SaaS',
      meta: 'Client entreprise • validée • il y a 10 jours',
      impact: '+2.1 pts',
      type: 'positive'
    },
    {
      title: 'Projet Landing Page Premium',
      meta: 'Client startup • 5 étoiles • il y a 3 semaines',
      impact: '+1.4 pts',
      type: 'positive'
    },
    {
      title: 'Retard partiel sur livrable visuel',
      meta: 'Mission freelance • ajustement accepté • il y a 2 mois',
      impact: '-0.6 pt',
      type: 'warning'
    },
    {
      title: 'Collaboration branding internationale',
      meta: 'Contrat terminé • retour global positif',
      impact: '+1.2 pts',
      type: 'neutral'
    }
  ];

  actions: TrustActionItem[] = [
    {
      title: 'Ajouter 2 études de cas vérifiées',
      description: 'Des projets détaillés avec résultats et visuels renforcent la confiance côté client.',
      priority: 'Haute'
    },
    {
      title: 'Maintenir un délai de réponse < 2h',
      description: 'La réactivité conversationnelle améliore directement la perception de fiabilité.',
      priority: 'Moyenne'
    },
    {
      title: 'Obtenir 3 avis supplémentaires sur missions récentes',
      description: 'Un volume d’avis frais stabilise le score et améliore sa robustesse.',
      priority: 'Optimisation'
    }
  ];

  credibilitySignals: string[] = [
    'Profil complété à 92%',
    'KYC validé',
    'Email et téléphone vérifiés',
    'Aucune réclamation critique en cours',
    'Historique d’activité régulier',
    'Portfolio mis à jour récemment'
  ];

  get timelineMax(): number {
    return Math.max(...this.timeline.map(item => item.score));
  }

  get timelineMin(): number {
    return Math.min(...this.timeline.map(item => item.score));
  }

  getBarHeight(score: number): number {
    const min = this.timelineMin;
    const max = this.timelineMax;

    if (max === min) {
      return 60;
    }

    return 34 + ((score - min) / (max - min)) * 86;
  }

  getPillarStroke(score: number): number {
    return score * 2.64;
  }

  getTrendIcon(trend: 'up' | 'down' | 'stable'): string {
    switch (trend) {
      case 'up':
        return 'fa-arrow-trend-up';
      case 'down':
        return 'fa-arrow-trend-down';
      default:
        return 'fa-minus';
    }
  }

  getPillarToneClass(tone: 'excellent' | 'good' | 'watch'): string {
    return `pillar-card--${tone}`;
  }

  getImpactClass(type: 'positive' | 'neutral' | 'warning'): string {
    return `impact-card--${type}`;
  }

  getActionPriorityClass(priority: 'Haute' | 'Moyenne' | 'Optimisation'): string {
    if (priority === 'Haute') {
      return 'priority-high';
    }

    if (priority === 'Moyenne') {
      return 'priority-medium';
    }

    return 'priority-soft';
  }
}