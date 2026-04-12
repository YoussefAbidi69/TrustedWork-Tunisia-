import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { FreelancerProfile } from '../../../core/models/freelancer.model';

/**
 * Page de création du profil freelancer — premier login uniquement
 * Redirige vers dashboard après création réussie
 */
@Component({
  selector: 'app-create-profile',
  templateUrl: './create-profile.component.html',
  styleUrls: ['./create-profile.component.css']
})
export class CreateProfileComponent {

  isLoading = false;
  errorMessage = '';

  profile: {
    headline: string;
    bio: string;
    hourlyRate: number | null;
    region: string;
    availabilityStatus: 'AVAILABLE' | 'BUSY' | 'ON_VACATION';
    visibility: 'PUBLIC' | 'PRIVATE' | 'CONNECTIONS_ONLY';
    projectType: 'SHORT_TERM' | 'LONG_TERM' | 'BOTH';
  } = {
    headline: '',
    bio: '',
    hourlyRate: null,
    region: '',
    availabilityStatus: 'AVAILABLE',
    visibility: 'PUBLIC',
    projectType: 'BOTH'
  };

  regions = [
    'Tunis', 'Sfax', 'Sousse', 'Kairouan', 'Bizerte',
    'Gabès', 'Ariana', 'Gafsa', 'Monastir', 'Ben Arous',
    'Kasserine', 'Médenine', 'Nabeul', 'Tataouine', 'Béja',
    'Jendouba', 'Mahdia', 'Sidi Bouzid', 'Siliana', 'Kébili',
    'Le Kef', 'Manouba', 'Zaghouan', 'Tozeur'
  ];

  constructor(
    private freelancerService: FreelancerProfileService,
    private authService: AuthService,
    private router: Router
  ) {}

  private get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  onSubmit(): void {
    if (!this.profile.headline.trim()) {
      this.errorMessage = 'Le titre professionnel est obligatoire.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const payload: Partial<FreelancerProfile> = {
      userId: this.currentUserId,
      headline: this.profile.headline.trim(),
      bio: this.profile.bio.trim(),
      hourlyRate: this.profile.hourlyRate ?? undefined,
      availabilityStatus: this.profile.availabilityStatus,
      visibility: this.profile.visibility,
      projectType: this.profile.projectType,
      region: this.profile.region
    };

    this.freelancerService.createProfile(payload).subscribe({
      next: () => {
        window.location.href = '/app/dashboard';
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Erreur lors de la création du profil. Réessayez.';
      }
    });
  }
}