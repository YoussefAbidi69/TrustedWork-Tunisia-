import { Component } from '@angular/core';

type RecruitmentStatus = 'Open' | 'Screening' | 'Priority';
type WorkMode = 'Remote' | 'Hybrid' | 'On-site';

interface RecruitmentJob {
  id: number;
  role: string;
  company: string;
  status: RecruitmentStatus;
  workMode: WorkMode;
  salary: string;
  location: string;
  seniority: string;
  summary: string;
  requirements: string[];
}

@Component({
  selector: 'app-recruitment-jobs',
  templateUrl: './recruitment-jobs.component.html',
  styleUrls: ['./recruitment-jobs.component.css']
})
export class RecruitmentJobsComponent {
  readonly jobs: RecruitmentJob[] = [
    {
      id: 1,
      role: 'Senior Angular Engineer',
      company: 'Nexora Studio',
      status: 'Priority',
      workMode: 'Remote',
      salary: '4.5k - 6k TND',
      location: 'Tunisia / Remote',
      seniority: 'Senior',
      summary: 'Join a product team building a premium multi-role SaaS experience for trusted freelancing and client operations.',
      requirements: ['Angular', 'TypeScript', 'Scalable UI', 'Clean architecture']
    },
    {
      id: 2,
      role: 'Spring Boot Backend Engineer',
      company: 'Atlas CloudOps',
      status: 'Screening',
      workMode: 'Hybrid',
      salary: '4k - 5.5k TND',
      location: 'Tunis',
      seniority: 'Mid / Senior',
      summary: 'Work on secure APIs, integrations and backend workflows for a trust-driven marketplace platform.',
      requirements: ['Spring Boot', 'SQL', 'Microservices', 'Security']
    },
    {
      id: 3,
      role: 'Product Designer',
      company: 'Craftlane',
      status: 'Open',
      workMode: 'On-site',
      salary: '3.2k - 4.4k TND',
      location: 'Sousse',
      seniority: 'Mid-level',
      summary: 'Shape recruiter-grade product experiences, profile reputation flows and premium interaction systems.',
      requirements: ['Figma', 'Design systems', 'UX', 'Visual polish']
    }
  ];

  getStatusClass(status: RecruitmentStatus): string {
    switch (status) {
      case 'Priority':
        return 'status-priority';
      case 'Screening':
        return 'status-screening';
      case 'Open':
        return 'status-open';
      default:
        return '';
    }
  }

  trackByJob(index: number, item: RecruitmentJob): number {
    return item.id;
  }

  trackByText(index: number, item: string): string {
    return item;
  }
}