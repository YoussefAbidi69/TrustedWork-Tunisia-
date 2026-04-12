import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { FreelancerProfile } from '../../../core/models/freelancer.model';

/**
 * Liste des profils publics — permet de naviguer vers le profil public
 * d'un freelancer pour l'endorser ou lui laisser un avis
 */
@Component({
  selector: 'app-freelancers-list',
  templateUrl: './freelancers-list.component.html',
  styleUrls: ['./freelancers-list.component.css']
})
export class FreelancersListComponent implements OnInit {

  freelancers: FreelancerProfile[] = [];
  isLoading = false;
  errorMessage = '';

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadFreelancers();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  loadFreelancers(): void {
    this.isLoading = true;
    this.profileService.getAllPublicProfiles().subscribe({
      next: (data) => {
        // Exclure son propre profil de la liste
        this.freelancers = data.filter(f => f.userId !== this.currentUserId);
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les freelancers.';
        this.isLoading = false;
      }
    });
  }

  viewProfile(userId: number): void {
    this.router.navigate(['/app/profile/public', userId]);
  }
}