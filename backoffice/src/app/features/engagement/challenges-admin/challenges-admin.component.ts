import { Component, OnInit } from '@angular/core';
import { ChallengeAdminService } from '../services/challenge-admin.service';
import { ChallengeDTO, ChallengeStatus } from '../models/engagement.models';

@Component({
  selector: 'app-challenges-admin',
  template: `
    <div class="page-header animate-fade-in">
      <div class="header-left">
        <span class="badge-label">Engagement System</span>
        <h1 class="page-title">Community Challenges</h1>
        <p class="page-subtitle">Create and manage engagement tasks for the community</p>
      </div>
      <div class="header-actions">
        <button class="btn-add" (click)="openModal()">
          <i class="fas fa-plus"></i> New Challenge
        </button>
      </div>
    </div>

    <div class="glow-line"></div>

    <!-- STATS ROW -->
    <div class="stats-row">
      <div class="stat-pill green">
        <i class="fas fa-play-circle"></i>
        <span>{{ countByStatus('ACTIVE') }} Active</span>
      </div>
      <div class="stat-pill orange">
        <i class="fas fa-flag-checkered"></i>
        <span>{{ countByStatus('COMPLETED') }} Completed</span>
      </div>
      <div class="stat-pill red">
        <i class="fas fa-clock"></i>
        <span>{{ countByStatus('EXPIRED') }} Expired</span>
      </div>
      <div class="stat-pill accent">
        <i class="fas fa-bolt"></i>
        <span>{{ getTotalXP() | number }} Total XP</span>
      </div>
    </div>

    <div class="table-card animate-slide-up">
      <div class="card-head">
        <span class="card-head-title">Active Challenges</span>
        <span class="card-head-count">{{ challenges.length }} total</span>
      </div>
      <div class="table-responsive">
        <table class="challenges-table">
          <thead>
            <tr>
              <th>Challenge</th>
              <th>Automation</th>
              <th>Reward</th>
              <th>Deadline</th>
              <th>Urgency</th>
              <th>Status</th>
              <th class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of challenges" [class.urgent-row]="getDaysLeft(c.deadline) <= 3 && c.status === 'ACTIVE'">
              <td>
                <div class="challenge-name">{{ c.title }}</div>
                <div class="challenge-desc">{{ c.description | slice:0:65 }}...</div>
              </td>
              <td>
                <span class="hook-pill">{{ c.challengeTypeCode || 'MANUAL' }}</span>
              </td>
              <td>
                <div class="xp-chip">
                  <i class="fas fa-bolt"></i>
                  <span>{{ c.xpReward }} XP</span>
                </div>
              </td>
              <td>
                <span class="date-text">{{ c.deadline | date:'d MMM yyyy' }}</span>
              </td>
              <td>
                <div class="urgency-col" *ngIf="c.status === 'ACTIVE'; else noUrgency">
                  <div class="deadline-bar-track">
                    <div class="deadline-bar-fill"
                         [class.fill-danger]="getDaysLeft(c.deadline) <= 3"
                         [class.fill-warning]="getDaysLeft(c.deadline) > 3 && getDaysLeft(c.deadline) <= 7"
                         [style.width.%]="getDeadlinePercent(c.deadline)">
                    </div>
                  </div>
                  <span class="days-left"
                        [class.text-danger]="getDaysLeft(c.deadline) <= 3"
                        [class.text-warning]="getDaysLeft(c.deadline) > 3 && getDaysLeft(c.deadline) <= 7">
                    {{ getDaysLeft(c.deadline) }}d left
                  </span>
                </div>
                <ng-template #noUrgency>
                  <span class="no-urgency">—</span>
                </ng-template>
              </td>
              <td>
                <span class="status-badge"
                      [class.active]="c.status === 'ACTIVE'"
                      [class.expired]="c.status === 'EXPIRED'"
                      [class.completed]="c.status === 'COMPLETED'">
                  {{ c.status }}
                </span>
              </td>
              <td class="text-end">
                <div class="action-group">
                  <button class="btn-icon-modern edit" (click)="editChallenge(c)" title="Edit">
                    <i class="fas fa-pen-nib"></i>
                  </button>
                  <button class="btn-icon-modern del" (click)="deleteChallenge(c.id!)" title="Delete">
                    <i class="fas fa-trash-alt"></i>
                  </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="challenges.length === 0">
              <td colspan="7" class="empty-row">
                <i class="fas fa-trophy" style="opacity:0.3; margin-right:8px;"></i>
                No challenges created yet.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL -->
    <div class="modal-overlay" *ngIf="showModal" (click)="showModal = false">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div>
            <span class="modal-badge">Challenge Configuration</span>
            <h2>{{ isEditing ? 'Edit Challenge' : 'Create New Challenge' }}</h2>
          </div>
          <button class="modal-close" (click)="showModal = false"><i class="fas fa-times"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Title</label>
            <div class="input-wrap">
              <i class="fas fa-trophy"></i>
              <input type="text" [(ngModel)]="currentChallenge.title" placeholder="e.g. Weekly Contributor">
            </div>
          </div>
          <div class="form-group">
            <label>Description</label>
            <div class="input-wrap">
              <i class="fas fa-align-left"></i>
              <textarea rows="3" [(ngModel)]="currentChallenge.description"></textarea>
            </div>
          </div>
          <div class="form-group">
            <label>Automation Hook (Logic)</label>
            <select class="form-control" [(ngModel)]="currentChallenge.challengeTypeCode">
              <option value="MANUAL">Manual / Simulated</option>
              <option value="REG_EVENT">Event Registration (Check real data)</option>
              <option value="FIRST_BADGE">First Badge Earned (Check real data)</option>
            </select>
            <small class="help-text">Determines how the system verifies mission success.</small>
          </div>
          <div class="form-row-2">
            <div class="form-group">
              <label>XP Reward</label>
              <div class="input-wrap">
                <i class="fas fa-bolt"></i>
                <input type="number" [(ngModel)]="currentChallenge.xpReward">
              </div>
            </div>
            <div class="form-group">
              <label>Deadline</label>
              <div class="input-wrap">
                <i class="fas fa-calendar-alt"></i>
                <input type="date" [(ngModel)]="currentChallenge.deadline">
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>Status</label>
            <select class="form-control" [(ngModel)]="currentChallenge.status">
              <option value="ACTIVE">ACTIVE</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="EXPIRED">EXPIRED</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" (click)="showModal = false">Cancel</button>
          <button class="btn-add" (click)="saveChallenge()">
            {{ isEditing ? 'Save Changes' : 'Create Challenge' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; padding: 20px; }

    .animate-fade-in { animation: fadeIn 0.6s ease-out; }
    .animate-slide-up { animation: slideUp 0.6s ease-out; }
    @keyframes fadeIn  { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

    .page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; gap: 16px; }
    .header-left  { display: flex; flex-direction: column; gap: 4px; }
    .header-actions { display: flex; align-items: center; gap: 10px; }

    .badge-label { display: inline-flex; padding: 3px 10px; border-radius: 99px; font-size: 11px; font-weight: 600; letter-spacing: 0.4px; text-transform: uppercase; background: rgba(99,102,241,0.12); color: #6366F1; margin-bottom: 4px; }
    .page-title    { font-family: 'Space Grotesk', sans-serif; font-size: 22px; font-weight: 700; color: #F1F5F9; line-height: 1.3; margin: 0; }
    .page-subtitle { font-size: 13px; color: #94A3B8; margin: 0; }

    .glow-line { height: 2px; background: linear-gradient(90deg, #6366F1, transparent); border-radius: 99px; margin-bottom: 24px; }

    /* Stats Row */
    .stats-row { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 20px; }
    .stat-pill { display: flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: 99px; font-size: 13px; font-weight: 700; border: 1px solid; cursor: default; transition: 0.2s; }
    .stat-pill:hover { transform: translateY(-2px); }
    .stat-pill.green   { background: rgba(16,185,129,0.08); color: #10B981; border-color: rgba(16,185,129,0.2); }
    .stat-pill.orange  { background: rgba(245,158,11,0.08); color: #F59E0B; border-color: rgba(245,158,11,0.2); }
    .stat-pill.red     { background: rgba(239,68,68,0.08); color: #F87171; border-color: rgba(239,68,68,0.2); }
    .stat-pill.accent  { background: rgba(99,102,241,0.08); color: #818CF8; border-color: rgba(99,102,241,0.2); }
    .stat-pill i { font-size: 12px; }

    .table-card { background: #161B27; border: 1px solid rgba(255,255,255,0.06); border-radius: 20px; overflow: hidden; box-shadow: 0 8px 40px rgba(0,0,0,0.4); }
    .card-head { padding: 20px 24px; border-bottom: 1px solid rgba(255,255,255,0.05); display: flex; align-items: center; justify-content: space-between; }
    .card-head-title { font-family: 'Space Grotesk', sans-serif; font-size: 15px; font-weight: 600; color: #F1F5F9; }
    .card-head-count { background: rgba(99,102,241,0.1); color: #6366F1; padding: 4px 12px; border-radius: 99px; font-size: 11px; font-weight: 700; }

    .challenges-table { width: 100%; border-collapse: collapse; font-size: 13px; }
    .challenges-table thead th { padding: 12px 20px; text-align: left; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.6px; color: #4B5563; border-bottom: 1px solid rgba(255,255,255,0.05); white-space: nowrap; background: rgba(255,255,255,0.01); }
    .challenges-table tbody tr { border-bottom: 1px solid rgba(255,255,255,0.04); transition: 0.2s; }
    .challenges-table tbody tr:last-child { border-bottom: none; }
    .challenges-table tbody tr:hover { background: rgba(99,102,241,0.04); }
    .challenges-table tbody td { padding: 16px 20px; color: #94A3B8; vertical-align: middle; }

    /* Urgent row highlight */
    .urgent-row { border-left: 3px solid rgba(239,68,68,0.4) !important; }

    .challenge-name { font-weight: 600; color: #F1F5F9; font-size: 14px; margin-bottom: 3px; }
    .challenge-desc { font-size: 11px; color: #4B5563; }

    .hook-pill { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08); padding: 4px 10px; border-radius: 6px; font-size: 10px; font-weight: 700; text-transform: uppercase; color: #94A3B8; }

    .xp-chip { display: flex; align-items: center; gap: 6px; background: rgba(80,205,137,0.1); color: #10B981; padding: 4px 10px; border-radius: 20px; font-weight: 700; width: fit-content; }
    .xp-chip i { font-size: 10px; }

    .date-text { font-size: 13px; color: #94A3B8; font-weight: 500; }

    /* Urgency bar */
    .urgency-col { display: flex; flex-direction: column; gap: 4px; min-width: 100px; }
    .deadline-bar-track { width: 100%; height: 5px; background: rgba(255,255,255,0.06); border-radius: 10px; overflow: hidden; }
    .deadline-bar-fill { height: 100%; border-radius: 10px; background: #10B981; transition: width 0.5s; }
    .deadline-bar-fill.fill-warning { background: #F59E0B; }
    .deadline-bar-fill.fill-danger  { background: #EF4444; animation: pulse-bar 1.5s ease-in-out infinite; }
    @keyframes pulse-bar { 0%,100% { opacity:1; } 50% { opacity:0.5; } }
    .days-left { font-size: 11px; font-weight: 700; color: #94A3B8; }
    .days-left.text-danger  { color: #F87171; }
    .days-left.text-warning { color: #F59E0B; }
    .no-urgency { color: #4B5563; font-size: 12px; }

    .status-badge { padding: 4px 10px; border-radius: 6px; font-size: 10px; font-weight: 800; text-transform: uppercase; background: rgba(148,163,184,0.08); color: #94A3B8; }
    .status-badge.active    { background: rgba(16,185,129,0.10); color: #10B981; }
    .status-badge.expired   { background: rgba(239,68,68,0.10);  color: #F87171; }
    .status-badge.completed { background: rgba(99,102,241,0.10); color: #818CF8; }

    .action-group { display: flex; gap: 6px; justify-content: flex-end; }
    .btn-icon-modern { width: 36px; height: 36px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.05); background: rgba(255,255,255,0.03); color: #94A3B8; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.2s; }
    .btn-icon-modern:hover      { transform: translateY(-2px); color: #fff; background: rgba(255,255,255,0.08); }
    .btn-icon-modern.edit:hover { background: rgba(99,102,241,0.12); color: #818CF8; border-color: rgba(99,102,241,0.3); }
    .btn-icon-modern.del:hover  { background: rgba(239,68,68,0.12); color: #F87171; border-color: rgba(239,68,68,0.3); }

    .empty-row { text-align: center; padding: 60px 20px; color: #4B5563; font-size: 14px; }
    .text-end { text-align: right; }

    .btn-add { display: flex; align-items: center; gap: 8px; background: #6366F1; color: #fff; border: none; padding: 9px 18px; border-radius: 10px; font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.2s; box-shadow: 0 4px 15px rgba(99,102,241,0.3); font-family: inherit; }
    .btn-add:hover { background: #4F52D9; transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99,102,241,0.4); }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.65); backdrop-filter: blur(10px); z-index: 1000; display: flex; align-items: center; justify-content: center; animation: fadeIn 0.3s ease; }
    .modal-content { background: rgba(22, 27, 39, 0.92); border: 1px solid rgba(255,255,255,0.12); border-radius: 28px; width: 560px; max-width: 95vw; max-height: 90vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 40px 100px rgba(0,0,0,0.6); animation: modalIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
    @keyframes modalIn { from { opacity: 0; transform: scale(0.92) translateY(20px); } to { opacity: 1; transform: scale(1) translateY(0); } }

    .modal-header { padding: 28px 32px 20px; border-bottom: 1px solid rgba(255,255,255,0.06); display: flex; justify-content: space-between; align-items: flex-start; flex-shrink: 0; }
    .modal-badge { font-size: 10px; font-weight: 800; color: #6366F1; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px; display: block; }
    .modal-header h2 { font-family: 'Space Grotesk', sans-serif; font-size: 20px; font-weight: 700; color: #F1F5F9; margin: 0; }
    .modal-close { width: 32px; height: 32px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.04); color: #94A3B8; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 16px; transition: 0.2s; }
    .modal-close:hover { color: #fff; background: rgba(255,255,255,0.1); transform: rotate(90deg); }

    .modal-body { padding: 24px 32px; overflow-y: auto; flex: 1; }
    .modal-footer { padding: 20px 32px 28px; display: flex; justify-content: flex-end; gap: 12px; border-top: 1px solid rgba(255,255,255,0.06); flex-shrink: 0; }

    .form-group   { display: flex; flex-direction: column; gap: 6px; margin-bottom: 18px; }
    .form-row-2   { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
    .form-group label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #4B5563; }

    .input-wrap { position: relative; display: flex; align-items: center; border-radius: 12px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); transition: 0.2s; }
    .input-wrap:focus-within { border-color: #6366F1; background: rgba(255,255,255,0.06); box-shadow: 0 0 0 3px rgba(99,102,241,0.12); }
    .input-wrap i { position: absolute; left: 14px; font-size: 13px; color: #4B5563; pointer-events: none; }
    .input-wrap input, .input-wrap textarea { background: transparent; border: none; padding: 11px 14px 11px 40px; color: #F1F5F9; width: 100%; outline: none; font-size: 14px; font-family: inherit; }
    .input-wrap textarea { padding-top: 11px; resize: vertical; min-height: 80px; }

    .form-control { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 11px 14px; color: #F1F5F9; outline: none; transition: 0.2s; font-size: 14px; width: 100%; font-family: inherit; }
    .form-control:focus { border-color: #6366F1; background: rgba(255,255,255,0.06); box-shadow: 0 0 0 3px rgba(99,102,241,0.12); }
    .form-control option { background: #1C2333; }

    .help-text { font-size: 11px; color: #4B5563; margin-top: 4px; font-style: italic; }

    .btn-secondary { background: rgba(255,255,255,0.04); color: #94A3B8; border: 1px solid rgba(255,255,255,0.08); padding: 9px 18px; border-radius: 10px; font-weight: 600; cursor: pointer; font-family: inherit; font-size: 13px; transition: 0.2s; }
    .btn-secondary:hover { background: rgba(255,255,255,0.08); color: #F1F5F9; }
  `]
})
export class ChallengesAdminComponent implements OnInit {
  challenges: ChallengeDTO[] = [];
  showModal = false;
  isEditing = false;
  currentChallenge: Partial<ChallengeDTO> = {};

  constructor(private challengeService: ChallengeAdminService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.challengeService.getAllChallenges().subscribe(data => this.challenges = data);
  }

  countByStatus(status: string): number {
    return this.challenges.filter(c => c.status === status).length;
  }

  getTotalXP(): number {
    return this.challenges.reduce((sum, c) => sum + (c.xpReward || 0), 0);
  }

  /** Returns how many days remain until deadline (negative = past) */
  getDaysLeft(deadline: string): number {
    if (!deadline) return 0;
    const now = new Date();
    const end = new Date(deadline);
    const diff = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return diff;
  }

  /** Returns percentage of time ELAPSED since a 30-day window before deadline */
  getDeadlinePercent(deadline: string): number {
    const daysLeft = this.getDaysLeft(deadline);
    if (daysLeft <= 0) return 100;
    if (daysLeft >= 30) return 5;
    return Math.round(((30 - daysLeft) / 30) * 100);
  }

  openModal(): void {
    this.isEditing = false;
    this.currentChallenge = {
      title: '',
      description: '',
      xpReward: 100,
      challengeTypeCode: 'MANUAL',
      status: 'ACTIVE'
    };
    this.showModal = true;
  }

  editChallenge(c: ChallengeDTO): void {
    this.isEditing = true;
    this.currentChallenge = { ...c };
    if (this.currentChallenge.deadline) {
      this.currentChallenge.deadline = this.currentChallenge.deadline.split('T')[0];
    }
    this.showModal = true;
  }

  saveChallenge(): void {
    if (this.isEditing && this.currentChallenge.id) {
      this.challengeService.updateChallenge(this.currentChallenge.id, this.currentChallenge as ChallengeDTO).subscribe({
        next: () => { this.showModal = false; this.refresh(); }
      });
    } else {
      this.challengeService.createChallenge(this.currentChallenge as ChallengeDTO).subscribe({
        next: () => { this.showModal = false; this.refresh(); }
      });
    }
  }

  deleteChallenge(id: number): void {
    if (confirm('Are you sure you want to delete this challenge?')) {
      this.challengeService.deleteChallenge(id).subscribe(() => this.refresh());
    }
  }
}
