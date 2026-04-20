import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { forkJoin, catchError, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ApiService } from '../../../core/services/api.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { Skill, CompletenessResponse, FreelancerProfile } from '../../../core/models/freelancer.model';
import { UserService } from '../../../core/services/user.service';

const API_BASE = 'http://localhost:8081/api';

interface ProfileModel {
  fullName:  string;
  firstName: string;
  lastName:  string;
  headline:  string;
  location:  string;
  bio:       string;
  phone:     string;
  photo:     string;
  avatar:    string;
  cin:       number | null;
  trustLevel:             number;
  kycStatus:              string;
  twoFactorEnabled:       boolean;
  portfolioAttached:      boolean;
  certificationsAdded:    boolean;
  trustPassportCompleted: boolean;
}

interface FreelancerDraft {
  headline:           string;
  bio:                string;
  hourlyRate:         number | null;
  region:             string;
  availabilityStatus: 'AVAILABLE' | 'BUSY' | 'ON_VACATION';
  visibility:         'PUBLIC' | 'PRIVATE' | 'CONNECTIONS_ONLY';
  projectType:        'SHORT_TERM' | 'LONG_TERM' | 'BOTH';
}

@Component({
  selector: 'app-profile-overview',
  templateUrl: './profile-overview.component.html',
  styleUrls: ['./profile-overview.component.css']
})
export class ProfileOverviewComponent implements OnInit {

  loading  = true;
  saving   = false;
  error    = '';
  successMessage = '';
  editMode = false;
  selectedAvatarFile: File | null = null;

  private userId: number | null = null;
  private userCin: number | null = null;

  skills:            Skill[]               = [];
  completeness:      CompletenessResponse  | null = null;
  freelancerProfile: FreelancerProfile     | null = null;

  freelancerDraft: FreelancerDraft = {
    headline: '', bio: '', hourlyRate: null, region: '',
    availabilityStatus: 'AVAILABLE', visibility: 'PUBLIC', projectType: 'BOTH'
  };

  regions = [
    'Tunis', 'Sfax', 'Sousse', 'Kairouan', 'Bizerte',
    'Gabès', 'Ariana', 'Gafsa', 'Monastir', 'Ben Arous',
    'Kasserine', 'Médenine', 'Nabeul', 'Tataouine', 'Béja',
    'Jendouba', 'Mahdia', 'Sidi Bouzid', 'Siliana', 'Kébili',
    'Le Kef', 'Manouba', 'Zaghouan', 'Tozeur'
  ];

  profile: ProfileModel = {
    fullName: '', firstName: '', lastName: '', headline: '',
    location: 'Tunisie', bio: '', phone: '', photo: '', avatar: '',
    cin: null, trustLevel: 1, kycStatus: 'PENDING',
    twoFactorEnabled: false, portfolioAttached: false,
    certificationsAdded: false, trustPassportCompleted: false
  };

  draftProfile: ProfileModel = { ...this.profile };

  constructor(
    private authService: AuthService,
    private api: ApiService,
    private freelancerService: FreelancerProfileService,
    private userService: UserService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  // ==================== AVATAR ====================

  hasRealPhoto(): boolean {
    const photo = this.editMode ? this.draftProfile.photo : this.profile.photo;
    return !!photo && !photo.includes('data:image/svg') && photo.trim() !== '';
  }

  getInitials(): string {
    const source = this.editMode ? this.draftProfile : this.profile;
    const f = (source.firstName || '').trim();
    const l = (source.lastName  || '').trim();
    if (f && l) return `${f.charAt(0)}${l.charAt(0)}`.toUpperCase();
    if (f)      return f.charAt(0).toUpperCase();
    const full = (source.fullName || '').trim().split(' ').filter(Boolean);
    if (full.length >= 2) return `${full[0][0]}${full[1][0]}`.toUpperCase();
    if (full.length === 1) return full[0][0].toUpperCase();
    return 'F';
  }

  // ==================== LOAD ====================

  private resolvePhotoUrl(photo?: string): string {
    if (!photo) return '';
    if (photo.startsWith('http://') || photo.startsWith('https://') || photo.startsWith('data:')) {
      return photo;
    }
    return `${API_BASE}${photo}`;
  }

  /**
   * Chargement du profil :
   * 1. Données de base depuis le token JWT (immédiat, sans appel API)
   * 2. TrustLevel via endpoint public /users/{id}/trust-level (permitAll)
   * 3. Données freelancer depuis freelancer-profile-service (port 8082)
   *
   * ✅ Ne plus appeler /users/me (403 sans config user-service)
   */
  loadProfile(): void {
    this.loading = true;
    this.error   = '';
    this.successMessage = '';

    const authUser = this.authService.getCurrentAuthUser();

    if (!authUser?.userId) {
      this.loading = false;
      this.error   = 'Session expirée. Veuillez vous reconnecter.';
      return;
    }

    this.userId = authUser.userId;

    // Initialisation depuis le token JWT
    this.profile = {
      ...this.profile,
      fullName:  authUser.email,
      firstName: authUser.email.split('@')[0],
      lastName:  '',
      cin:       null,
      trustLevel: 1
    };

    this.draftProfile = { ...this.profile };

    // Charger user-service (/users/me) + trustLevel + données freelancer en parallèle
    forkJoin({
      userMe:       this.userService.getMyProfile().pipe(catchError(() => of(null))),
      trustData:    this.freelancerService.getUserTrustLevel(authUser.userId).pipe(catchError(() => of({ trustLevel: 1 }))),
      fpProfile:    this.freelancerService.getProfileByUserId(authUser.userId).pipe(catchError(() => of(null))),
      skills:       this.freelancerService.getMySkills(authUser.userId).pipe(catchError(() => of([]))),
      completeness: this.freelancerService.getCompleteness(authUser.userId).pipe(catchError(() => of(null)))
    })
    .pipe(finalize(() => this.loading = false))
    .subscribe({
      next: ({ userMe, trustData, fpProfile, skills, completeness }) => {
        // TrustLevel depuis endpoint public
        const trustLevel = Number((trustData as any)?.trustLevel ?? 1);

        // Données utilisateur réelles depuis /users/me
        if (userMe) {
          const photoUrl = this.resolvePhotoUrl((userMe as any).photo || '');
          this.userCin   = (userMe as any).cin ?? null;
          this.profile = {
            ...this.profile,
            firstName:          (userMe as any).firstName        || authUser.email.split('@')[0],
            lastName:           (userMe as any).lastName         || '',
            fullName:           `${(userMe as any).firstName || ''} ${(userMe as any).lastName || ''}`.trim() || authUser.email,
            phone:              (userMe as any).phone            || '',
            headline:           (userMe as any).headline         || '',
            location:           (userMe as any).location         || 'Tunisie',
            bio:                (userMe as any).bio              || '',
            photo:              photoUrl,
            avatar:             photoUrl,
            cin:                (userMe as any).cin              ?? null,
            kycStatus:          (userMe as any).kycStatus        || 'PENDING',
            twoFactorEnabled:   (userMe as any).twoFactorEnabled ?? false,
            trustLevel,
            trustPassportCompleted: trustLevel >= 3
          };
        } else {
          this.profile = { ...this.profile, trustLevel, trustPassportCompleted: trustLevel >= 3 };
        }

        this.draftProfile = { ...this.profile };

        // Données freelancer
        this.freelancerProfile = fpProfile;
        this.skills            = skills || [];
        this.completeness      = completeness;

        if (fpProfile) {
          this.freelancerDraft = {
            headline:           fpProfile.headline           || '',
            bio:                fpProfile.bio                || '',
            hourlyRate:         fpProfile.hourlyRate         || null,
            region:             fpProfile.region             || '',
            availabilityStatus: fpProfile.availabilityStatus || 'AVAILABLE',
            visibility:         fpProfile.visibility         || 'PUBLIC',
            projectType:        fpProfile.projectType        || 'BOTH'
          };

          // Synchroniser le headline depuis freelancer profile si disponible
          if (fpProfile.headline) {
            this.profile.headline      = fpProfile.headline;
            this.draftProfile.headline = fpProfile.headline;
          }
        }
      },
      error: () => {
        this.error = 'Impossible de charger les données du profil.';
      }
    });
  }

  // ==================== COMPUTED ====================

  get completion(): number {
    if (this.completeness) return this.completeness.score;
    const checks = [
      !!this.profile.fullName,
      !!this.profile.phone,
      this.profile.kycStatus === 'APPROVED',
      this.profile.twoFactorEnabled,
      this.profile.trustLevel >= 3,
      this.skills.length > 0
    ];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  }

  get completionItems(): { label: string; done: boolean }[] {
    return [
      { label: 'Informations de base', done: !!this.profile.fullName        },
      { label: 'Téléphone',            done: !!this.profile.phone           },
      { label: 'KYC approuvé',         done: this.profile.kycStatus === 'APPROVED' },
      { label: '2FA activé',           done: this.profile.twoFactorEnabled  },
      { label: 'Trust Level ≥ 3',      done: this.profile.trustLevel >= 3   },
      { label: 'Compétences ajoutées', done: this.skills.length > 0         }
    ];
  }

  get completionSuggestions(): string[] {
    return this.completeness?.suggestions ?? [];
  }

  get availabilityLabel(): string {
    switch (this.freelancerDraft.availabilityStatus) {
      case 'AVAILABLE': return 'Disponible';
      case 'BUSY':      return 'Occupé';
      default:          return 'En vacances';
    }
  }

  // ==================== ACTIONS ====================

  toggleEdit(): void {
    this.error = '';
    this.successMessage = '';
    if (!this.editMode) {
      this.draftProfile       = { ...this.profile };
      this.selectedAvatarFile = null;
      if (this.freelancerProfile) {
        this.freelancerDraft = {
          headline:           this.freelancerProfile.headline           || '',
          bio:                this.freelancerProfile.bio                || '',
          hourlyRate:         this.freelancerProfile.hourlyRate         || null,
          region:             this.freelancerProfile.region             || '',
          availabilityStatus: this.freelancerProfile.availabilityStatus || 'AVAILABLE',
          visibility:         this.freelancerProfile.visibility         || 'PUBLIC',
          projectType:        this.freelancerProfile.projectType        || 'BOTH'
        };
      }
    }
    this.editMode = !this.editMode;
  }

  cancelEdit(): void {
    this.draftProfile       = { ...this.profile };
    this.selectedAvatarFile = null;
    if (this.freelancerProfile) {
      this.freelancerDraft = {
        headline:           this.freelancerProfile.headline           || '',
        bio:                this.freelancerProfile.bio                || '',
        hourlyRate:         this.freelancerProfile.hourlyRate         || null,
        region:             this.freelancerProfile.region             || '',
        availabilityStatus: this.freelancerProfile.availabilityStatus || 'AVAILABLE',
        visibility:         this.freelancerProfile.visibility         || 'PUBLIC',
        projectType:        this.freelancerProfile.projectType        || 'BOTH'
      };
    }
    this.editMode = false;
    this.error    = '';
    this.successMessage = '';
  }

  saveProfile(): void {
    if (!this.userId) { this.error = 'User ID introuvable.'; return; }

    this.saving = true;
    this.error  = '';
    this.successMessage = '';

    const normalizedHeadline =
      this.freelancerDraft.headline?.trim() ||
      this.draftProfile.headline?.trim()    || '';

    // 1 — Mise à jour du profil freelancer (port 8082)
    const freelancerPayload: Partial<FreelancerProfile> = {
      headline:           normalizedHeadline,
      bio:                this.freelancerDraft.bio?.trim()  || '',
      hourlyRate:         this.freelancerDraft.hourlyRate   ?? undefined,
      region:             this.freelancerDraft.region       || '',
      availabilityStatus: this.freelancerDraft.availabilityStatus,
      visibility:         this.freelancerDraft.visibility,
      projectType:        this.freelancerDraft.projectType
    };

    // 2 — Mise à jour du profil utilisateur (user-service port 8081) avec la photo
    const saveUserProfile$ = this.userCin
      ? (() => {
          const formData = new FormData();
          formData.append('firstName', this.draftProfile.firstName?.trim() || '');
          formData.append('lastName',  this.draftProfile.lastName?.trim()  || '');
          if (this.draftProfile.phone)    formData.append('phone',    this.draftProfile.phone.trim());
          if (this.draftProfile.headline) formData.append('headline', normalizedHeadline);
          if (this.draftProfile.location) formData.append('location', this.draftProfile.location.trim());
          if (this.draftProfile.bio)      formData.append('bio',      this.draftProfile.bio.trim());
          if (this.selectedAvatarFile)    formData.append('photo',    this.selectedAvatarFile, this.selectedAvatarFile.name);

          return this.http.put<any>(
            `${API_BASE}/users/${this.userCin}`,
            formData
          ).pipe(catchError(() => of(null)));
        })()
      : of(null);

    forkJoin({
      freelancer: this.freelancerService.updateProfile(this.userId, freelancerPayload).pipe(catchError(() => of(null))),
      user:       saveUserProfile$
    })
    .pipe(finalize(() => this.saving = false))
    .subscribe({
      next: ({ user }) => {
        // Si la photo a été uploadée, mettre à jour l'avatar immédiatement
        if (user && (user as any).photo) {
          const newPhotoUrl = this.resolvePhotoUrl((user as any).photo);
          this.profile.photo  = newPhotoUrl;
          this.profile.avatar = newPhotoUrl;
        }
        this.successMessage     = 'Profil mis à jour avec succès.';
        this.editMode           = false;
        this.selectedAvatarFile = null;
        this.loadProfile();
      },
      error: (err: HttpErrorResponse) => {
        this.error =
          err?.error?.error   ||
          err?.error?.message ||
          'Erreur lors de la sauvegarde.';
      }
    });
  }

  onAvatarSelected(event: Event): void {
    this.error = '';
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) { this.error = 'Avatar : format invalide.';                        return; }
    if (file.size > 5 * 1024 * 1024)  { this.error = 'Avatar : fichier trop volumineux (max 5MB).';     return; }

    this.selectedAvatarFile = file;
    const reader = new FileReader();
    reader.onload = () => {
      this.draftProfile.avatar = String(reader.result);
      this.draftProfile.photo  = String(reader.result);
    };
    reader.readAsDataURL(file);
  }

  trackBySkill(index: number, skill: Skill): number { return skill.id; }

downloadCv(): void {
  if (!this.userId) {
    console.error('UserId manquant');
    this.error = 'Impossible d’exporter le CV : utilisateur introuvable.';
    return;
  }

  this.freelancerService.exportMyCv(this.userId);
}
}