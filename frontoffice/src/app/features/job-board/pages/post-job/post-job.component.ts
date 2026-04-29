import { Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Subscription, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';
import { JobBoardService } from '../../services/job-board.service';
import { JobBoardToastService } from '../../services/job-board-toast.service';
import { JobOffer } from '../../models/job-board.models';

function atLeastOneSkill(): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = c.value as string[] | undefined;
    return v && v.length ? null : { skills: true };
  };
}

function budgetOrder(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const min = group.get('budgetMin')?.value as number;
    const max = group.get('budgetMax')?.value as number;
    if (min == null || max == null) {
      return null;
    }
    return max > min ? null : { budgetOrder: true };
  };
}

function futureDate(): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const raw = c.value as string;
    if (!raw) {
      return null;
    }
    const d = new Date(raw.length === 16 ? `${raw}:00` : raw);
    return d.getTime() > Date.now() ? null : { past: true };
  };
}

@Component({
  selector: 'app-post-job',
  templateUrl: './post-job.component.html',
  styleUrls: ['./post-job.component.scss']
})
export class PostJobComponent implements OnInit, OnDestroy {
  aiPhase: 'idle' | 'scanning' | 'done' = 'idle';
  extractedSkills: string[] = [];
  requiredSkills: string[] = [];
  manualSkill = '';
  extracting = false;
  private descSub?: Subscription;
  private saveSub?: Subscription;

  categories = ['Software', 'Design', 'Marketing', 'Data', 'Operations', 'Other'];

  form: FormGroup;

  saving = false;
  error: string | null = null;
  published = false;
  publishedJob: JobOffer | null = null;

  tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

  constructor(
    private fb: FormBuilder,
    private jobBoard: JobBoardService,
    private toast: JobBoardToastService
  ) {
    this.form = this.fb.group(
      {
        title: ['', [Validators.required, Validators.minLength(10)]],
        description: ['', [Validators.required, Validators.minLength(100)]],
        category: ['', Validators.required],
        budgetMin: [0, [Validators.required, Validators.min(0.01)]],
        budgetMax: [0, [Validators.required, Validators.min(0.01)]],
        durationDays: [30, Validators.required],
        location: ['', Validators.required],
        remote: [false],
        expiresAt: ['', [Validators.required, futureDate()]],
        requiredSkills: this.fb.nonNullable.control<string[]>([], { validators: [atLeastOneSkill()] })
      },
      { validators: [budgetOrder()] }
    );
  }

  ngOnInit(): void {
    this.descSub = this.form
      .get('description')!
      .valueChanges.pipe(
        debounceTime(800),
        distinctUntilChanged(),
        tap(() => {
          const d = this.form.get('description')?.value || '';
          if (d.length < 100) {
            this.aiPhase = 'idle';
            return;
          }
          this.aiPhase = 'scanning';
          this.extracting = true;
        }),
        switchMap((text) => {
          const d = text || '';
          if (d.length < 100) {
            return of(null);
          }
          return this.jobBoard.previewSkillsFromDescription(d).pipe(
            catchError(() => {
              this.aiPhase = 'idle';
              this.extracting = false;
              return of(null);
            })
          );
        })
      )
      .subscribe((res) => {
        if (!res) {
          return;
        }
        this.extractedSkills = res.skills || [];
        this.aiPhase = 'done';
        this.extracting = false;
        const merged = new Set([...(this.requiredSkills || []), ...this.extractedSkills]);
        this.requiredSkills = Array.from(merged);
      });
    this.requiredSkills = this.form.get('requiredSkills')?.value || [];
  }

  ngOnDestroy(): void {
    this.descSub?.unsubscribe();
    this.saveSub?.unsubscribe();
  }

  descLength(): number {
    return this.form.get('description')?.value?.length || 0;
  }

  addManualSkill(): void {
    const s = this.manualSkill.trim();
    if (!s) {
      return;
    }
    if (!this.requiredSkills.includes(s)) {
      this.requiredSkills = [...this.requiredSkills, s];
    }
    this.manualSkill = '';
  }

  removeSkill(s: string): void {
    this.requiredSkills = (this.requiredSkills || []).filter((x: string) => x !== s);
    this.extractedSkills = this.extractedSkills.filter((x: string) => x !== s);
  }

  isAiSkill(s: string): boolean {
    return this.extractedSkills.includes(s);
  }

  get budgetInvalid(): boolean {
    return !!this.form.hasError('budgetOrder');
  }



  private normalizeDate(raw: string): string {
    if (!raw) {
      return '';
    }
    return raw.length === 16 ? `${raw}:00` : raw;
  }

  private payload() {
    const v = this.form.getRawValue();
    return {
      title: v.title!.trim(),
      description: v.description!.trim(),
      category: v.category!.trim(),
      requiredSkills: this.requiredSkills || [],
      budgetMin: v.budgetMin!,
      budgetMax: v.budgetMax!,
      durationDays: v.durationDays!,
      location: v.location!.trim(),
      remote: !!v.remote,
      expiresAt: this.normalizeDate(v.expiresAt as string)
    };
  }

  saveDraft(): void {
    this.submit(false);
  }

  publishNow(): void {
    this.submit(true);
  }

  ignoreCardClick(_event: JobOffer): void {}

  private submit(publish: boolean): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.error = 'Please fix validation errors.';
      return;
    }
    this.saving = true;
    this.error = null;
    const created = this.jobBoard.createJob(this.payload());
    if (publish) {
      this.saveSub?.unsubscribe();
      this.saveSub = created
        .pipe(switchMap((job) => this.jobBoard.publishJob(job.id)))
        .subscribe({
          next: (job) => {
            this.saving = false;
            this.published = true;
            this.publishedJob = job;
            this.toast.show('Job published successfully.');
          },
          error: () => {
            this.saving = false;
            this.error =
              'Publish failed (high fraud risk or validation). If a draft was created, review it under My Jobs.';
          }
        });
    } else {
      this.saveSub?.unsubscribe();
      this.saveSub = created.subscribe({
        next: () => {
          this.saving = false;
          this.toast.show('Draft saved.');
        },
        error: () => {
          this.saving = false;
          this.error = 'Unable to save job.';
        }
      });
    }
  }

  resetForm(): void {
    this.published = false;
    this.publishedJob = null;
    this.extractedSkills = [];
    this.requiredSkills = [];
    this.manualSkill = '';
    this.form.reset({
      title: '',
      description: '',
      category: '',
      budgetMin: 0,
      budgetMax: 0,
      durationDays: 30,
      location: '',
      remote: false,
      expiresAt: '',
      requiredSkills: []
    });
  }

  loadTemplate(): void {
    this.resetForm();
    const d = new Date();
    d.setDate(d.getDate() + 30);
    this.form.patchValue({
      title: 'Senior Frontend Developer for SaaS Platform',
      category: 'Software',
      location: 'Tunis, Tunisia',
      remote: true,
      description: 'We are looking for a Senior Frontend Developer with expertise in Angular, React, and modern UI/UX to help build our next-generation SaaS platform. You will be responsible for creating highly performant, responsive components using glassmorphism designs and clean architecture. Requires minimum 5 years experience.',
      budgetMin: 1500,
      budgetMax: 4000,
      durationDays: 90,
      expiresAt: d.toISOString().slice(0, 10)
    });
  }
}
