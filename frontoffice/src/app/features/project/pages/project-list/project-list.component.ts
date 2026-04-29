import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../services/project-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Project } from '../../models/project.models';

@Component({
  selector: 'app-project-list',
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.css']
})
export class ProjectListComponent implements OnInit {

  projects: Project[] = [];
  loading = true;
  error = '';

  // ── Rôle ──
  currentRole = '';
  isAdmin = false;


  isClient = false;
isFreelancer = false;

  constructor(
    private projectApi: ProjectApiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
  const user = this.authService.getCurrentAuthUser();
  this.currentRole = user?.role?.toUpperCase() || '';
  this.isAdmin = this.currentRole === 'ADMIN';
  this.isClient = this.currentRole === 'CLIENT';
  this.isFreelancer = this.currentRole === 'FREELANCER';
  this.loadProjects();
  }

  private loadProjects(): void {
    this.loading = true;
    this.error = '';

    // Admin → tous les projets | Client/Freelancer → les leurs
    const source$ = this.isAdmin
      ? this.projectApi.getAllProjects()
      : this.projectApi.getMyProjects();

    source$.subscribe({
      next: (data) => {
        this.projects = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Erreur chargement projets :', err);
        if (err.status === 401 || err.status === 403) {
          this.error = 'Session expirée. Veuillez vous reconnecter.';
          this.authService.logout();
          this.router.navigate(['/auth/login']);
        } else {
          this.error = 'Impossible de charger les projets. Vérifiez que le backend est démarré.';
        }
        this.loading = false;
      }
    });
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Actif', ON_HOLD: 'En pause', COMPLETED: 'Terminé', CANCELLED: 'Annulé'
    };
    return map[status] || status;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'status--active', ON_HOLD: 'status--hold',
      COMPLETED: 'status--done', CANCELLED: 'status--cancelled'
    };
    return map[status] || '';
  }
}