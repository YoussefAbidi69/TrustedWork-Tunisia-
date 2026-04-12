import { Component, OnInit } from '@angular/core';
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

  constructor(
    private projectApi: ProjectApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  private loadProjects(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user) return;

    this.projectApi.getProjectsByUserId(user.userId).subscribe({
      next: (data) => {
        this.projects = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossible de charger les projets.';
        this.loading = false;
        console.error(err);
      }
    });
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Actif', ON_HOLD: 'En pause',
      COMPLETED: 'Terminé', CANCELLED: 'Annulé'
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