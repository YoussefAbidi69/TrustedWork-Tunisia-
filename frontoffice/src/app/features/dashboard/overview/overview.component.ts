import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { DashboardUser } from '../../../core/models/user.model';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent implements OnInit {
  loadingUser = true;
  userError = '';

  connectedUser: DashboardUser = {
    id: null,
    fullName: 'Utilisateur',
    firstName: 'Utilisateur',
    lastName: '',
    email: '',
    role: ''
  };

  marketTrends = [
    {
      title: 'Spring Boot reste très demandé',
      description: 'Les missions backend Java/Spring progressent fortement cette semaine, surtout sur les projets API et microservices.',
      tag: 'Tendance marché'
    },
    {
      title: 'Angular + UI Dashboard en hausse',
      description: 'Les entreprises recherchent davantage de profils capables de moderniser des dashboards métiers et des interfaces internes.',
      tag: 'Frontend'
    },
    {
      title: 'Cloud & DevOps attire plus de contrats premium',
      description: 'Les opportunités impliquant Docker, CI/CD et déploiement cloud offrent actuellement les meilleurs budgets.',
      tag: 'Cloud'
    }
  ];

  recommendedJobs = [
    {
      title: 'Développeur Spring Boot Senior',
      company: 'Tekru Solutions',
      location: 'Tunis / Remote',
      budget: '3,500 TND',
      match: '92%'
    },
    {
      title: 'Refonte Dashboard Angular',
      company: 'Vermeg',
      location: 'La Marsa',
      budget: '2,800 TND',
      match: '88%'
    },
    {
      title: 'Ingénieur DevOps / CI-CD',
      company: 'Datavora',
      location: 'Sfax',
      budget: '4,200 TND',
      match: '85%'
    }
  ];

  recentActivities = [
    'Votre profil a été consulté 12 fois cette semaine.',
    'Votre Trust Score a augmenté de 4 points ce mois-ci.',
    'Une nouvelle opportunité correspond à vos compétences Angular.',
    'Un client a ajouté votre profil à ses favoris.'
  ];

  featuredEvents = [
    {
      title: 'Hackathon FinTech Tunisia',
      meta: 'Ariana • 21 Nov 2025',
      type: 'Event'
    },
    {
      title: 'Challenge Spring Boot Expert',
      meta: 'Online • Cette semaine',
      type: 'Challenge'
    }
  ];

  quickStats = {
    trustScore: 87,
    visibility: 'Top 22%',
    profileCompletion: 78,
    availability: 'Disponible'
  };

  constructor(
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.initializeConnectedUser();
  }

  private initializeConnectedUser(): void {
    const authUser = this.authService.getCurrentAuthUser();

    if (authUser) {
      this.connectedUser = {
        id: authUser.userId,
        fullName: authUser.email,
        firstName: this.extractDisplayNameFromEmail(authUser.email),
        lastName: '',
        email: authUser.email,
        role: authUser.role
      };
    }

    this.userService.getCurrentDashboardUser().subscribe({
      next: (user: DashboardUser) => {
        this.connectedUser = user;
        this.loadingUser = false;
        this.userError = '';
      },
      error: (error: HttpErrorResponse) => {
        this.loadingUser = false;

        if (!authUser) {
          this.userError = 'Impossible de charger les informations du profil connecté.';
        }

        console.error('Erreur lors du chargement du user connecté :', error);
      }
    });
  }

  private extractDisplayNameFromEmail(email: string): string {
    if (!email) {
      return 'Utilisateur';
    }

    const localPart = email.split('@')[0];
    if (!localPart) {
      return 'Utilisateur';
    }

    return localPart.charAt(0).toUpperCase() + localPart.slice(1);
  }

  get displayName(): string {
    return this.connectedUser.firstName || this.connectedUser.fullName || 'Utilisateur';
  }

  get displayFullName(): string {
    return this.connectedUser.fullName || 'Utilisateur';
  }

  get displayRole(): string {
    return this.connectedUser.role || 'MEMBER';
  }

  get displayEmail(): string {
    return this.connectedUser.email || 'Email non disponible';
  }
}