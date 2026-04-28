import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RecommendationService } from '../../services/recommendation.service';
import { FreelancerRecommendation, RecommendationFilter } from '../../models/recommendation.model';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-recommended-freelancers',
  templateUrl: './recommended-freelancers.component.html',
  styleUrls: ['./recommended-freelancers.component.css']
})
export class RecommendedFreelancersComponent implements OnInit {

  agencyId!: number;
  agencyName: string = '';
  
  freelancers: FreelancerRecommendation[] = [];
  loading: boolean = true;
  computing: boolean = false;
  
  // Pagination
  page: number = 0;
  size: number = 12;
  totalCandidates: number = 0;
  hasMore: boolean = true;

  // Filters
  filters: RecommendationFilter = {
    sortBy: 'score',
    minScore: 0,
    skills: [],
    availability: '',
    search: ''
  };

  searchTimeout: any;

  // Modal state
  showModal: boolean = false;
  selectedFreelancer: FreelancerRecommendation | null = null;
  invitationMessage: string = '';
  invitationRole: string = 'MEMBER';
  inviting: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private recommendationService: RecommendationService,
    private toastr: ToastrService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.agencyId = +id;
        this.loadRecommendations(true);
      }
    });
  }

  loadRecommendations(reset: boolean = false): void {
    if (reset) {
      this.page = 0;
      this.freelancers = [];
      this.hasMore = true;
    }

    if (!this.hasMore) return;

    this.loading = true;
    this.recommendationService.getRecommendedFreelancers(this.agencyId, this.filters, this.page, this.size)
      .subscribe({
        next: (res) => {
          this.agencyName = res.data.agencyName;
          this.totalCandidates = res.data.totalCandidates;
          
          if (res.data.recommendations.length < this.size) {
            this.hasMore = false;
          }

          if (reset) {
            this.freelancers = res.data.recommendations;
          } else {
            this.freelancers = [...this.freelancers, ...res.data.recommendations];
          }
          this.loading = false;
          this.computing = false;
          (this.filters as any).refresh = false;
        },
        error: (err) => {
          console.error(err);
          this.loading = false;
          this.computing = false;
          this.toastr.error("Impossible de charger les recommandations.");
        }
      });
  }

  onSearchChange(event: any): void {
    const value = event.target.value;
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.filters.search = value;
      this.loadRecommendations(true);
    }, 300);
  }

  onFilterChange(): void {
    this.loadRecommendations(true);
  }

  refreshRecommendations(): void {
    (this.filters as any).refresh = true;
    this.computing = true;
    this.loadRecommendations(true);
  }

  loadMore(): void {
    if (!this.loading && this.hasMore) {
      this.page++;
      this.loadRecommendations();
    }
  }

  // Invitation Modal Logic
  openInviteModal(freelancer: FreelancerRecommendation): void {
    if (freelancer.alreadyInvited) return;
    this.selectedFreelancer = freelancer;
    this.invitationMessage = `Bonjour ${freelancer.firstName}, nous souhaitons vous inviter à rejoindre notre équipe.`;
    this.invitationRole = 'MEMBER';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedFreelancer = null;
    this.inviting = false;
  }

  confirmInvitation(): void {
    if (!this.selectedFreelancer) return;
    this.inviting = true;
    
    const user = this.authService.getCurrentAuthUser();
    const senderId = user ? user.userId : 1; // Fallback to 1 if not found, but it should be available
    
    this.recommendationService.sendInvitation(this.agencyId, this.selectedFreelancer.freelancerId, senderId, this.invitationRole, this.invitationMessage)
      .subscribe({
        next: (res) => {
          this.toastr.success(`Invitation envoyée à ${this.selectedFreelancer?.firstName} !`);
          if (this.selectedFreelancer) {
            this.selectedFreelancer.alreadyInvited = true;
          }
          this.closeModal();
        },
        error: (err) => {
          this.inviting = false;
          if (err.status === 409 || err.error?.message?.includes("already has a pending invitation")) {
            this.toastr.warning("Une invitation est déjà en cours pour ce freelancer.");
            if (this.selectedFreelancer) this.selectedFreelancer.alreadyInvited = true;
            this.closeModal();
          } else {
            const msg = err.error && err.error.message ? err.error.message : "Erreur lors de l'envoi de l'invitation.";
            this.toastr.error(msg);
          }
        }
      });
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName?.charAt(0) || ''}${lastName?.charAt(0) || ''}`.toUpperCase();
  }
}
