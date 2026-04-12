import { Component } from '@angular/core';

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
export class TrustPassportComponent {
  readonly stats: PassportStat[] = [
    { label: 'Trust Passport Score', value: '91/100', tone: 'accent' },
    { label: 'Verification level', value: 'Premium', tone: 'success' },
    { label: 'Reputation strength', value: 'High', tone: 'success' },
    { label: 'Client confidence', value: '+27%', tone: 'warning' }
  ];

  readonly pillars: PassportPillar[] = [
    {
      title: 'Identity & KYC',
      description: 'Official identity, address and profile legitimacy verification.',
      score: 92,
      status: 'Strong',
      icon: 'fa-id-card'
    },
    {
      title: 'Reputation & Reviews',
      description: 'Client feedback, consistency and historical trust indicators.',
      score: 89,
      status: 'Strong',
      icon: 'fa-star'
    },
    {
      title: 'Skills & Certifications',
      description: 'Verified expertise, certifications and market-ready positioning.',
      score: 87,
      status: 'Good',
      icon: 'fa-certificate'
    },
    {
      title: 'Business Reliability',
      description: 'Escrow readiness, responsiveness and professional delivery signals.',
      score: 84,
      status: 'Good',
      icon: 'fa-briefcase'
    }
  ];

  readonly badges: PassportBadge[] = [
    { title: 'Identity Verified', category: 'KYC', unlocked: true },
    { title: 'Top Rated Feedback', category: 'Reputation', unlocked: true },
    { title: 'Certified Expertise', category: 'Skills', unlocked: true },
    { title: 'Escrow Ready', category: 'Business', unlocked: false },
    { title: 'Premium Profile', category: 'Visibility', unlocked: true },
    { title: 'Trust Leader', category: 'Authority', unlocked: false }
  ];

  readonly timeline: TimelineEvent[] = [
    {
      title: 'Identity validation completed',
      description: 'Government document and profile identity successfully confirmed.',
      date: '12 Mar 2026',
      status: 'Completed'
    },
    {
      title: 'Professional signals upgraded',
      description: 'Skills, certifications and portfolio elements reinforced profile credibility.',
      date: '19 Mar 2026',
      status: 'Completed'
    },
    {
      title: 'Business trust review',
      description: 'Final checks for premium payment and escrow eligibility.',
      date: '28 Mar 2026',
      status: 'In Review'
    },
    {
      title: 'Trust Leader unlock',
      description: 'Next milestone based on consistency, reviews and secure business activity.',
      date: 'Next milestone',
      status: 'Upcoming'
    }
  ];

  readonly advantages = [
    'Stronger visibility for premium freelance opportunities',
    'Higher confidence during first client screening',
    'Better positioning for secure escrow-based collaborations',
    'More authority in a competitive talent marketplace'
  ];

  getStatToneClass(tone?: PassportStat['tone']): string {
    switch (tone) {
      case 'accent':
        return 'stat-value stat-value--accent';
      case 'success':
        return 'stat-value stat-value--success';
      case 'warning':
        return 'stat-value stat-value--warning';
      default:
        return 'stat-value';
    }
  }

  getPillarStatusClass(status: PassportPillar['status']): string {
    switch (status) {
      case 'Strong':
        return 'status-badge status-badge--success';
      case 'Good':
        return 'status-badge status-badge--warning';
      case 'Pending':
        return 'status-badge status-badge--neutral';
      default:
        return 'status-badge';
    }
  }

  getTimelineStatusClass(status: TimelineEvent['status']): string {
    switch (status) {
      case 'Completed':
        return 'status-badge status-badge--success';
      case 'In Review':
        return 'status-badge status-badge--warning';
      case 'Upcoming':
        return 'status-badge status-badge--neutral';
      default:
        return 'status-badge';
    }
  }
}