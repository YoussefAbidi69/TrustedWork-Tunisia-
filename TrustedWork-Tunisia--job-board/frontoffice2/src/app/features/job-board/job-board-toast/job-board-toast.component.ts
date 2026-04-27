import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { JobBoardToastService } from '../services/job-board-toast.service';

@Component({
  selector: 'app-job-board-toast',
  templateUrl: './job-board-toast.component.html',
  styleUrls: ['./job-board-toast.component.scss']
})
export class JobBoardToastComponent implements OnInit, OnDestroy {
  message: string | null = null;
  private sub?: Subscription;
  private hideTimer = 0;

  constructor(private toasts: JobBoardToastService) {}

  ngOnInit(): void {
    this.sub = this.toasts.messages$.subscribe((m: string) => {
       this.message = m;
      window.clearTimeout(this.hideTimer);
      this.hideTimer = window.setTimeout(() => (this.message = null), 4000);
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    window.clearTimeout(this.hideTimer);
  }
}
