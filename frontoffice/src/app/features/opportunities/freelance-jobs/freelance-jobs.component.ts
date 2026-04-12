import { Component } from '@angular/core';

type BudgetType = 'Fixed price' | 'Hourly';
type ExperienceLevel = 'Junior' | 'Mid-level' | 'Senior';
type OpportunityStatus = 'Hot' | 'Featured' | 'New' | 'Recommended';

interface OpportunityStat {
  label: string;
  value: string;
  caption: string;
}

interface MarketSignal {
  label: string;
  value: string;
  trend: 'up' | 'neutral';
}

interface JobOpportunity {
  id: number;
  title: string;
  company: string;
  companyLogo: string;
  location: string;
  postedAt: string;
  budget: string;
  budgetType: BudgetType;
  duration: string;
  level: ExperienceLevel;
  proposals: number;
  matchScore: number;
  status: OpportunityStatus;
  featured: boolean;
  verified: boolean;
  category: string;
  description: string;
  skills: string[];
  highlights: string[];
}

@Component({
  selector: 'app-freelance-jobs',
  templateUrl: './freelance-jobs.component.html',
  styleUrls: ['./freelance-jobs.component.css']
})
export class FreelanceJobsComponent {
  readonly heroStats: OpportunityStat[] = [
    {
      label: 'Open opportunities',
      value: '148',
      caption: 'Curated freelance missions this week'
    },
    {
      label: 'Avg. profile match',
      value: '84%',
      caption: 'Based on your skills, trust and history'
    },
    {
      label: 'Fastest response',
      value: '< 2h',
      caption: 'Top clients replying on premium missions'
    }
  ];

  readonly marketSignals: MarketSignal[] = [
    { label: 'Angular missions', value: '+18%', trend: 'up' },
    { label: 'Remote-first briefs', value: '72%', trend: 'up' },
    { label: 'Verified clients', value: '91%', trend: 'neutral' }
  ];

  readonly categoryFilters: string[] = [
    'All',
    'Web Development',
    'Cloud & DevOps',
    'UI/UX Design',
    'Data & AI'
  ];

  readonly levelFilters: Array<ExperienceLevel | 'All'> = [
    'All',
    'Junior',
    'Mid-level',
    'Senior'
  ];

  selectedCategory = 'All';
  selectedLevel: ExperienceLevel | 'All' = 'All';
  featuredOnly = false;
  savedJobs = new Set<number>([2, 5]);

  readonly jobs: JobOpportunity[] = [
    {
      id: 1,
      title: 'Senior Angular Frontend Engineer for SaaS Marketplace',
      company: 'Nexora Studio',
      companyLogo: 'NS',
      location: 'Remote · Europe / MENA',
      postedAt: '2 hours ago',
      budget: '$2,400',
      budgetType: 'Fixed price',
      duration: '4 to 6 weeks',
      level: 'Senior',
      proposals: 12,
      matchScore: 94,
      status: 'Featured',
      featured: true,
      verified: true,
      category: 'Web Development',
      description:
        'Build a premium Angular dashboard for a multi-role freelance platform with polished UI, modular architecture and scalable component design.',
      skills: ['Angular', 'TypeScript', 'RxJS', 'SCSS', 'UI Systems'],
      highlights: [
        'High-budget client',
        'Long-term potential',
        'Strong product culture'
      ]
    },
    {
      id: 2,
      title: 'Cloud & DevOps Consultant for CI/CD Hardening',
      company: 'Atlas CloudOps',
      companyLogo: 'AC',
      location: 'Remote · Tunisia preferred',
      postedAt: '5 hours ago',
      budget: '$38',
      budgetType: 'Hourly',
      duration: '3 months',
      level: 'Senior',
      proposals: 7,
      matchScore: 91,
      status: 'Hot',
      featured: true,
      verified: true,
      category: 'Cloud & DevOps',
      description:
        'Audit and improve deployment pipelines, Docker orchestration and monitoring workflows for a growing B2B SaaS environment.',
      skills: ['Docker', 'CI/CD', 'Linux', 'Monitoring', 'Cloud'],
      highlights: [
        'Technical lead exposure',
        'Architecture ownership',
        'Fast hiring cycle'
      ]
    },
    {
      id: 3,
      title: 'UI/UX Designer for Trust-Centered Marketplace Flows',
      company: 'Craftlane',
      companyLogo: 'CL',
      location: 'Hybrid · Tunis',
      postedAt: 'Today',
      budget: '$1,600',
      budgetType: 'Fixed price',
      duration: '2 to 3 weeks',
      level: 'Mid-level',
      proposals: 19,
      matchScore: 76,
      status: 'New',
      featured: false,
      verified: true,
      category: 'UI/UX Design',
      description:
        'Design onboarding, profile reputation and contract experience for a premium trust-driven marketplace targeting North Africa.',
      skills: ['Figma', 'Design Systems', 'UX Research', 'Prototyping'],
      highlights: [
        'Clear brief',
        'Fast scope',
        'Strong visual direction'
      ]
    },
    {
      id: 4,
      title: 'Angular Developer for Messaging & Notifications Module',
      company: 'BluePeak Digital',
      companyLogo: 'BP',
      location: 'Remote',
      postedAt: '1 day ago',
      budget: '$1,150',
      budgetType: 'Fixed price',
      duration: '10 days',
      level: 'Mid-level',
      proposals: 23,
      matchScore: 88,
      status: 'Recommended',
      featured: false,
      verified: true,
      category: 'Web Development',
      description:
        'Implement real-time style chat interfaces, notification panels and polished interaction states inside an enterprise Angular workspace.',
      skills: ['Angular', 'Component Architecture', 'CSS', 'UX'],
      highlights: [
        'Excellent functional spec',
        'Portfolio-worthy',
        'Design-first team'
      ]
    },
    {
      id: 5,
      title: 'AI Data Analyst for Reputation Scoring Insights',
      company: 'Verity Labs',
      companyLogo: 'VL',
      location: 'Remote · International',
      postedAt: '1 day ago',
      budget: '$42',
      budgetType: 'Hourly',
      duration: '6 weeks',
      level: 'Senior',
      proposals: 9,
      matchScore: 81,
      status: 'Featured',
      featured: true,
      verified: false,
      category: 'Data & AI',
      description:
        'Translate platform trust and review signals into actionable dashboards, scoring logic and product-facing recommendation summaries.',
      skills: ['Python', 'Analytics', 'SQL', 'Dashboards', 'AI'],
      highlights: [
        'Research-oriented',
        'Interesting dataset',
        'Remote-friendly'
      ]
    },
    {
      id: 6,
      title: 'Junior Frontend Support for Client Portal Enhancements',
      company: 'OrbitHQ',
      companyLogo: 'OH',
      location: 'Remote · GMT+1 to GMT+3',
      postedAt: '2 days ago',
      budget: '$650',
      budgetType: 'Fixed price',
      duration: '1 week',
      level: 'Junior',
      proposals: 31,
      matchScore: 67,
      status: 'New',
      featured: false,
      verified: true,
      category: 'Web Development',
      description:
        'Refine dashboard screens, tidy interaction states and improve UI consistency across a client-facing portal with reusable components.',
      skills: ['Angular', 'HTML', 'CSS', 'Responsive UI'],
      highlights: [
        'Beginner-friendly',
        'Clear deliverables',
        'Short mission'
      ]
    }
  ];

  selectedJob: JobOpportunity = this.jobs[0];

  get filteredJobs(): JobOpportunity[] {
    return this.jobs.filter((job) => {
      const matchesCategory =
        this.selectedCategory === 'All' || job.category === this.selectedCategory;

      const matchesLevel =
        this.selectedLevel === 'All' || job.level === this.selectedLevel;

      const matchesFeatured = !this.featuredOnly || job.featured;

      return matchesCategory && matchesLevel && matchesFeatured;
    });
  }

  get featuredCount(): number {
    return this.jobs.filter((job) => job.featured).length;
  }

  get verifiedCount(): number {
    return this.jobs.filter((job) => job.verified).length;
  }

  selectCategory(category: string): void {
    this.selectedCategory = category;
    this.syncSelectedJob();
  }

  selectLevel(level: ExperienceLevel | 'All'): void {
    this.selectedLevel = level;
    this.syncSelectedJob();
  }

  toggleFeaturedOnly(): void {
    this.featuredOnly = !this.featuredOnly;
    this.syncSelectedJob();
  }

  selectJob(job: JobOpportunity): void {
    this.selectedJob = job;
  }

  toggleSave(jobId: number): void {
    if (this.savedJobs.has(jobId)) {
      this.savedJobs.delete(jobId);
      return;
    }

    this.savedJobs.add(jobId);
  }

  isSaved(jobId: number): boolean {
    return this.savedJobs.has(jobId);
  }

  getStatusClass(status: OpportunityStatus): string {
    switch (status) {
      case 'Hot':
        return 'status-hot';
      case 'Featured':
        return 'status-featured';
      case 'New':
        return 'status-new';
      case 'Recommended':
        return 'status-recommended';
      default:
        return '';
    }
  }

  trackByStat(index: number, item: OpportunityStat): string {
    return item.label;
  }

  trackBySignal(index: number, item: MarketSignal): string {
    return item.label;
  }

  trackByJobId(index: number, item: JobOpportunity): number {
    return item.id;
  }

  trackByText(index: number, item: string): string {
    return `${index}-${item}`;
  }

  private syncSelectedJob(): void {
    const availableJobs = this.filteredJobs;

    if (!availableJobs.length) {
      return;
    }

    const stillVisible = availableJobs.some((job) => job.id === this.selectedJob.id);

    if (!stillVisible) {
      this.selectedJob = availableJobs[0];
    }
  }
}