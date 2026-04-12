import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { Certification } from '../../../core/models/freelancer.model';

/**
 * Composant Certifications — diplômes et certifications externes du freelancer
 * Types supportés : EXTERNAL (AWS, PMP, etc.) et ACADEMIC (Licence, Master, etc.)
 * Les certifications internes plateforme = Module 04, hors scope de ce composant
 */
@Component({
  selector: 'app-certifications',
  templateUrl: './certifications.component.html',
  styleUrls: ['./certifications.component.css']
})
export class CertificationsComponent implements OnInit {

  certifications: Certification[] = [];
  isLoading = false;
  errorMessage = '';
  showForm = false;

  // Formulaire d'ajout
  newCert: {
    title: string;
    issuer: string;
    type: 'EXTERNAL' | 'ACADEMIC';
    issueDate: string;
    expiryDate: string;
    certificateUrl: string;
  } = {
    title: '',
    issuer: '',
    type: 'EXTERNAL',
    issueDate: '',
    expiryDate: '',
    certificateUrl: ''
  };

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

  loadCertifications(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.profileService.getMyCertifications(this.currentUserId).subscribe({
      next: (data) => {
        this.certifications = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les certifications.';
        this.isLoading = false;
      }
    });
  }

  addCertification(): void {
    if (!this.newCert.title.trim() || !this.newCert.issuer.trim()) return;

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
        this.newCert = {
          title: '',
          issuer: '',
          type: 'EXTERNAL',
          issueDate: '',
          expiryDate: '',
          certificateUrl: ''
        };
        this.showForm = false;
        this.loadCertifications();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de l\'ajout de la certification.';
      }
    });
  }

  deleteCertification(certId: number): void {
    if (!confirm('Supprimer cette certification ?')) return;
    this.profileService.deleteCertification(certId, this.currentUserId).subscribe({
      next: () => this.loadCertifications(),
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression.';
      }
    });
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'EXTERNAL': 'Externe',
      'ACADEMIC': 'Académique'
    };
    return labels[type] ?? type;
  }

  getTypeClass(type: string): string {
    const classes: Record<string, string> = {
      'EXTERNAL': 'type-external',
      'ACADEMIC': 'type-academic'
    };
    return classes[type] ?? '';
  }

  // Compter les certifications valides (non expirées)
  get validCount(): number {
    return this.certifications.filter(c => !c.isExpired).length;
  }

  // Compter les certifications expirées
  get expiredCount(): number {
    return this.certifications.filter(c => c.isExpired).length;
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    this.errorMessage = '';
  }
}