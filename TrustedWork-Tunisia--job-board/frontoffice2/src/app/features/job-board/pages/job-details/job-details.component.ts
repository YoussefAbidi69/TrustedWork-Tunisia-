import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { JobBoardService } from '../../services/job-board.service';
import { JobBoardToastService } from '../../services/job-board-toast.service';
import { AuthService } from '../../../../core/services/auth.service';
import { JobApplication, JobOffer, SuccessPrediction } from '../../models/job-board.models';
import { OpportunityScoreView } from '../../components/opportunity-score-card/opportunity-score-card.component';
import { animate, style, transition, trigger } from '@angular/animations';

@Component({
  selector: 'app-job-details',
  templateUrl: './job-details.component.html',
  styleUrls: ['./job-details.component.scss'],
  animations: [
    trigger('applyPanel', [
      transition(':enter', [
        style({ height: 0, opacity: 0 }),
        animate('0.35s ease-out', style({ height: '*', opacity: 1 }))
      ]),
      transition(':leave', [animate('0.25s ease-in', style({ height: 0, opacity: 0 }))])
    ])
  ]
})
export class JobDetailsComponent implements OnInit {
  job: JobOffer | null = null;
  loading = true;
  error: string | null = null;
  applyOpen = false;
  applyError: string | null = null;
  applying = false;
  submitted: JobApplication | null = null;
  livePrediction: SuccessPrediction | null = null;
  generatingCoverLetter = false;

  applyForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    public router: Router,
    private jobBoard: JobBoardService,
    private auth: AuthService,
    private toast: JobBoardToastService
  ) {
    this.applyForm = this.fb.group({
      coverLetter: ['', [Validators.required, Validators.minLength(50)]],
      proposedRate: [0, [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.error = 'Invalid job id.';
      this.loading = false;
      return;
    }
    const loadMine = this.isFreelancer()
      ? this.jobBoard.getMyApplications()
      : of<JobApplication[]>([]);
    forkJoin({
      job: this.jobBoard.getJobById(id),
      mine: loadMine
    }).subscribe({
      next: ({ job, mine }) => {
        this.job = job;
        const existing = mine.find((a) => a.jobOfferId === id);
        if (existing) {
          this.submitted = existing;
        }
        this.loading = false;
        const uid = this.auth.getCurrentAuthUser()?.userId;
        if (this.isFreelancer() && uid != null) {
          this.jobBoard
            .postSuccessPrediction({ jobOfferId: id, freelancerId: uid, freelancerSkills: [] })
            .subscribe({
              next: (p) => (this.livePrediction = p),
              error: () => {}
            });
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load this job.';
      }
    });
  }

  opportunityView(job: JobOffer): OpportunityScoreView {
    return {
      total: job.opportunityScore,
      budget: job.opportunityBudgetComponent,
      demand: job.opportunityDemandComponent,
      competition: job.opportunityCompetitionComponent
    };
  }

  isFreelancer(): boolean {
    return this.auth.getCurrentAuthUser()?.role?.toUpperCase() === 'FREELANCER';
  }

  toggleApply(): void {
    if (!this.isFreelancer()) {
      void this.router.navigate(['/auth/login']);
      return;
    }
    this.applyOpen = !this.applyOpen;
    this.applyError = null;
  }

  generateAiCoverLetter(): void {
    if (!this.job) return;

    const user = this.auth.getCurrentAuthUser();
    const freelancerName = user?.email ? user.email.split('@')[0] : 'Pro Freelancer';

    this.generatingCoverLetter = true;
    this.applyForm.get('coverLetter')?.disable();

    this.jobBoard.generateCoverLetter({
      jobTitle: this.job.title,
      jobDescription: this.job.description,
      freelancerName: freelancerName,
      skills: [], // the profile skills could be loaded here, but keeping simple for now
      bio: '',
      pastProjects: ''
    }).subscribe({
      next: (res) => {
        this.applyForm.patchValue({ coverLetter: res.coverLetter });
        this.applyForm.get('coverLetter')?.enable();
        this.generatingCoverLetter = false;
      },
      error: () => {
        this.applyForm.get('coverLetter')?.enable();
        this.generatingCoverLetter = false;
      }
    });
  }

  submitApply(): void {
    if (!this.job) {
      return;
    }
    this.applyForm.markAllAsTouched();
    if (this.applyForm.invalid) {
      this.applyError = 'Please fix validation errors before submitting.';
      return;
    }
    this.applying = true;
    this.applyError = null;
    const v = this.applyForm.getRawValue();
    this.jobBoard
      .submitApplication({
        jobOfferId: this.job.id,
        coverLetter: v.coverLetter!,
        proposedRate: v.proposedRate!,
        declaredSkills: []
      })
      .subscribe({
        next: (res) => {
          this.applying = false;
          this.submitted = res;
          this.applyOpen = false;
          this.toast.show('Application submitted successfully.');
          const uid = this.auth.getCurrentAuthUser()?.userId;
          if (uid != null && this.job) {
            this.jobBoard
              .postSuccessPrediction({
                jobOfferId: this.job.id,
                freelancerId: uid,
                freelancerSkills: []
              })
              .subscribe({
                next: (pred) => {
                  this.submitted = {
                    ...res,
                    successProbability: pred.probability,
                    predictionConfidence: pred.confidenceLabel
                  };
                },
                error: () => {}
              });
          }
        },
        error: () => {
          this.applying = false;
          this.applyError = 'Submission failed. Please verify your profile and try again.';
        }
      });
  }
}
