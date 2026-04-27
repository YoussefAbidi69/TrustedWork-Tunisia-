import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { JobBoardService } from '../../services/job-board.service';
import { JobBoardToastService } from '../../services/job-board-toast.service';
import { JobApplication, MessageDto } from '../../models/job-board.models';

@Component({
  selector: 'app-applicants',
  templateUrl: './applicants.component.html',
  styleUrls: ['./applicants.component.scss']
})
export class ApplicantsComponent implements OnInit, OnDestroy {
  jobId = 0;
  loading = true;
  error: string | null = null;
  job: any = null;
  applicants: JobApplication[] = [];
  expandedId: number | null = null;
  updatingId: number | null = null;
  predictions: Record<number, any> = {};
  get rows() { return this.applicants; }

  /* ── Chat drawer state ── */
  chatOpen = false;
  chatPeerId = 0;
  chatPeerLabel = '';
  chatMessages: MessageDto[] = [];
  chatInput = '';
  chatSending = false;
  chatLoading = false;
  private chatPollTimer: any = null;
  private currentUserId = 0;

  constructor(
    private route: ActivatedRoute,
    private jobBoard: JobBoardService,
    private toast: JobBoardToastService
  ) {}

  ngOnInit(): void {
    this.jobId = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  ngOnDestroy(): void {
    this.stopChatPoll();
  }

  reload(): void {
    this.loading = true;
    this.jobBoard.getJobById(this.jobId).subscribe(j => {
      this.job = j;
      this.currentUserId = j.clientId;
    });
    this.jobBoard.getJobApplications(this.jobId).subscribe({
      next: (r) => {
        this.applicants = r;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load applicants.';
      }
    });
  }

  toggleExpand(id: number): void {
    this.expandedId = this.expandedId === id ? null : id;
  }

  get total(): number { return this.applicants.length; }
  get shortlisted(): number { return this.applicants.filter((r) => r.status === 'SHORTLISTED').length; }
  get accepted(): number { return this.applicants.filter((r) => r.status === 'ACCEPTED').length; }
  get rejected(): number { return this.applicants.filter((r) => r.status === 'REJECTED').length; }
  get topMatch(): JobApplication | null {
    if (!this.applicants.length) return null;
    return [...this.applicants].sort((a,b) => ((b.matchScore?.totalScore) ?? 0) - ((a.matchScore?.totalScore) ?? 0))[0];
  }
  get topPrediction(): any {
    return this.topMatch ? this.predictions[this.topMatch.freelancerId] : null;
  }

  summary(): { total: number; short: number; acc: number } {
    return {
      total: this.rows.length,
      short: this.rows.filter((r) => r.status === 'SHORTLISTED').length,
      acc: this.rows.filter((r) => r.status === 'ACCEPTED').length
    };
  }

  updateStatus(app: JobApplication, status: 'ACCEPTED' | 'REJECTED' | 'SHORTLISTED'): void {
    if (this.updatingId) return;
    this.updatingId = app.id;
    this.jobBoard.updateApplicationStatus(app.id, status).subscribe({
      next: (updated) => {
        const i = this.applicants.findIndex((a) => a.id === app.id);
        if (i >= 0) this.applicants[i] = updated;
        this.updatingId = null;
        this.toast.show('Application updated.');
      },
      error: () => {
        this.updatingId = null;
        this.error = 'Status update failed.';
      }
    });
  }

  /* ── Chat Drawer Methods ── */

  openChat(app: JobApplication): void {
    this.chatPeerId = app.freelancerId;
    this.chatPeerLabel = `Freelancer #${app.freelancerId}`;
    this.chatOpen = true;
    this.chatInput = '';
    this.loadMessages();
    this.startChatPoll();
  }

  closeChat(): void {
    this.chatOpen = false;
    this.stopChatPoll();
  }

  sendChat(): void {
    if (!this.chatInput.trim() || this.chatSending) return;
    this.chatSending = true;
    this.jobBoard.sendMessage({
      jobOfferId: this.jobId,
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
    this.chatLoading = true;
    this.jobBoard.getConversation(this.jobId, this.chatPeerId).subscribe({
      next: (msgs) => {
        this.chatMessages = msgs;
        this.chatLoading = false;
        this.jobBoard.markMessagesRead(this.jobId, this.chatPeerId).subscribe();
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
        this.jobBoard.getConversation(this.jobId, this.chatPeerId).subscribe(msgs => {
          this.chatMessages = msgs;
          this.jobBoard.markMessagesRead(this.jobId, this.chatPeerId).subscribe();
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

  messageApplicant(app: JobApplication): void {
    this.openChat(app);
  }
}
