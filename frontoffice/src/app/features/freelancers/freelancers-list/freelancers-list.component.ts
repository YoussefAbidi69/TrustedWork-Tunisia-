import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { ApiService } from '../../../core/services/api.service';
import { FreelancerProfile } from '../../../core/models/freelancer.model';

/**
 * Profil freelancer enrichi avec le nom résolu depuis user-service
 * /identity/users/{userId} → PublicUserDTO { firstName, lastName }
 */
interface FreelancerViewModel extends FreelancerProfile {
  fullName: string;
  initials: string;
  photo: string;
}

/**
 * Liste des profils publics — permet de naviguer vers le profil public
 * d'un freelancer pour l'endorser ou lui laisser un avis
 * Intégration inter-services : freelancer-profile-service + user-service
 */
@Component({
  selector: 'app-freelancers-list',
  templateUrl: './freelancers-list.component.html',
  styleUrls: ['./freelancers-list.component.css']
})
export class FreelancersListComponent implements OnInit {

  freelancers: FreelancerViewModel[] = [];
  isLoading = false;
  errorMessage = '';
  currentTime = '';

private readonly clockInterval: any;

  selectedRegion = '';
  selectedStatus: 'AVAILABLE' | 'BUSY' | 'ON_VACATION' | '' = '';
  minRate: number | null = null;
  maxRate: number | null = null;

  availableRegions: string[] = [];

  constructor(
    private readonly profileService: FreelancerProfileService,
    private readonly authService: AuthService,
    private readonly api: ApiService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadFreelancers();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  /**
   * Charge les profils publics filtrés via le backend
   * puis résout le nom de chaque freelancer via /identity/users/{userId}
   */
  loadFreelancers(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.profileService.searchProfiles({
      region: this.selectedRegion || undefined,
      availability: this.selectedStatus || undefined,
      minRate: this.minRate,
      maxRate: this.maxRate
    }).subscribe({
      next: (data) => {
        // Exclure son propre profil de la liste
        const others = data.filter(f => f.userId !== this.currentUserId);

        // Alimenter la liste des régions depuis le résultat backend
        this.availableRegions = [
          ...new Set(
            data
              .map(f => f.region)
              .filter((region): region is string => !!region && region.trim().length > 0)
          )
        ].sort((a, b) => a.localeCompare(b));

        if (others.length === 0) {
          this.freelancers = [];
          this.isLoading = false;
          return;
        }

        // Résoudre le nom de chaque freelancer en parallèle
        const userRequests = others.map(f =>
          this.api.get<any>(`/identity/users/${f.userId}`).pipe(
            catchError(() => of({ firstName: '', lastName: '' }))
          )
        );

        forkJoin(userRequests).subscribe({
          next: (users) => {
            this.freelancers = others.map((f, index) => {
              const user = users[index] || {};
              const firstName = (user.firstName || '').trim();
              const lastName  = (user.lastName  || '').trim();
              const fullName  = `${firstName} ${lastName}`.trim();

              return {
                ...f,
                fullName: fullName || f.headline || 'Freelancer',
                initials: this.buildInitials(firstName, lastName, f.headline),
                photo:    (user.photo as string) || ''
              };
            });

            this.isLoading = false;
          },
          error: () => {
            // Fail-open — afficher avec headline comme fallback
            this.freelancers = others.map(f => ({
              ...f,
              fullName: f.headline || 'Freelancer',
              initials: f.headline?.charAt(0)?.toUpperCase() || 'F',
              photo:    ''
            }));
            this.isLoading = false;
          }
        });
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les freelancers.';
        this.isLoading = false;
      }
    });
  }

  applyFilters(): void {
    this.loadFreelancers();
  }

  resetFilters(): void {
    this.selectedRegion = '';
    this.selectedStatus = '';
    this.minRate = null;
    this.maxRate = null;
    this.loadFreelancers();
  }

  buildInitials(firstName: string, lastName: string, fallback?: string): string {
    const f = (firstName || '').trim();
    const l = (lastName || '').trim();

    if (f && l) return `${f.charAt(0)}${l.charAt(0)}`.toUpperCase();
    if (f) return f.charAt(0).toUpperCase();

    return fallback?.charAt(0)?.toUpperCase() || 'F';
  }

  viewProfile(userId: number): void {
    this.router.navigate(['/app/profile/public', userId]);
  }

  updateClock(): void {
  this.currentTime = new Date().toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
}}