import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { JobBoardService } from '../../services/job-board.service';
import { JobBoardToastService } from '../../services/job-board-toast.service';
import { ApplicationStatus, JobApplication, MessageDto } from '../../models/job-board.models';

@Component({
  selector: 'app-my-applications',
  templateUrl: './my-applications.component.html',
  styleUrls: ['./my-applications.component.scss']
})
export class MyApplicationsComponent implements OnInit, OnDestroy {
  jbApps: JobApplication[] = [];
  jbLoading = true;
  jbError: string | null = null;
  selectedJbApp: JobApplication | null = null;
  activeFilter: ApplicationStatus | 'ALL' = 'ALL';

  readonly statusFilters: (ApplicationStatus | 'ALL')[] = [
    'ALL',
    'PENDING',
    'SHORTLISTED',
    'ACCEPTED',
    'REJECTED',
    'WITHDRAWN'
  ];

  /* ── Chat drawer state ── */
  chatOpen = false;
  chatJobId = 0;
  chatPeerId = 0;
  chatPeerLabel = '';
  chatMessages: MessageDto[] = [];
  chatInput = '';
  chatSending = false;
  chatLoading = false;
  private chatPollTimer: any = null;
  private currentUserId = 0;

  constructor(
    private jobBoard: JobBoardService,
    private auth: AuthService,
    private router: Router,
    private toast: JobBoardToastService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentAuthUser();
    if (user?.role?.toUpperCase() !== 'FREELANCER') {
      this.jbLoading = false;
      return;
    }
    this.currentUserId = user.userId;
    this.loadApplications();
  }

  ngOnDestroy(): void {
    this.stopChatPoll();
  }

  loadApplications(): void {
    this.jbLoading = true;
    this.jbError = null;
    this.jobBoard.getMyApplications().subscribe({
      next: (apps) => {
        this.jbApps = apps;
        this.jbLoading = false;
        this.syncSelection();
      },
      error: () => {
        this.jbApps = [];
        this.jbError = 'Unable to load applications.';
        this.jbLoading = false;
        this.selectedJbApp = null;
      }
    });
  }

  private syncSelection(): void {
    const list = this.filteredJbApps;
    if (!list.length) {
      this.selectedJbApp = null;
      return;
    }
    if (!this.selectedJbApp || !list.some((a) => a.id === this.selectedJbApp!.id)) {
      this.selectedJbApp = list[0];
    }
  }

  get isFreelancer(): boolean {
    return this.auth.getCurrentAuthUser()?.role?.toUpperCase() === 'FREELANCER';
  }

  get filteredJbApps(): JobApplication[] {
    if (this.activeFilter === 'ALL') {
      return this.jbApps;
    }
    return this.jbApps.filter((a) => a.status === this.activeFilter);
  }

  get activePipelineCount(): number {
    return this.jbApps.filter((a) => a.status === 'PENDING' || a.status === 'SHORTLISTED').length;
  }

  get pendingCount(): number {
    return this.jbApps.filter((a) => a.status === 'PENDING').length;
  }

  get shortlistedCount(): number {
    return this.jbApps.filter((a) => a.status === 'SHORTLISTED').length;
  }

  get acceptedCount(): number {
    return this.jbApps.filter((a) => a.status === 'ACCEPTED').length;
  }

  setFilter(f: ApplicationStatus | 'ALL'): void {
    this.activeFilter = f;
    this.syncSelection();
  }

  selectJb(app: JobApplication): void {
    this.selectedJbApp = app;
  }

  withdrawJb(app: JobApplication): void {
    if (app.status !== 'PENDING') {
      return;
    }
    if (!window.confirm('Withdraw this application?')) {
      return;
    }
    const snapshot = [...this.jbApps];
    this.jbApps = this.jbApps.map((a) =>
      a.id === app.id ? { ...a, status: 'WITHDRAWN' as ApplicationStatus } : a
    );
    this.syncSelection();
    this.jobBoard.withdrawApplication(app.id).subscribe({
      error: () => {
        this.jbApps = snapshot;
        this.syncSelection();
        window.alert('Could not withdraw application.');
      }
    });
  }

  openJbJob(jobOfferId: number): void {
    void this.router.navigate(['/app/job-board/marketplace'], { queryParams: { job: jobOfferId } });
  }

  trackByApp(_i: number, app: JobApplication): number {
    return app.id;
  }

  /* ── Chat Drawer Methods ── */

  openChat(app: JobApplication): void {
    this.chatLoading = true;
    this.chatOpen = true;
    this.chatJobId = app.jobOfferId;
    this.chatInput = '';
    
    // We need the client ID to message them. Let's fetch the job details.
    this.jobBoard.getJobById(app.jobOfferId).subscribe({
      next: (job) => {
        this.chatPeerId = job.clientId;
        this.chatPeerLabel = `Client #${job.clientId}`;
        this.loadMessages();
        this.startChatPoll();
      },
      error: () => {
        this.chatLoading = false;
        this.chatOpen = false;
        this.toast.show('Failed to initiate chat with the client.');
      }
    });
  }

  closeChat(): void {
    this.chatOpen = false;
    this.stopChatPoll();
  }

  sendChat(): void {
    if (!this.chatInput.trim() || this.chatSending) return;
    this.chatSending = true;
    this.jobBoard.sendMessage({
      jobOfferId: this.chatJobId,
      receiverId: this.chatPeerId,
      content: this.chatInput.trim()
    }).subscribe({
      next: (msg) => {
        this.chatMessages.push(msg);
        this.chatInput = '';
        this.chatSending = false;
      },
      error: () => {
        this.chatSending = false;
        this.toast.show('Failed to send message.');
      }
    });
  }

  isMe(msg: MessageDto): boolean {
    return msg.senderId === this.currentUserId;
  }

  private loadMessages(): void {
    this.jobBoard.getConversation(this.chatJobId, this.chatPeerId).subscribe({
      next: (msgs) => {
        this.chatMessages = msgs;
        this.chatLoading = false;
        this.jobBoard.markMessagesRead(this.chatJobId, this.chatPeerId).subscribe();
      },
      error: () => {
        this.chatLoading = false;
      }
    });
  }

  private startChatPoll(): void {
    this.stopChatPoll();
    this.chatPollTimer = setInterval(() => {
      if (this.chatOpen) {
        this.jobBoard.getConversation(this.chatJobId, this.chatPeerId).subscribe(msgs => {
          this.chatMessages = msgs;
          this.jobBoard.markMessagesRead(this.chatJobId, this.chatPeerId).subscribe();
        });
      }
    }, 5000);
  }

  private stopChatPoll(): void {
    if (this.chatPollTimer) {
      clearInterval(this.chatPollTimer);
      this.chatPollTimer = null;
    }
  }
}
