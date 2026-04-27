import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { trigger, transition, style, animate } from '@angular/animations';
import { BadgeAdminService } from '../services/badge-admin.service';
import { BadgeDTO, BadgeRarity } from '../models/engagement.models';
import { UserService, UserDTO } from '../../../core/services/user.service';

@Component({
  selector: 'app-badges-admin',
  animations: [
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('300ms ease-out', style({ opacity: 1 })),
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0 })),
      ])
    ]),
    trigger('slideUp', [
      transition(':enter', [
        style({ transform: 'translate(-50%, -40%)', opacity: 0 }),
        animate('400ms cubic-bezier(0.34, 1.56, 0.64, 1)', style({ transform: 'translate(-50%, -50%)', opacity: 1 })),
      ]),
      transition(':leave', [
        animate('250ms ease-in', style({ transform: 'translate(-50%, -45%)', opacity: 0 })),
      ])
    ])
  ],
  template: `
    <div class="page-header animate-fade-in">
      <div class="header-left">
        <span class="badge badge-accent mb-2">Engagement System</span>
        <h1 class="page-title">Badges Administration</h1>
        <p class="page-subtitle">Configure Gamification Badges & Rewards for your community.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" (click)="openCreateModal()">
          <i class="fas fa-magic"></i> Add New Badge
        </button>
      </div>
    </div>

    <div class="glow-line"></div>
    
    <!-- Modals -->
    <div class="modal-backdrop" *ngIf="showModal" (click)="closeModal()" @fadeIn></div>
    <div class="modal-container" *ngIf="showModal" @slideUp>
      <div class="modal-card premium-glass">
        <div class="modal-header">
          <div class="header-info">
             <i class="fas" [class]="editingBadge ? 'fa-edit' : 'fa-plus-circle'"></i>
             <div class="title-group">
                <h3>{{ editingBadge ? 'Edit Badge' : 'Add New Badge' }}</h3>
                <span>Configure badge properties and rarity</span>
             </div>
          </div>
          <button class="close-btn" (click)="closeModal()">&times;</button>
        </div>
        
        <div class="modal-body-layout">
          <!-- Form Section -->
          <form [formGroup]="badgeForm" (ngSubmit)="saveBadge()" class="modal-form">
            <div class="form-row">
              <div class="form-group flex-2">
                <label>Badge Name</label>
                <div class="input-wrapper">
                  <i class="fas fa-tag"></i>
                  <input type="text" formControlName="name" placeholder="e.g. Master Contributor" />
                </div>
              </div>
              <div class="form-group flex-1">
                <label>Internal Code</label>
                <div class="input-wrapper">
                  <i class="fas fa-code"></i>
                  <input type="text" formControlName="code" placeholder="MASTER_CONTRIB" />
                </div>
              </div>
            </div>
            
            <div class="form-group">
              <label>Description</label>
              <textarea formControlName="description" rows="2" placeholder="Describe the achievement..."></textarea>
            </div>
            
            <div class="form-row">
              <div class="form-group">
                <label>Rarity Tier</label>
                <div class="custom-select">
                  <select formControlName="rarity">
                    <option value="COMMON">Common (Normal)</option>
                    <option value="RARE">Rare (Vibrant)</option>
                    <option value="EPIC">Epic (Mythic)</option>
                    <option value="LEGENDARY">Legendary (Ancient)</option>
                  </select>
                  <i class="fas fa-chevron-down select-arrow"></i>
                </div>
              </div>
              <div class="form-group">
                <label>XP Reward</label>
                <div class="input-wrapper">
                  <i class="fas fa-bolt text-warning"></i>
                  <input type="number" formControlName="xpReward" />
                </div>
              </div>
            </div>
            
            <div class="form-group">
              <label>Icon Identifier (FontAwesome)</label>
              <div class="input-wrapper">
                 <i class="fas fa-icons"></i>
                 <input type="text" formControlName="iconUrl" placeholder="fas fa-medal" />
              </div>
            </div>
          </form>

          <!-- Divider -->
          <div class="modal-divider-v"></div>

          <!-- Preview Section -->
          <div class="preview-section">
            <h4 class="preview-title">Live Preview</h4>
            <div class="preview-container">
               <div class="preview-badge-card" [ngClass]="'preview-' + badgeForm.get('rarity')?.value.toLowerCase()">
                  <div class="badge-icon-outer">
                    <i [class]="badgeForm.get('iconUrl')?.value || 'fas fa-award'"></i>
                  </div>
                  <div class="badge-info-outer">
                    <span class="preview-rarity">{{ badgeForm.get('rarity')?.value }}</span>
                    <h5 class="preview-name">{{ badgeForm.get('name')?.value || 'Untitled Badge' }}</h5>
                    <p class="preview-desc">{{ badgeForm.get('description')?.value || 'Provide a description...' }}</p>
                    <div class="preview-xp">
                      <i class="fas fa-bolt"></i>
                      <span>+{{ badgeForm.get('xpReward')?.value }} XP</span>
                    </div>
                  </div>
               </div>
            </div>
             <p class="preview-hint">This is how users will see the badge on their profile.</p>
          </div>
        </div>

        <div class="modal-footer mt-4">
          <button type="button" class="btn btn-ghost" (click)="closeModal()">Cancel</button>
          <button type="submit" class="btn btn-primary ml-auto" (click)="saveBadge()" [disabled]="badgeForm.invalid">
            {{ editingBadge ? 'Update Configuration' : 'Create Badge Now' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Main Table -->
    <div class="card animate-slide-up">
      <div class="card-header border-0">
        <h3 class="card-title">Badge Catalog</h3>
        <span class="badge badge-muted">{{ badges.length }} total badges</span>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="tw-table">
            <thead>
              <tr>
                <th width="80">Icon</th>
                <th width="120">Code</th>
                <th>Identity & Purpose</th>
                <th width="120">Rarity</th>
                <th width="100">Reward</th>
                <th>Active Owners</th>
                <th width="100">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let b of badges" class="badge-row">
                <td>
                  <div class="list-badge-icon" [ngClass]="getRarityBg(b.rarity)">
                    <i [class]="b.iconUrl || 'fas fa-award'"></i>
                    <div class="icon-glow"></div>
                  </div>
                </td>
                <td><code class="code-label">{{ b.code }}</code></td>
                <td>
                  <div class="name-col">
                    <strong>{{ b.name }}</strong>
                    <span class="desc-text">{{ b.description }}</span>
                  </div>
                </td>
                <td><span class="rarity-tag" [ngClass]="'rarity-' + b.rarity.toLowerCase()">{{ b.rarity }}</span></td>
                <td>
                  <div class="xp-chip">
                    <i class="fas fa-bolt"></i>
                    <span>{{ b.xpReward }}</span>
                  </div>
                </td>
                <td>
                  <div class="owners-stack" *ngIf="b.ownerIds && b.ownerIds.length > 0; else noOwners">
                    <div class="avatar-group">
                       <div *ngFor="let id of b.ownerIds | slice:0:4" 
                            class="avatar avatar-sm avatar-ring clickable"
                            (click)="goToUserDetails(id)"
                            [title]="getOwnerName(id)">
                          <img *ngIf="getOwner(id)?.photo; else noPhoto" [src]="getOwner(id)?.photo">
                          <ng-template #noPhoto>
                            <div class="avatar-placeholder">{{ getOwnerName(id) | slice:0:1 }}</div>
                          </ng-template>
                       </div>
                       <div class="avatar-more" *ngIf="b.ownerIds.length > 4">
                         +{{ b.ownerIds.length - 4 }}
                       </div>
                    </div>
                  </div>
                  <ng-template #noOwners>
                    <span class="text-muted small">None yet</span>
                  </ng-template>
                </td>
                <td>
                  <div class="action-btns">
                    <button class="btn-icon-soft" (click)="openEditModal(b)" title="Customize">
                      <i class="fas fa-pen-nib"></i>
                    </button>
                    <button class="btn-icon-soft danger" (click)="deleteBadge(b.id)" title="Archive">
                      <i class="fas fa-trash-alt"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; padding: 20px; }
    
    .animate-fade-in { animation: fadeIn 0.6s ease-out; }
    .animate-slide-up { animation: slideUp 0.6s ease-out; }
    
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

    /* Modal Styling */
    .modal-backdrop { 
      position: fixed; top: 0; left: 0; width: 100%; height: 100%; 
      background: rgba(0,0,0,0.6); backdrop-filter: blur(8px); z-index: 1000;
      animation: fadeIn 0.3s ease;
    }
    
    .modal-container { 
      position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); 
      z-index: 1001; width: 95%; max-width: 900px;
    }
    
    .premium-glass {
      background: rgba(22, 27, 39, 0.85);
      border: 1px solid rgba(255,255,255,0.12);
      border-radius: 28px;
      padding: 32px;
      box-shadow: 0 40px 100px rgba(0,0,0,0.6), inset 0 0 0 1px rgba(255,255,255,0.05);
      color: #fff;
    }
    
    .modal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
    .header-info { display: flex; align-items: center; gap: 16px; }
    .header-info i { font-size: 24px; color: var(--accent); }
    .title-group h3 { font-family: var(--font-display); font-size: 22px; font-weight: 700; margin: 0; }
    .title-group span { font-size: 13px; color: var(--text-secondary); }
    .close-btn { font-size: 28px; color: var(--text-muted); cursor: pointer; transition: 0.2s; }
    .close-btn:hover { color: #fff; transform: rotate(90deg); }

    .modal-body-layout { display: flex; gap: 32px; }
    .modal-form { flex: 1.2; display: flex; flex-direction: column; gap: 18px; }
    .modal-divider-v { width: 1px; background: rgba(255,255,255,0.06); }
    .preview-section { flex: 0.8; display: flex; flex-direction: column; align-items: center; text-align: center; }
    
    .preview-title { font-size: 11px; font-weight: 800; text-transform: uppercase; color: var(--text-muted); margin-bottom: 24px; letter-spacing: 2px; }
    .preview-container { width: 100%; display: flex; justify-content: center; margin-bottom: 20px; }
    
    /* Live Preview Card */
    .preview-badge-card {
      width: 260px; padding: 24px; border-radius: 24px; background: rgba(255,255,255,0.03); 
      border: 1px solid rgba(255,255,255,0.1); display: flex; flex-direction: column; gap: 16px;
      transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); position: relative; overflow: hidden;
    }
    
    .badge-icon-outer { 
      width: 72px; height: 72px; border-radius: 20px; display: flex; align-items: center; justify-content: center;
      font-size: 32px; margin: 0 auto; transition: 0.3s;
    }
    
    .preview-rarity { font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
    .preview-name { font-size: 17px; font-weight: 700; margin: 0; }
    .preview-desc { font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin: 0; height: 36px; overflow: hidden; }
    .preview-xp { display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 14px; font-weight: 700; color: #fff; }
    .preview-xp i { color: #f59e0b; }
    
    /* Rarity Special Effects */
    .preview-common { border-color: rgba(255,255,255,0.2); }
    .preview-common .badge-icon-outer { background: linear-gradient(135deg, #333, #555); color: #ddd; }
    
    .preview-rare { border-color: rgba(59, 130, 246, 0.4); box-shadow: 0 0 20px rgba(59, 130, 246, 0.1); }
    .preview-rare .badge-icon-outer { background: linear-gradient(135deg, #1e3a8a, #3b82f6); color: #fff; }
    .preview-rare .preview-rarity { color: #3b82f6; }
    
    .preview-epic { border-color: rgba(139, 92, 246, 0.4); box-shadow: 0 0 20px rgba(139, 92, 246, 0.15); }
    .preview-epic .badge-icon-outer { background: linear-gradient(135deg, #4c1d95, #8b5cf6); color: #fff; }
    .preview-epic .preview-rarity { color: #a78bfa; }
    
    .preview-legendary { border-color: rgba(245, 158, 11, 0.5); box-shadow: 0 0 30px rgba(245, 158, 11, 0.2); }
    .preview-legendary .badge-icon-outer { background: linear-gradient(135deg, #78350f, #f59e0b); color: #fff; }
    .preview-legendary .preview-rarity { color: #f59e0b; }
    .preview-legendary::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: radial-gradient(circle at 50% 0%, rgba(245,158,11,0.1), transparent); pointer-events: none; }

    .preview-hint { font-size: 11px; color: var(--text-muted); font-style: italic; }

    /* Inputs Wrapper */
    .input-wrapper { 
      position: relative; display: flex; align-items: center; 
      margin-top: 4px; border-radius: 12px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); transition: 0.2s;
    }
    .input-wrapper:focus-within { border-color: var(--accent); background: rgba(255,255,255,0.06); box-shadow: 0 0 0 3px var(--accent-soft); }
    .input-wrapper i { position: absolute; left: 14px; font-size: 14px; color: var(--text-muted); }
    .input-wrapper input { background: transparent; border: none; padding: 12px 14px 12px 42px; color: #fff; width: 100%; outline: none; font-size: 14px; }
    
    textarea { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 12px; color: #fff; outline: none; transition: 0.2s; font-size: 14px; width: 100%; }
    textarea:focus { border-color: var(--accent); background: rgba(255,255,255,0.06); }
    
    .form-row { display: flex; gap: 16px; }
    .flex-2 { flex: 2; } .flex-1 { flex: 1; }
    
    .custom-select { position: relative; display: flex; align-items: center; margin-top: 4px; }
    .custom-select select { 
      width: 100%; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); 
      border-radius: 12px; padding: 12px; color: #fff; -webkit-appearance: none; outline: none; font-size: 14px;
    }
    .select-arrow { position: absolute; right: 14px; font-size: 12px; color: var(--text-muted); pointer-events: none; }

    /* List Table Styles */
    .list-badge-icon { 
      width: 52px; height: 52px; border-radius: 16px; display: flex; align-items: center; justify-content: center;
      font-size: 1.5rem; position: relative; overflow: hidden;
    }
    .icon-glow { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: radial-gradient(circle, rgba(255,255,255,0.1), transparent); }
    
    .common-bg { background: rgba(255,255,255,0.05); color: #fff; border: 1px solid rgba(255,255,255,0.1); }
    .rare-bg { background: rgba(59, 130, 246, 0.15); color: #3b82f6; border: 1px solid rgba(59, 130, 246, 0.3); }
    .epic-bg { background: rgba(139, 92, 246, 0.15); color: #a78bfa; border: 1px solid rgba(139, 92, 246, 0.3); }
    .legendary-bg { background: rgba(245, 158, 11, 0.15); color: #f59e0b; border: 1px solid rgba(245, 158, 11, 0.3); }

    .code-label { background: rgba(255,255,255,0.05); padding: 4px 8px; border-radius: 6px; font-size: 11px; color: var(--accent); }
    .name-col { display: flex; flex-direction: column; gap: 4px; }
    .name-col strong { color: #fff; font-size: 14px; }
    .desc-text { font-size: 12px; color: var(--text-secondary); max-width: 250px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .rarity-tag { padding: 4px 10px; border-radius: 6px; font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; }
    .rarity-common { background: rgba(255,255,255,0.05); color: #fff; }
    .rarity-rare { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
    .rarity-epic { background: rgba(139, 92, 246, 0.1); color: #a78bfa; }
    .rarity-legendary { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }

    .xp-chip { display: flex; align-items: center; gap: 6px; background: rgba(80, 205, 137, 0.1); color: #50cd89; padding: 4px 10px; border-radius: 20px; font-weight: 700; width: fit-content; }
    .xp-chip i { font-size: 10px; }

    .avatar-group { display: flex; }
    .avatar-ring { border: 2px solid var(--bg-card); position: relative; margin-left: -10px; }
    .avatar-ring:first-child { margin-left: 0; }
    .avatar-group:hover .avatar-ring { margin-left: -5px; }
    .avatar-placeholder { width: 100%; height: 100%; border-radius: 50%; background: var(--accent); color: #fff; display: flex; align-items: center; justify-content: center; }
    .avatar-more { width: 32px; height: 32px; border-radius: 50%; background: var(--bg-card-hover); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 10px; margin-left: -10px; position: relative; z-index: 5; }
    
    .btn-icon-soft { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.03); color: var(--text-secondary); border: 1px solid rgba(255,255,255,0.05); transition: 0.2s; cursor: pointer; }
    .btn-icon-soft:hover { background: var(--accent-soft); color: var(--accent); border-color: var(--accent); transform: translateY(-2px); }
    .btn-icon-soft.danger:hover { background: var(--danger-soft); color: var(--danger); border-color: var(--danger); }
    
    .clickable { cursor: pointer; transition: 0.2s; }
    .clickable:hover { z-index: 10; transform: scale(1.1); }
  `]
})
export class BadgesAdminComponent implements OnInit {
  badges: BadgeDTO[] = [];
  userMap: Map<number, UserDTO> = new Map();
  badgeForm: FormGroup;
  showModal = false;
  editingBadge: BadgeDTO | null = null;

  constructor(
    private badgeService: BadgeAdminService,
    private userService: UserService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.badgeForm = this.fb.group({
      name: ['', Validators.required],
      code: ['', Validators.required],
      description: ['', Validators.required],
      rarity: ['COMMON', Validators.required],
      xpReward: [50, [Validators.required, Validators.min(0)]],
      iconUrl: ['fas fa-award', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadUsers();
    this.loadBadges();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe(users => {
      users.forEach(u => {
        this.userMap.set(u.id, u);
      });
    });
  }

  loadBadges(): void {
    this.badgeService.getAll().subscribe(data => this.badges = data);
  }

  getOwner(id: number): UserDTO | undefined {
    return this.userMap.get(id);
  }

  getOwnerName(id: number): string {
    const user = this.getOwner(id);
    return user ? `${user.firstName} ${user.lastName}` : `User #${id}`;
  }

  getOwnerTooltip(id: number): string {
    const user = this.getOwner(id);
    if (!user) return `User #${id}`;
    return `${user.firstName} ${user.lastName}\nEmail: ${user.email}\nRole: ${user.role}`;
  }

  goToUserDetails(id: number): void {
    this.router.navigate(['/admin/users', id]);
  }

  openCreateModal(): void {
    this.editingBadge = null;
    this.badgeForm.reset({ rarity: 'COMMON', xpReward: 50, iconUrl: 'fas fa-award' });
    this.showModal = true;
  }

  openEditModal(badge: BadgeDTO): void {
    this.editingBadge = badge;
    this.badgeForm.patchValue(badge);
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveBadge(): void {
    if (this.badgeForm.invalid) return;

    const data = this.badgeForm.value;
    if (this.editingBadge) {
      this.badgeService.update(this.editingBadge.id, data).subscribe(() => {
        this.loadBadges();
        this.closeModal();
      });
    } else {
      this.badgeService.create(data).subscribe(() => {
        this.loadBadges();
        this.closeModal();
      });
    }
  }

  deleteBadge(id: number): void {
    if (confirm('Are you sure you want to delete this badge? This will remove it from all users.')) {
      this.badgeService.delete(id).subscribe(() => this.loadBadges());
    }
  }

  getRarityBg(rarity: BadgeRarity): string {
    const bg: Record<string, string> = {
      COMMON: 'common-bg',
      RARE: 'rare-bg',
      EPIC: 'epic-bg',
      LEGENDARY: 'legendary-bg'
    };
    return bg[rarity] || 'common-bg';
  }
}
