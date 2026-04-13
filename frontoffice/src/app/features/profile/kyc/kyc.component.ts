import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { ApiService } from '../../../core/services/api.service';

interface KycStat {
  label: string;
  value: string;
  tone?: 'default' | 'accent' | 'success' | 'warning';
}

interface VerificationStep {
  id: string;
  title: string;
  description: string;
  status: 'Completed' | 'In Review' | 'Pending';
  progress: number;
}

interface RequiredDocument {
  id: string;
  title: string;
  description: string;
  fileHint: string;
  status: 'Uploaded' | 'Missing' | 'Under Review';
}

interface ComplianceItem {
  label: string;
  description: string;
  checked: boolean;
}

@Component({
  selector: 'app-kyc',
  templateUrl: './kyc.component.html',
  styleUrls: ['./kyc.component.css']
})
export class KycComponent implements OnInit {
  loading = true;
  submitting = false;
  error = '';
  successMessage = '';

  kycStatus = 'PENDING';
  livenessPassed = false;
  trustLevel = 1;
  userId: number | null = null;
  userCin: number | null = null;

  showSubmitForm = false;

  cinDocumentPath = '';
  selfiePath = '';
  diplomaDocumentPath = '';

  cinDocumentPreview = '';
  selfiePreview = '';
  diplomaDocumentPreview = '';

  cinDocumentFile: File | null = null;
  selfieFile: File | null = null;
  diplomaFile: File | null = null;

  kycHistory: any[] = [];

  readonly complianceChecklist: ComplianceItem[] = [
    {
      label: 'Identité correspond au profil',
      description: 'Votre nom légal doit correspondre à votre CIN.',
      checked: true
    },
    {
      label: 'Documents clairs et lisibles',
      description: 'Les fichiers flous peuvent retarder la validation.',
      checked: true
    },
    {
      label: 'Selfie récent et de bonne qualité',
      description: 'Utilisé pour la liveness detection.',
      checked: true
    },
    {
      label: 'Diplôme ou preuve professionnelle',
      description: 'Renforce le score de confiance du profil.',
      checked: false
    }
  ];

  readonly trustBenefits = [
    'Débloquer des marqueurs de confiance premium',
    'Accéder aux missions à haute valeur',
    'Renforcer la crédibilité des paiements escrow',
    'Augmenter l’autorité du profil public'
  ];

  constructor(
    private userService: UserService,
    private api: ApiService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;

    this.userService.getMyProfile().pipe(
      finalize(() => (this.loading = false))
    ).subscribe({
      next: (data: UserProfileResponse) => {
        this.kycStatus = data.kycStatus || 'PENDING';
        this.livenessPassed = (data as any).livenessPassed || false;
        this.trustLevel = (data as any).trustLevel ?? 1;
        this.userId = (data as any).id ?? null;
        this.userCin = (data.cin as number) ?? null;

        console.log('[KYC] Profil chargé :', data);
        console.log('[KYC] userId =', this.userId);
        console.log('[KYC] userCin =', this.userCin);

        if (this.userId) {
          this.loadKycHistory(this.userId);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error = 'Impossible de charger les données KYC.';
        console.error('[KYC] Erreur chargement profil :', err);
      }
    });
  }

  loadKycHistory(userId: number): void {
    this.api.get<any[]>(`/kyc/requests/history/${userId}`).subscribe({
      next: (history) => {
        this.kycHistory = history || [];
        console.log('[KYC] Historique KYC :', this.kycHistory);
      },
      error: (err: HttpErrorResponse) => {
        console.error('[KYC] Erreur historique KYC :', err);
      }
    });
  }

  private resetMessages(): void {
    this.error = '';
    this.successMessage = '';
  }

  private isAllowedType(file: File, allowed: string[]): boolean {
    return allowed.includes(file.type);
  }

  private isImage(file: File): boolean {
    return file.type.startsWith('image/');
  }

  private validateFile(
    file: File,
    allowedTypes: string[],
    maxMb: number,
    label: string
  ): boolean {
    if (!this.isAllowedType(file, allowedTypes)) {
      this.error = `${label} : format invalide.`;
      return false;
    }

    if (file.size > maxMb * 1024 * 1024) {
      this.error = `${label} : fichier trop volumineux (max ${maxMb}MB).`;
      return false;
    }

    return true;
  }

  private readPreview(file: File, callback: (value: string) => void): void {
    if (!this.isImage(file)) {
      callback('');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => callback(String(reader.result));
    reader.readAsDataURL(file);
  }

  onCinDocumentSelected(event: Event): void {
    this.resetMessages();

    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const allowed = ['image/jpeg', 'image/png', 'application/pdf'];
    if (!this.validateFile(file, allowed, 10, 'Document CIN')) return;

    this.cinDocumentFile = file;
    this.cinDocumentPath = file.name;

    this.readPreview(file, (preview) => {
      this.cinDocumentPreview = preview;
    });
  }

  onSelfieSelected(event: Event): void {
    this.resetMessages();

    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!this.validateFile(file, allowed, 5, 'Selfie')) return;

    this.selfieFile = file;
    this.selfiePath = file.name;

    this.readPreview(file, (preview) => {
      this.selfiePreview = preview;
    });
  }

  onDiplomaSelected(event: Event): void {
    this.resetMessages();

    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const allowed = ['image/jpeg', 'image/png', 'application/pdf'];
    if (!this.validateFile(file, allowed, 10, 'Diplôme')) return;

    this.diplomaFile = file;
    this.diplomaDocumentPath = file.name;

    this.readPreview(file, (preview) => {
      this.diplomaDocumentPreview = preview;
    });
  }

  removeCinDocument(): void {
    this.cinDocumentFile = null;
    this.cinDocumentPath = '';
    this.cinDocumentPreview = '';
  }

  removeSelfie(): void {
    this.selfieFile = null;
    this.selfiePath = '';
    this.selfiePreview = '';
  }

  removeDiploma(): void {
    this.diplomaFile = null;
    this.diplomaDocumentPath = '';
    this.diplomaDocumentPreview = '';
  }

  get stats(): KycStat[] {
    const kycScore =
      this.kycStatus === 'APPROVED'
        ? 100
        : this.kycStatus === 'IN_REVIEW'
        ? 60
        : this.kycStatus === 'REJECTED'
        ? 0
        : 20;

    return [
      {
        label: 'Statut KYC',
        value: this.kycStatusLabel,
        tone: this.kycStatus === 'APPROVED' ? 'success' : 'warning'
      },
      {
        label: 'Score KYC',
        value: `${kycScore}%`,
        tone: 'accent'
      },
      {
        label: 'Liveness Detection',
        value: this.livenessPassed ? 'Validé' : 'Non validé',
        tone: this.livenessPassed ? 'success' : 'warning'
      },
      {
        label: 'Trust Level',
        value: `${this.trustLevel}/5`,
        tone: this.trustLevel >= 3 ? 'success' : 'default'
      }
    ];
  }

  get kycStatusLabel(): string {
    const map: Record<string, string> = {
      APPROVED: 'Approuvé',
      IN_REVIEW: 'En cours',
      REJECTED: 'Rejeté',
      PENDING: 'En attente'
    };
    return map[this.kycStatus] || this.kycStatus;
  }

  get steps(): VerificationStep[] {
    const kycApproved = this.kycStatus === 'APPROVED';
    const kycInReview = this.kycStatus === 'IN_REVIEW';

    return [
      {
        id: 'step-1',
        title: 'Soumission du dossier KYC',
        description: 'CIN, selfie et documents soumis pour vérification.',
        status: kycInReview || kycApproved ? 'Completed' : 'Pending',
        progress: kycInReview || kycApproved ? 100 : 0
      },
      {
        id: 'step-2',
        title: 'Liveness Detection',
        description: 'Correspondance selfie / photo CIN vérifiée.',
        status: this.livenessPassed
          ? 'Completed'
          : kycInReview
          ? 'In Review'
          : 'Pending',
        progress: this.livenessPassed ? 100 : kycInReview ? 60 : 0
      },
      {
        id: 'step-3',
        title: 'Validation admin',
        description: 'Examen du dossier par l’équipe TrustedWork.',
        status: kycApproved ? 'Completed' : kycInReview ? 'In Review' : 'Pending',
        progress: kycApproved ? 100 : kycInReview ? 50 : 0
      },
      {
        id: 'step-4',
        title: 'Trust Passport activé',
        description: 'Activation du niveau premium après validation complète.',
        status: this.trustLevel >= 3 ? 'Completed' : 'Pending',
        progress: this.trustLevel >= 3 ? 100 : 0
      }
    ];
  }

  get documents(): RequiredDocument[] {
    const lastKyc = this.kycHistory[0];

    return [
      {
        id: 'doc-1',
        title: 'Document CIN',
        description: 'Photo recto/verso de la carte d’identité nationale.',
        fileHint: 'JPG, PNG ou PDF • Max 10MB',
        status: lastKyc?.cinDocumentPath ? 'Uploaded' : 'Missing'
      },
      {
        id: 'doc-2',
        title: 'Selfie (Liveness)',
        description: 'Photo récente pour la détection de vivacité.',
        fileHint: 'JPG, PNG ou WEBP • Visage visible',
        status: lastKyc?.selfiePath
          ? this.livenessPassed
            ? 'Uploaded'
            : 'Under Review'
          : 'Missing'
      },
      {
        id: 'doc-3',
        title: 'Diplôme / Preuve professionnelle',
        description: 'Certificat ou preuve d’activité professionnelle.',
        fileHint: 'Optionnel mais recommandé',
        status: lastKyc?.diplomaDocumentPath ? 'Uploaded' : 'Missing'
      }
    ];
  }

  toggleSubmitForm(): void {
    this.showSubmitForm = !this.showSubmitForm;
    this.resetMessages();

    if (!this.showSubmitForm) {
      this.cinDocumentPath = '';
      this.selfiePath = '';
      this.diplomaDocumentPath = '';
      this.cinDocumentPreview = '';
      this.selfiePreview = '';
      this.diplomaDocumentPreview = '';
      this.cinDocumentFile = null;
      this.selfieFile = null;
      this.diplomaFile = null;
    }
  }

  submitKyc(): void {
    this.resetMessages();

    if (!this.userId) {
      this.error = 'Utilisateur non identifié.';
      return;
    }

    if (!this.userCin) {
      this.error = 'CIN utilisateur introuvable.';
      return;
    }

    if (!this.cinDocumentFile || !this.selfieFile) {
      this.error = 'Le document CIN et le selfie sont obligatoires.';
      return;
    }

    this.submitting = true;

    const formData = new FormData();
    formData.append('cinNumber', String(this.userCin));
    formData.append('cinDocument', this.cinDocumentFile);
    formData.append('selfie', this.selfieFile);

    if (this.diplomaFile) {
      formData.append('diploma', this.diplomaFile);
    }

    formData.forEach((value, key) => {
      console.log('[KYC] FormData =>', key, value);
    });

    this.api.post(`/kyc/requests/submit/${this.userId}`, formData)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Dossier KYC soumis avec succès.';
          this.showSubmitForm = false;
          this.removeCinDocument();
          this.removeSelfie();
          this.removeDiploma();
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          console.error('[KYC] Full error:', err);
          console.error('[KYC] Backend body:', err.error);

          this.error =
            err?.error?.error ||
            err?.error?.message ||
            err?.error?.details ||
            'Erreur lors de la soumission du KYC.';
        }
      });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      Completed: 'status-badge status-badge--success',
      Uploaded: 'status-badge status-badge--success',
      'In Review': 'status-badge status-badge--warning',
      'Under Review': 'status-badge status-badge--warning',
      Pending: 'status-badge status-badge--neutral',
      Missing: 'status-badge status-badge--neutral',
      APPROVED: 'status-badge status-badge--success',
      IN_REVIEW: 'status-badge status-badge--warning',
      REJECTED: 'status-badge status-badge--danger',
      PENDING: 'status-badge status-badge--neutral'
    };

    return map[status] || 'status-badge';
  }

  getStatToneClass(tone?: KycStat['tone']): string {
    const map: Record<string, string> = {
      accent: 'stat-value stat-value--accent',
      success: 'stat-value stat-value--success',
      warning: 'stat-value stat-value--warning'
    };

    return map[tone || ''] || 'stat-value';
  }

 submitDiplomaOnly(): void {
  this.resetMessages();

  if (!this.userId) {
    this.error = 'Utilisateur non identifié.';
    return;
  }

  if (!this.userCin) {
    this.error = 'CIN utilisateur introuvable.';
    return;
  }

  if (!this.diplomaFile) {
    this.error = 'Veuillez sélectionner un diplôme.';
    return;
  }

  this.submitting = true;

  const formData = new FormData();

  // ⚠️ IMPORTANT → backend exige cinNumber même pour diplôme seul
  formData.append('cinNumber', String(this.userCin));

  // ✅ on envoie UNIQUEMENT le diplôme
  formData.append('diploma', this.diplomaFile);

  formData.forEach((value, key) => {
    console.log('[DIPLOMA] FormData =>', key, value);
  });

  this.api.post(`/kyc/requests/submit/${this.userId}`, formData)
    .pipe(finalize(() => (this.submitting = false)))
    .subscribe({
      next: () => {
        this.successMessage = 'Diplôme envoyé avec succès ✅';

        this.removeDiploma();
        this.showSubmitForm = false;

        this.loadData();
      },
      error: (err: HttpErrorResponse) => {
        console.error('[DIPLOMA] Full error:', err);
        console.error('[DIPLOMA] Backend body:', err.error);

        this.error =
          err?.error?.error ||
          err?.error?.message ||
          err?.error?.details ||
          'Erreur lors de l’envoi du diplôme.';
      }
    });
}
}