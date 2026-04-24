import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService, UserDTO, SuspensionRecordDTO } from '../../../core/services/user.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { FreelancerProfile } from '../../../core/models/freelancer.model';

@Component({
  selector: 'app-user-detail',
  templateUrl: './user-detail.component.html',
  styleUrls: ['./user-detail.component.css']
})
export class UserDetailComponent implements OnInit {

  userId!: number;
  user: UserDTO | null = null;
  suspensions: SuspensionRecordDTO[] = [];
  freelancerProfile: FreelancerProfile | null = null;

  loading = true;
  errorMsg = '';
  successMsg = '';

  showSuspendForm = false;
  suspendReason = '';
  actionLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private profileService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    this.userId = parseInt(this.route.snapshot.paramMap.get('id') || '0', 10);

    if (!this.userId || Number.isNaN(this.userId)) {
      this.loading = false;
      this.errorMsg = 'Identifiant utilisateur invalide.';
      return;
    }

    this.loadUser();
  }

  loadUser(): void {
    this.loading = true;
    this.errorMsg = '';

    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.user = users.find(u => u.id === this.userId) || null;
        this.loading = false;

        if (!this.user) {
          this.errorMsg = 'Utilisateur introuvable.';
          return;
        }

        this.loadSuspensionHistory();
        this.loadFreelancerProfile();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Erreur lors du chargement de l’utilisateur';
        this.loading = false;
      }
    });
  }

  loadSuspensionHistory(): void {
    this.userService.getSuspensionHistory(this.userId).subscribe({
      next: (data) => {
        this.suspensions = data;
      },
      error: (err) => {
        console.error(err);
        this.suspensions = [];
      }
    });
  }

  loadFreelancerProfile(): void {
    const safeUserId = Math.trunc(this.userId);
    console.log('userId envoyé au backend pour profile freelancer:', safeUserId);

    this.profileService.getProfileByUserId(safeUserId).subscribe({
      next: (profile) => {
        this.freelancerProfile = profile;
      },
      error: (err) => {
        console.error('Erreur chargement profil freelancer:', err);
        this.freelancerProfile = null;
      }
    });
  }

  suspendUser(): void {
    if (!this.suspendReason.trim()) {
      return;
    }

    this.actionLoading = true;
    this.errorMsg = '';

    this.userService.suspendUser(this.userId, this.suspendReason.trim()).subscribe({
      next: () => {
        this.showSuccess('Utilisateur suspendu');
        this.showSuspendForm = false;
        this.suspendReason = '';
        this.actionLoading = false;
        this.loadUser();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Erreur lors de la suspension';
        this.actionLoading = false;
      }
    });
  }

  liftSuspension(): void {
    this.actionLoading = true;
    this.errorMsg = '';

    this.userService.liftSuspension(this.userId).subscribe({
      next: () => {
        this.showSuccess('Suspension levée');
        this.actionLoading = false;
        this.loadUser();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Erreur lors de la levée de suspension';
        this.actionLoading = false;
      }
    });
  }

  goToFreelancerProfile(): void {
    if (this.freelancerProfile) {
      this.router.navigate(['/admin/freelancers', this.freelancerProfile.id]);
    }
  }

  goBack(): void {
    this.router.navigate(['/admin/users']);
  }

  private showSuccess(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 3000);
  }

  getStatusBadge(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'badge-success';
      case 'SUSPENDED':
        return 'badge-danger';
      case 'PENDING':
        return 'badge-warning';
      default:
        return 'badge-muted';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'Actif';
      case 'SUSPENDED':
        return 'Suspendu';
      case 'PENDING':
        return 'En attente';
      default:
        return status || '—';
    }
  }

  getKycBadge(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'badge-success';
      case 'PENDING':
        return 'badge-warning';
      case 'REJECTED':
        return 'badge-danger';
      default:
        return 'badge-muted';
    }
  }

  getKycLabel(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'Approuvé';
      case 'PENDING':
        return 'En attente';
      case 'REJECTED':
        return 'Rejeté';
      default:
        return status || '—';
    }
  }

  getRoleBadge(role: string): string {
    switch (role) {
      case 'ADMIN':
        return 'badge-accent';
      case 'FREELANCER':
        return 'badge-info';
      case 'CLIENT':
        return 'badge-muted';
      default:
        return 'badge-muted';
    }
  }

  getRoleLabel(role: string): string {
    switch (role) {
      case 'ADMIN':
        return 'Admin';
      case 'FREELANCER':
        return 'Freelancer';
      case 'CLIENT':
        return 'Client';
      default:
        return role || '—';
    }
  }

  getInitials(user: UserDTO): string {
    return ((user.firstName?.[0] || '') + (user.lastName?.[0] || '')).toUpperCase() || '?';
  }

  getFreelancerAvailabilityBadge(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'badge-success';
      case 'BUSY':
        return 'badge-warning';
      case 'ON_VACATION':
        return 'badge-muted';
      default:
        return 'badge-muted';
    }
  }

  getFreelancerAvailabilityLabel(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'Disponible';
      case 'BUSY':
        return 'Occupé';
      case 'ON_VACATION':
        return 'En vacances';
      default:
        return status || '—';
    }
  }
}