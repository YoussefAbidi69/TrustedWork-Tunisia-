import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { ApiService } from '../../../core/services/api.service';

const API_BASE = 'http://localhost:8081/api';
const DEFAULT_AVATAR = 'data:image/svg+xml;utf8,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160">
  <rect width="160" height="160" rx="24" fill="#1e293b"/>
  <circle cx="80" cy="60" r="28" fill="#94a3b8"/>
  <path d="M40 128c8-22 28-34 40-34s32 12 40 34" fill="#94a3b8"/>
</svg>
`);

interface ProfileModel {
  fullName: string; firstName: string; lastName: string;
  headline: string; location: string; bio: string;
  phone: string; photo: string; avatar: string;
  skills: string[]; cin: number | null;
  trustLevel: number; kycStatus: string;
  twoFactorEnabled: boolean; portfolioAttached: boolean;
  certificationsAdded: boolean; trustPassportCompleted: boolean;
}

@Component({
  selector: 'app-profile-overview',
  templateUrl: './profile-overview.component.html',
  styleUrls: ['./profile-overview.component.css']
})
export class ProfileOverviewComponent implements OnInit {
  loading = true;
  saving = false;
  error = '';
  successMessage = '';
  editMode = false;
  newSkill = '';
  selectedAvatarFile: File | null = null;

  private userId: number | null = null;
  private readonly SKILLS_KEY = 'profile_skills';

  profile: ProfileModel = {
    fullName: '', firstName: '', lastName: '',
    headline: '', location: 'Tunisie', bio: '',
    phone: '', photo: '', avatar: '', skills: [],
    cin: null, trustLevel: 1, kycStatus: 'PENDING',
    twoFactorEnabled: false, portfolioAttached: false,
    certificationsAdded: false, trustPassportCompleted: false
  };

  draftProfile: ProfileModel = { ...this.profile, skills: [] };

  constructor(private userService: UserService, private api: ApiService) {}

  ngOnInit(): void { this.loadProfile(); }

  private loadSkillsFromStorage(): string[] {
    if (!this.userId) return [];
    try {
      const stored = localStorage.getItem(`${this.SKILLS_KEY}_${this.userId}`);
      return stored ? JSON.parse(stored) : [];
    } catch { return []; }
  }

  private saveSkillsToStorage(skills: string[]): void {
    if (!this.userId) return;
    localStorage.setItem(`${this.SKILLS_KEY}_${this.userId}`, JSON.stringify(skills));
  }

  private resolvePhotoUrl(photo?: string): string {
    if (!photo) return DEFAULT_AVATAR;
    if (photo.startsWith('http://') || photo.startsWith('https://') || photo.startsWith('data:')) return photo;
    return `${API_BASE}${photo}`;
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';
    this.userService.getMyProfile().pipe(finalize(() => (this.loading = false))).subscribe({
      next: (data: UserProfileResponse) => {
        const kycApproved = data.kycStatus === 'APPROVED';
        const twoFa       = data.twoFactorEnabled || false;
        const trust       = data.trustLevel ?? 1;
        const photoUrl    = this.resolvePhotoUrl(data.photo);
        this.userId       = (data as any).id ?? (data as any).userId ?? null;
        const savedSkills = this.loadSkillsFromStorage();

        this.profile = {
          fullName:               `${data.firstName || ''} ${data.lastName || ''}`.trim(),
          firstName:              data.firstName || '',
          lastName:               data.lastName  || '',
          headline:               data.headline  || '',
          location:               data.location  || 'Tunisie',
          bio:                    data.bio        || '',
          phone:                  data.phone      || '',
          photo:                  photoUrl,
          avatar:                 photoUrl,
          skills:                 savedSkills,
          cin:                    typeof data.cin === 'number' ? data.cin : Number(data.cin) || null,
          trustLevel:             trust,
          kycStatus:              data.kycStatus  || 'PENDING',
          twoFactorEnabled:       twoFa,
          portfolioAttached:      false,
          certificationsAdded:    kycApproved,
          trustPassportCompleted: trust >= 3
        };
        this.draftProfile = { ...this.profile, skills: [...this.profile.skills] };
      },
      error: (err: HttpErrorResponse) => {
        this.error = err?.error?.error || err?.error?.message || 'Impossible de charger le profil.';
      }
    });
  }

  get completion(): number {
    const checks = [
      !!this.profile.fullName, !!this.profile.phone,
      this.profile.kycStatus === 'APPROVED', this.profile.twoFactorEnabled,
      this.profile.trustLevel >= 3, this.profile.skills.length > 0
    ];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  }

  get completionItems(): { label: string; done: boolean }[] {
    return [
      { label: 'Basic info',      done: !!this.profile.fullName },
      { label: 'Phone',           done: !!this.profile.phone },
      { label: 'KYC approuvé',    done: this.profile.kycStatus === 'APPROVED' },
      { label: '2FA activé',      done: this.profile.twoFactorEnabled },
      { label: 'Trust Level ≥ 3', done: this.profile.trustLevel >= 3 },
      { label: 'Skills',          done: this.profile.skills.length > 0 }
    ];
  }

  toggleEdit(): void {
    this.error = ''; this.successMessage = '';
    if (!this.editMode) {
      this.draftProfile = { ...this.profile, skills: [...this.profile.skills] };
      this.selectedAvatarFile = null;
    }
    this.editMode = !this.editMode;
  }

  cancelEdit(): void {
    this.draftProfile = { ...this.profile, skills: [...this.profile.skills] };
    this.selectedAvatarFile = null;
    this.editMode = false;
    this.error = ''; this.successMessage = '';
  }

  saveProfile(): void {
    if (!this.profile.cin) { this.error = 'CIN introuvable.'; return; }
    this.saving = true; this.error = ''; this.successMessage = '';

    const formData = new FormData();
    formData.append('firstName', this.draftProfile.firstName || '');
    formData.append('lastName',  this.draftProfile.lastName  || '');
    formData.append('phone',     this.draftProfile.phone     || '');
    formData.append('headline',  this.draftProfile.headline  || '');
    formData.append('location',  this.draftProfile.location  || '');
    formData.append('bio',       this.draftProfile.bio       || '');
    if (this.selectedAvatarFile) formData.append('photo', this.selectedAvatarFile);

    this.api.put(`/users/${this.profile.cin}`, formData).pipe(
      finalize(() => (this.saving = false))
    ).subscribe({
      next: () => {
        this.saveSkillsToStorage(this.draftProfile.skills);
        this.successMessage = 'Profil mis à jour avec succès.';
        this.editMode = false;
        this.selectedAvatarFile = null;
        this.loadProfile();
      },
      error: (err: HttpErrorResponse) => {
        this.error = err?.error?.error || err?.error?.message || err?.error?.details || 'Erreur lors de la sauvegarde.';
      }
    });
  }

  addSkill(): void {
    const value = this.newSkill.trim();
    if (!value) return;
    const exists = this.draftProfile.skills.some(s => s.toLowerCase() === value.toLowerCase());
    if (!exists) this.draftProfile.skills = [...this.draftProfile.skills, value];
    this.newSkill = '';
  }

  removeSkill(skill: string): void {
    this.draftProfile.skills = this.draftProfile.skills.filter(s => s !== skill);
  }

  onSkillInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') { event.preventDefault(); this.addSkill(); }
  }

  onAvatarSelected(event: Event): void {
    this.error = '';
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) { this.error = 'Avatar : format invalide.'; return; }
    if (file.size > 5 * 1024 * 1024) { this.error = 'Avatar : fichier trop volumineux (max 5MB).'; return; }
    this.selectedAvatarFile = file;
    const reader = new FileReader();
    reader.onload = () => {
      const preview = String(reader.result);
      this.draftProfile.avatar = preview;
      this.draftProfile.photo  = preview;
    };
    reader.readAsDataURL(file);
  }

  trackBySkill(index: number, skill: string): string { return `${index}-${skill}`; }
}