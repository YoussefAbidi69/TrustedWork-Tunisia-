import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { Certification } from '../../../core/models/freelancer.model';

@Component({
  selector: 'app-certifications',
  templateUrl: './certifications.component.html',
  styleUrls: ['./certifications.component.css']
})
export class CertificationsComponent implements OnInit {

  certifications: Certification[] = [];

  isLoading = false;
  isSaving = false;
  deletingCertId: number | null = null;

  editingCertId: number | null = null;

  errorMessage = '';
  successMessage = '';
  showForm = false;

  newCert: {
    title: string;
    issuer: string;
    type: 'EXTERNAL' | 'ACADEMIC';
    issueDate: string;
    expiryDate: string;
    certificateUrl: string;
  } = this.getEmptyCert();

  editCert: {
    title: string;
    issuer: string;
    type: 'EXTERNAL' | 'ACADEMIC';
    issueDate: string;
    expiryDate: string;
    certificateUrl: string;
  } = this.getEmptyCert();

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCertifications();
  }

  private get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  private getEmptyCert(): {
    title: string;
    issuer: string;
    type: 'EXTERNAL' | 'ACADEMIC';
    issueDate: string;
    expiryDate: string;
    certificateUrl: string;
  } {
    return {
      title: '',
      issuer: '',
      type: 'EXTERNAL',
      issueDate: '',
      expiryDate: '',
      certificateUrl: ''
    };
  }

  get canSave(): boolean {
    return !!this.newCert.title.trim() &&
           !!this.newCert.issuer.trim() &&
           this.validateDates(this.newCert.issueDate, this.newCert.expiryDate) &&
           !this.isSaving;
  }

  get validCount(): number {
    return this.certifications.filter(c => !c.isExpired && !this.isExpiringSoon(c.expiryDate as any)).length;
  }

  get expiredCount(): number {
    return this.certifications.filter(c => c.isExpired).length;
  }

  get academicCount(): number {
    return this.certifications.filter(c => c.type === 'ACADEMIC').length;
  }

  get externalCount(): number {
    return this.certifications.filter(c => c.type === 'EXTERNAL').length;
  }

  loadCertifications(): void {
    this.isLoading = true;
    this.clearMessages();

    this.profileService.getMyCertifications(this.currentUserId).subscribe({
      next: (data) => {
        this.certifications = data || [];
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les certifications.';
        this.isLoading = false;
        this.autoClearMessages();
      }
    });
  }

  openForm(): void {
    this.clearMessages();
    this.showForm = true;
    this.editingCertId = null;
    this.resetForm();
  }

  closeForm(): void {
    this.showForm = false;
    this.resetForm();
  }

  validateDates(issue: string | undefined, expiry: string | undefined): boolean {
    if (!issue || !expiry) {
      return true;
    }
    return new Date(expiry) >= new Date(issue);
  }

  isDuplicate(cert: {
    title: string;
    issuer: string;
  }, excludeId?: number): boolean {
    const title = cert.title.trim().toLowerCase();
    const issuer = cert.issuer.trim().toLowerCase();

    return this.certifications.some(c =>
      c.id !== excludeId &&
      (c.title || '').trim().toLowerCase() === title &&
      (c.issuer || '').trim().toLowerCase() === issuer
    );
  }

  addCertification(): void {
    if (!this.canSave) {
      return;
    }

    if (this.isDuplicate(this.newCert)) {
      this.errorMessage = 'Une certification avec le même titre et le même organisme existe déjà.';
      this.autoClearMessages();
      return;
    }

    this.clearMessages();
    this.isSaving = true;

    const payload: Partial<Certification> = {
      title: this.newCert.title.trim(),
      issuer: this.newCert.issuer.trim(),
      type: this.newCert.type,
      issueDate: this.newCert.issueDate || undefined,
      expiryDate: this.newCert.expiryDate || undefined,
      certificateUrl: this.newCert.certificateUrl.trim() || undefined
    };

    this.profileService.addCertification(this.currentUserId, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.successMessage = 'Certification ajoutée avec succès.';
        this.showForm = false;
        this.resetForm();
        this.loadCertifications();
        this.autoClearMessages();
      },
      error: (err) => {
        console.error(err);
        this.isSaving = false;
        this.errorMessage = err?.error?.message || 'Erreur lors de l\'ajout de la certification.';
        this.autoClearMessages();
      }
    });
  }

  startEdit(cert: Certification): void {
    this.clearMessages();
    this.showForm = false;
    this.editingCertId = cert.id;

    this.editCert = {
      title: cert.title || '',
      issuer: cert.issuer || '',
      type: (cert.type as 'EXTERNAL' | 'ACADEMIC') || 'EXTERNAL',
      issueDate: cert.issueDate ? this.toDateInputValue(cert.issueDate) : '',
      expiryDate: cert.expiryDate ? this.toDateInputValue(cert.expiryDate) : '',
      certificateUrl: cert.certificateUrl || ''
    };
  }

  cancelEdit(): void {
    this.editingCertId = null;
    this.editCert = this.getEmptyCert();
  }

  saveEdit(certId: number): void {
    if (!this.editCert.title.trim() || !this.editCert.issuer.trim()) {
      this.errorMessage = 'Le titre et l\'organisme émetteur sont obligatoires.';
      this.autoClearMessages();
      return;
    }

    if (!this.validateDates(this.editCert.issueDate, this.editCert.expiryDate)) {
      this.errorMessage = 'La date d\'expiration doit être postérieure ou égale à la date d\'obtention.';
      this.autoClearMessages();
      return;
    }

    if (this.isDuplicate(this.editCert, certId)) {
      this.errorMessage = 'Une autre certification avec le même titre et le même organisme existe déjà.';
      this.autoClearMessages();
      return;
    }

    this.clearMessages();
    this.isSaving = true;

    const payload: Partial<Certification> = {
      title: this.editCert.title.trim(),
      issuer: this.editCert.issuer.trim(),
      type: this.editCert.type,
      issueDate: this.editCert.issueDate || undefined,
      expiryDate: this.editCert.expiryDate || undefined,
      certificateUrl: this.editCert.certificateUrl.trim() || undefined
    };

    this.profileService.updateCertification(certId, this.currentUserId, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.successMessage = 'Certification modifiée avec succès.';
        this.editingCertId = null;
        this.editCert = this.getEmptyCert();
        this.loadCertifications();
        this.autoClearMessages();
      },
      error: (err) => {
        console.error(err);
        this.isSaving = false;
        this.errorMessage = err?.error?.message || 'Erreur lors de la modification.';
        this.autoClearMessages();
      }
    });
  }

  deleteCertification(certId: number): void {
    if (!confirm('Supprimer cette certification ?')) {
      return;
    }

    this.clearMessages();
    this.deletingCertId = certId;

    this.profileService.deleteCertification(certId, this.currentUserId).subscribe({
      next: () => {
        this.deletingCertId = null;
        this.successMessage = 'Certification supprimée avec succès.';
        this.loadCertifications();
        this.autoClearMessages();
      },
      error: (err) => {
        console.error(err);
        this.deletingCertId = null;
        this.errorMessage = err?.error?.message || 'Erreur lors de la suppression.';
        this.autoClearMessages();
      }
    });
  }

  resetForm(): void {
    this.newCert = this.getEmptyCert();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  autoClearMessages(): void {
    setTimeout(() => {
      this.errorMessage = '';
      this.successMessage = '';
    }, 3500);
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      EXTERNAL: 'Externe',
      ACADEMIC: 'Académique'
    };
    return labels[type] ?? type;
  }

  getTypeClass(type: string): string {
    const classes: Record<string, string> = {
      EXTERNAL: 'type-external',
      ACADEMIC: 'type-academic'
    };
    return classes[type] ?? '';
  }

  isExpiringSoon(date: string | Date | undefined): boolean {
    if (!date) {
      return false;
    }

    const expiryDate = new Date(date);
    const today = new Date();

    expiryDate.setHours(0, 0, 0, 0);
    today.setHours(0, 0, 0, 0);

    const diffMs = expiryDate.getTime() - today.getTime();
    const diffDays = diffMs / (1000 * 60 * 60 * 24);

    return diffDays > 0 && diffDays <= 30;
  }

  trackByCertification(index: number, cert: Certification): number {
    return cert.id;
  }

  private toDateInputValue(date: string | Date): string {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = `${d.getMonth() + 1}`.padStart(2, '0');
    const day = `${d.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}