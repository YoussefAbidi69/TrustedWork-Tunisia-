import { Component } from '@angular/core';

interface CertificationStat {
  label: string;
  value: string;
  delta: string;
  tone: 'positive' | 'neutral' | 'accent';
  description: string;
}

interface CertificationItem {
  id: string;
  title: string;
  issuer: string;
  category: string;
  status: 'Verified' | 'Pending Review' | 'Expiring Soon';
  issueDate: string;
  expiryDate: string;
  credentialId: string;
  trustImpact: string;
  skills: string[];
  progress: number;
  featured: boolean;
  description: string;
}

interface VerificationStep {
  label: string;
  done: boolean;
}

@Component({
  selector: 'app-certifications',
  templateUrl: './certifications.component.html',
  styleUrls: ['./certifications.component.css']
})
export class CertificationsComponent {
  activeCertificationId = 'CERT-UX-2048';

  readonly stats: CertificationStat[] = [
    {
      label: 'Verified credentials',
      value: '12',
      delta: '+3 this quarter',
      tone: 'positive',
      description: 'Publicly visible and trust-boosting certifications.'
    },
    {
      label: 'Profile trust impact',
      value: '+18%',
      delta: 'Strong signal',
      tone: 'accent',
      description: 'Certified expertise increases premium client confidence.'
    },
    {
      label: 'Pending reviews',
      value: '02',
      delta: 'Needs action',
      tone: 'neutral',
      description: 'Documents waiting for approval by the platform team.'
    },
    {
      label: 'Next expiration',
      value: '24 Apr',
      delta: 'Soon',
      tone: 'neutral',
      description: 'One certification should be renewed to stay highlighted.'
    }
  ];

  readonly spotlightMetrics = [
    { label: 'Trust signal', value: '94/100' },
    { label: 'Completion', value: '88%' },
    { label: 'Featured badges', value: '06' }
  ];

  readonly certifications: CertificationItem[] = [
    {
      id: 'CERT-UX-2048',
      title: 'Advanced UI/UX Systems Design',
      issuer: 'Google Professional Certificates',
      category: 'Design',
      status: 'Verified',
      issueDate: '12 Jan 2026',
      expiryDate: '12 Jan 2028',
      credentialId: 'GGL-UX-9087-TR',
      trustImpact: '+9 trust points',
      skills: ['Design Systems', 'User Research', 'Wireframing', 'Figma'],
      progress: 100,
      featured: true,
      description:
        'Confirms advanced product design methodology, design system thinking and user-centered delivery for premium digital products.'
    },
    {
      id: 'CERT-PM-7712',
      title: 'Agile Product & Delivery Excellence',
      issuer: 'Atlassian Academy',
      category: 'Product',
      status: 'Verified',
      issueDate: '03 Nov 2025',
      expiryDate: '03 Nov 2027',
      credentialId: 'ATL-DEL-7712',
      trustImpact: '+6 trust points',
      skills: ['Agile Delivery', 'Sprint Planning', 'Stakeholder Management'],
      progress: 100,
      featured: true,
      description:
        'Validates structured product delivery, roadmap execution and cross-functional collaboration in agile environments.'
    },
    {
      id: 'CERT-AWS-1134',
      title: 'Cloud Architecture Foundations',
      issuer: 'AWS Training',
      category: 'Cloud',
      status: 'Pending Review',
      issueDate: '18 Mar 2026',
      expiryDate: '18 Mar 2029',
      credentialId: 'AWS-FND-1134',
      trustImpact: '+5 trust points',
      skills: ['Cloud Basics', 'Security', 'Infrastructure', 'Scalability'],
      progress: 72,
      featured: false,
      description:
        'Recently uploaded credential awaiting verification before being displayed as a validated expertise signal on the profile.'
    },
    {
      id: 'CERT-SEO-4821',
      title: 'Performance Marketing & SEO Strategy',
      issuer: 'HubSpot Academy',
      category: 'Marketing',
      status: 'Expiring Soon',
      issueDate: '24 Apr 2024',
      expiryDate: '24 Apr 2026',
      credentialId: 'HBS-SEO-4821',
      trustImpact: '+4 trust points',
      skills: ['SEO', 'Content Strategy', 'Analytics', 'Growth'],
      progress: 92,
      featured: false,
      description:
        'Well-rated credential that still adds value, but should be renewed soon to preserve profile freshness and credibility.'
    }
  ];

  readonly verificationSteps: VerificationStep[] = [
    { label: 'Upload official document', done: true },
    { label: 'Credential metadata validated', done: true },
    { label: 'Platform verification review', done: false },
    { label: 'Public profile publication', done: false }
  ];

  readonly trustBenefits = [
    {
      title: 'Higher profile credibility',
      text: 'Verified certifications reinforce your expertise and reduce client hesitation.'
    },
    {
      title: 'Better shortlisting potential',
      text: 'Premium clients often filter profiles using trust and skill signals.'
    },
    {
      title: 'Stronger category authority',
      text: 'Featured credentials help you position yourself faster in competitive niches.'
    }
  ];

  get activeCertification(): CertificationItem {
    return (
      this.certifications.find(cert => cert.id === this.activeCertificationId) ??
      this.certifications[0]
    );
  }

  selectCertification(id: string): void {
    this.activeCertificationId = id;
  }

  trackByStat(index: number, item: CertificationStat): string {
    return item.label;
  }

  trackByCertification(index: number, item: CertificationItem): string {
    return item.id;
  }

  trackBySkill(index: number, skill: string): string {
    return skill;
  }

  trackByStep(index: number, step: VerificationStep): string {
    return step.label;
  }

  getStatusClass(status: CertificationItem['status']): string {
    switch (status) {
      case 'Verified':
        return 'status-badge status-badge--verified';
      case 'Pending Review':
        return 'status-badge status-badge--pending';
      case 'Expiring Soon':
        return 'status-badge status-badge--expiring';
      default:
        return 'status-badge';
    }
  }

  getStatToneClass(tone: CertificationStat['tone']): string {
    switch (tone) {
      case 'positive':
        return 'stat-delta stat-delta--positive';
      case 'accent':
        return 'stat-delta stat-delta--accent';
      default:
        return 'stat-delta stat-delta--neutral';
    }
  }
}