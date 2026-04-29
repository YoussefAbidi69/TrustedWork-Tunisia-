import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { JobBoardService } from '../../services/job-board.service';
import { JobBoardToastService } from '../../services/job-board-toast.service';
import { JobOffer } from '../../models/job-board.models';
import { OpportunityScoreView } from '../../components/opportunity-score-card/opportunity-score-card.component';

@Component({
  selector: 'app-my-jobs',
  templateUrl: './my-jobs.component.html',
  styleUrls: ['./my-jobs.component.scss']
})
export class MyJobsComponent implements OnInit {
  loading = true;
  error: string | null = null;
  jobs: JobOffer[] = [];
  statusFilter: 'ALL' | 'PUBLISHED' | 'DRAFT' | 'CLOSED' | 'FLAGGED' = 'ALL';
  
  get displayJobs(): JobOffer[] {
    if (this.statusFilter === 'ALL') return this.jobs;
    return this.jobs.filter(j => j.status === this.statusFilter);
  }

  constructor(
    private jobBoard: JobBoardService,
    private router: Router,
    private toast: JobBoardToastService
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.loading = true;
    this.error = null;
    this.jobBoard.getMyJobs(0, 50).subscribe({
      next: (p) => {
        this.jobs = p.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load your jobs.';
      }
    });
  }

  get publishedCount(): number { return this.jobs.filter(j => j.status === 'PUBLISHED').length; }
  get draftCount(): number { return this.jobs.filter(j => j.status === 'DRAFT').length; }
  get closedCount(): number { return this.jobs.filter(j => j.status === 'CLOSED').length; }
  get flaggedCount(): number { return this.jobs.filter(j => j.status === 'FLAGGED').length; }

  opportunity(job: JobOffer): OpportunityScoreView {
    return {
      total: job.opportunityScore,
      budget: job.opportunityBudgetComponent,
      demand: job.opportunityDemandComponent,
      competition: job.opportunityCompetitionComponent
    };
  }

  publish(job: JobOffer): void {
    const prev = job.status;
    job.status = 'PUBLISHED';
    this.jobBoard.publishJob(job.id).subscribe({
      next: (j) => {
        Object.assign(job, j);
        this.toast.show('Job published.');
      },
      error: () => {
        job.status = prev;
        this.error = 'Publish failed.';
      }
    });
  }

  close(job: JobOffer): void {
    const prev = job.status;
    job.status = 'CLOSED';
    this.jobBoard.closeJob(job.id).subscribe({
      next: (j) => {
        Object.assign(job, j);
        this.toast.show('Job closed.');
      },
      error: () => {
        job.status = prev;
        this.error = 'Unable to close job.';
      }
    });
  }

  deleteJob(job: JobOffer): void {
    if (!window.confirm('Delete this job?')) return;
    this.jobBoard.deleteJob(job.id).subscribe({
      next: () => {
        this.jobs = this.jobs.filter(j => j.id !== job.id);
        this.toast.show('Job deleted.');
      },
      error: () => this.error = 'Failed to delete job'
    });
  }

  applicants(job: JobOffer): void {
    void this.router.navigate(['/app/job-board/jobs', job.id, 'applicants']);
  }
}
