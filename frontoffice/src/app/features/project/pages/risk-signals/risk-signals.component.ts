import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectApiService } from '../../services/project-api.service';
import { AuthService } from '../../../../core/services/auth.service';

import {
  Project, DeliveryRiskSignal, Task, RiskType, RiskSeverity
} from '../../models/project.models';

@Component({
  selector: 'app-risk-signals',
  templateUrl: './risk-signals.component.html',
  styleUrls: ['./risk-signals.component.css']
})
export class RiskSignalsComponent implements OnInit {

  projectId!: number;
  project!: Project;
  risks: DeliveryRiskSignal[] = [];
  tasks: Task[] = [];
  loading = true;
  error = '';
  analyzingRisks = false;

  currentUserId = 0;
currentRole = '';
isClient = false;
isFreelancer = false;
isAdmin = false;


  // Filtres
  activeFilter: 'ALL' | RiskSeverity = 'ALL';
  activeTypeFilter: 'ALL' | RiskType = 'ALL';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectApi: ProjectApiService,
      private authService: AuthService  // ← AJOUTER

  ) {}

  ngOnInit(): void {
  this.projectId = Number(this.route.snapshot.paramMap.get('id'));
  const user = this.authService.getCurrentAuthUser();
  this.currentUserId = user?.userId || 0;
  this.currentRole = user?.role?.toUpperCase() || '';
  this.isAdmin = this.currentRole === 'ADMIN';
  this.loadData();
  }

  private loadData(): void {
    this.loading = true;

    this.projectApi.getProjectById(this.projectId).subscribe({
      next: (p) => this.project = p
    });

    this.projectApi.getTasksByProjectId(this.projectId).subscribe({
      next: (t) => this.tasks = t
    });

    this.projectApi.getActiveRisks(this.projectId).subscribe({
      next: (data) => {
        this.risks = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les risques.';
        this.loading = false;
      }
    });

this.projectApi.getProjectById(this.projectId).subscribe({
  next: (p) => {
    this.project = p;
    // ✅ Rôle basé sur le JWT
    this.isClient = this.currentRole === 'CLIENT';
    this.isFreelancer = this.currentRole === 'FREELANCER';
  }
});


  }

  // ══════════════════════════════════════
  // ACTIONS
  // ══════════════════════════════════════

  triggerAnalysis(): void {
    this.analyzingRisks = true;
    this.projectApi.analyzeRisks(this.projectId).subscribe({
      next: () => {
        this.projectApi.getActiveRisks(this.projectId).subscribe({
          next: (data) => {
            this.risks = data;
            this.analyzingRisks = false;
          }
        });
      },
      error: () => this.analyzingRisks = false
    });
  }

  resolveRisk(risk: DeliveryRiskSignal, event: Event): void {
    event.stopPropagation();
    this.projectApi.resolveRisk(risk.id).subscribe({
      next: () => {
        this.risks = this.risks.filter(r => r.id !== risk.id);
      }
    });
  }

  // ══════════════════════════════════════
  // FILTRES
  // ══════════════════════════════════════

  get filteredRisks(): DeliveryRiskSignal[] {
    let result = this.risks;
    if (this.activeFilter !== 'ALL') {
      result = result.filter(r => r.severity === this.activeFilter);
    }
    if (this.activeTypeFilter !== 'ALL') {
      result = result.filter(r => r.riskType === this.activeTypeFilter);
    }
    return result;
  }

  setSeverityFilter(f: 'ALL' | RiskSeverity): void { this.activeFilter = f; }
  setTypeFilter(f: 'ALL' | RiskType): void { this.activeTypeFilter = f; }

  get countBySeverity(): Record<string, number> {
    return {
      ALL: this.risks.length,
      CRITICAL: this.risks.filter(r => r.severity === 'CRITICAL').length,
      HIGH: this.risks.filter(r => r.severity === 'HIGH').length,
      MEDIUM: this.risks.filter(r => r.severity === 'MEDIUM').length,
      LOW: this.risks.filter(r => r.severity === 'LOW').length
    };
  }

  get countByType(): Record<string, number> {
    return {
      ALL: this.risks.length,
      DELAY_RISK: this.risks.filter(r => r.riskType === 'DELAY_RISK').length,
      BOTTLENECK: this.risks.filter(r => r.riskType === 'BOTTLENECK').length,
      INACTIVITY: this.risks.filter(r => r.riskType === 'INACTIVITY').length,
      SCOPE_CREEP: this.risks.filter(r => r.riskType === 'SCOPE_CREEP').length
    };
  }

  // ══════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════

  goBack(): void {
    this.router.navigate(['/app/projects', this.projectId]);
  }

  getRiskIcon(type: string): string {
    return {
      DELAY_RISK: 'fa-clock', BOTTLENECK: 'fa-traffic-light',
      INACTIVITY: 'fa-moon', SCOPE_CREEP: 'fa-expand'
    }[type] || 'fa-triangle-exclamation';
  }

  getRiskLabel(type: string): string {
    return {
      DELAY_RISK: 'Risque de retard', BOTTLENECK: 'Goulot d\'étranglement',
      INACTIVITY: 'Inactivité détectée', SCOPE_CREEP: 'Dérive du périmètre'
    }[type] || type;
  }

  getRiskDescription(type: string): string {
    return {
      DELAY_RISK: 'Une ou plusieurs tâches risquent de ne pas respecter leur deadline.',
      BOTTLENECK: 'Une tâche est bloquée en cours ou en review depuis trop longtemps.',
      INACTIVITY: 'Aucune activité détectée sur le projet depuis plusieurs jours.',
      SCOPE_CREEP: 'Le nombre de tâches ajoutées dépasse 30% du volume initial.'
    }[type] || '';
  }

  getSeverityLabel(s: string): string {
    return { LOW: 'Bas', MEDIUM: 'Moyen', HIGH: 'Élevé', CRITICAL: 'Critique' }[s] || s;
  }

  getSeverityClass(s: string): string {
    return {
      LOW: 'sev--low', MEDIUM: 'sev--medium', HIGH: 'sev--high', CRITICAL: 'sev--critical'
    }[s] || '';
  }

  getTypeClass(type: string): string {
    return {
      DELAY_RISK: 'type--delay', BOTTLENECK: 'type--bottleneck',
      INACTIVITY: 'type--inactivity', SCOPE_CREEP: 'type--scope'
    }[type] || '';
  }

  getAffectedTaskTitle(taskId: number | null): string {
    if (!taskId) return '';
    const task = this.tasks.find(t => t.id === taskId);
    return task ? task.title : 'Tâche #' + taskId;
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-TN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  getHealthScore(): number {
    if (this.risks.length === 0) return 100;
    let penalty = 0;
    this.risks.forEach(r => {
      if (r.severity === 'CRITICAL') penalty += 25;
      else if (r.severity === 'HIGH') penalty += 15;
      else if (r.severity === 'MEDIUM') penalty += 8;
      else penalty += 3;
    });
    return Math.max(0, 100 - penalty);
  }

  getHealthColor(): string {
    const score = this.getHealthScore();
    if (score >= 80) return '#22c55e';
    if (score >= 50) return '#f59e0b';
    return '#ef4444';
  }


  get canAnalyze(): boolean {
  return this.isFreelancer || this.isAdmin;
}

get canResolve(): boolean {
  return this.isFreelancer || this.isAdmin;
}
}