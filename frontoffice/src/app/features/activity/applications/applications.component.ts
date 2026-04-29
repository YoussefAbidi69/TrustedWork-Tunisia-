import { Component } from '@angular/core';

interface ApplicationStat {
  label: string;
  value: string;
  tone?: 'default' | 'accent' | 'success' | 'warning';
}

interface ApplicationTimelineStep {
  label: string;
  done: boolean;
}

interface ApplicationItem {
  id: string;
  company: string;
  jobTitle: string;
  location: string;
  contractType: string;
  budget: string;
  appliedAt: string;
  status: 'Accepted' | 'Pending' | 'Rejected' | 'Interview';
  matchScore: number;
  tags: string[];
  summary: string;
  timeline: ApplicationTimelineStep[];
}

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.css']
})
export class ApplicationsComponent {
  selectedApplicationId = 'app-1';

  readonly stats: ApplicationStat[] = [
    { label: 'Total applications', value: '24', tone: 'default' },
    { label: 'Active pipeline', value: '08', tone: 'accent' },
    { label: 'Interview stage', value: '03', tone: 'warning' },
    { label: 'Acceptance rate', value: '37%', tone: 'success' }
  ];

  readonly filters: string[] = [
    'All',
    'Pending',
    'Interview',
    'Accepted',
    'Rejected'
  ];

  readonly applications: ApplicationItem[] = [
    {
      id: 'app-1',
      company: 'Nova Studio',
      jobTitle: 'Senior UI/UX Designer',
      location: 'Remote • Tunis / Paris',
      contractType: 'Freelance mission',
      budget: '2,500 - 3,200 TND',
      appliedAt: '2 hours ago',
      status: 'Interview',
      matchScore: 94,
      tags: ['Figma', 'Design System', 'UX Audit'],
      summary:
        'A premium design mission focused on redesigning a SaaS dashboard with strong UX, clear product logic and scalable design system foundations.',
      timeline: [
        { label: 'Application submitted', done: true },
        { label: 'Profile reviewed', done: true },
        { label: 'Interview scheduled', done: true },
        { label: 'Final decision', done: false }
      ]
    },
    {
      id: 'app-2',
      company: 'Pixel Forge',
      jobTitle: 'Brand Designer',
      location: 'Hybrid • Tunis',
      contractType: 'Part-time contract',
      budget: '1,800 - 2,200 TND',
      appliedAt: 'Yesterday',
      status: 'Pending',
      matchScore: 88,
      tags: ['Branding', 'Identity', 'Packaging'],
      summary:
        'Brand identity and marketing asset creation for a growing product-oriented company looking for strong visual direction.',
      timeline: [
        { label: 'Application submitted', done: true },
        { label: 'Profile reviewed', done: false },
        { label: 'Interview scheduled', done: false },
        { label: 'Final decision', done: false }
      ]
    },
    {
      id: 'app-3',
      company: 'Trusted Labs',
      jobTitle: 'Product Designer',
      location: 'Remote',
      contractType: 'Long-term freelance',
      budget: '3,000 - 4,000 TND',
      appliedAt: '3 days ago',
      status: 'Accepted',
      matchScore: 97,
      tags: ['Product Design', 'Prototyping', 'Research'],
      summary:
        'A high-value product design role centered on roadmap execution, user flows and premium interface refinement for an international SaaS product.',
      timeline: [
        { label: 'Application submitted', done: true },
        { label: 'Profile reviewed', done: true },
        { label: 'Interview scheduled', done: true },
        { label: 'Final decision', done: true }
      ]
    },
    {
      id: 'app-4',
      company: 'North Star Agency',
      jobTitle: 'Motion & Visual Designer',
      location: 'Remote • MENA',
      contractType: 'Project-based',
      budget: '1,500 - 2,000 TND',
      appliedAt: '1 week ago',
      status: 'Rejected',
      matchScore: 71,
      tags: ['Motion', 'Visual Identity', 'Social Media'],
      summary:
        'A visual communication mission requiring fast-paced content production and strong creative direction across campaign assets.',
      timeline: [
        { label: 'Application submitted', done: true },
        { label: 'Profile reviewed', done: true },
        { label: 'Interview scheduled', done: false },
        { label: 'Final decision', done: true }
      ]
    }
  ];

  get selectedApplication(): ApplicationItem {
    return (
      this.applications.find(item => item.id === this.selectedApplicationId) ??
      this.applications[0]
    );
  }

  selectApplication(id: string): void {
    this.selectedApplicationId = id;
  }

  getStatToneClass(tone?: ApplicationStat['tone']): string {
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

  getStatusClass(status: ApplicationItem['status']): string {
    switch (status) {
      case 'Accepted':
        return 'status-badge status-badge--accepted';
      case 'Interview':
        return 'status-badge status-badge--interview';
      case 'Pending':
        return 'status-badge status-badge--pending';
      case 'Rejected':
        return 'status-badge status-badge--rejected';
      default:
        return 'status-badge';
    }
  }
}