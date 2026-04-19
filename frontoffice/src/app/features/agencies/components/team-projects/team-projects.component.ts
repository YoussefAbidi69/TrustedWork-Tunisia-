import { Component, Input, OnInit } from '@angular/core';
import { TeamProjectService } from '../../services/team-project.service';
import { TeamProject, ProjectStatus } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-team-projects',
  templateUrl: './team-projects.component.html',
  styleUrls: ['./team-projects.component.css']
})
export class TeamProjectsComponent implements OnInit {
  @Input() agencyId!: number;
  projects: TeamProject[] = [];
  loading = true;

  constructor(private projectService: TeamProjectService) {}

  ngOnInit(): void {
    if (this.agencyId) {
      this.loadProjects();
    }
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getProjectsByAgency(this.agencyId).subscribe({
      next: (data) => {
        this.projects = data;
        this.loading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  getStatusClass(status: string): string {
    switch(status) {
      case ProjectStatus.COMPLETED: return 'status--completed';
      case ProjectStatus.ON_HOLD: return 'status--hold';
      case ProjectStatus.PLANNING: return 'status--planning';
      default: return 'status--active';
    }
  }
}
