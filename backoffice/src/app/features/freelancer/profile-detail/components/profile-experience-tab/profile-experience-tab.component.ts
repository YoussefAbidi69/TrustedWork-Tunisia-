import { Component, Input } from '@angular/core';
import {
  Certification,
  WorkExperience,
  Education
} from '../../../../../core/models/freelancer.model';

@Component({
  selector: 'app-profile-experience-tab',
  templateUrl: './profile-experience-tab.component.html',
  styleUrls: ['./profile-experience-tab.component.css']
})
export class ProfileExperienceTabComponent {
  @Input() certifications: Certification[] = [];
  @Input() workExperiences: WorkExperience[] = [];
  @Input() educations: Education[] = [];

  isCertExpiringSoon(expiryDate: string | undefined, isExpired?: boolean): boolean {
    if (!expiryDate || isExpired) return false;

    const today = new Date();
    const expiry = new Date(expiryDate);

    today.setHours(0, 0, 0, 0);
    expiry.setHours(0, 0, 0, 0);

    const diffMs = expiry.getTime() - today.getTime();
    const diffDays = diffMs / (1000 * 60 * 60 * 24);

    return diffDays > 0 && diffDays <= 30;
  }

  getCertStatusClass(cert: Certification): string {
    if (cert.isExpired) return 'badge-danger';
    if (this.isCertExpiringSoon(cert.expiryDate, cert.isExpired)) return 'badge-warning';
    return 'badge-success';
  }

  getCertStatusLabel(cert: Certification): string {
    if (cert.isExpired) return 'Expirée';
    if (this.isCertExpiringSoon(cert.expiryDate, cert.isExpired)) return 'Expire bientôt';
    return 'Valide';
  }

  getWorkDurationLabel(work: WorkExperience): string {
    if (work.durationLabel) return work.durationLabel;

    const start = work.startDate ? new Date(work.startDate) : null;
    const end = work.isCurrent ? new Date() : (work.endDate ? new Date(work.endDate) : null);

    if (!start || !end || Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      return '';
    }

    const totalMonths =
      (end.getFullYear() - start.getFullYear()) * 12 +
      (end.getMonth() - start.getMonth());

    if (totalMonths <= 0) return 'Moins d’un mois';
    if (totalMonths < 12) return `${totalMonths} mois`;

    const years = Math.floor(totalMonths / 12);
    const months = totalMonths % 12;

    if (months > 0) {
      return `${years} ${years === 1 ? 'an' : 'ans'} ${months} mois`;
    }

    return `${years} ${years === 1 ? 'an' : 'ans'}`;
  }
}