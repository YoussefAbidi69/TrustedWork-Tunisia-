import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MilestoneService } from '../../../core/services/milestone.service';
import { AuthService } from '../../../core/services/auth.service';
import { Milestone } from '../../../core/models/milestone.model';

type EscrowStatus = 'Secured' | 'Pending Release' | 'Released' | 'Under Review';
type EscrowPriority = 'Low' | 'Medium' | 'High';

interface EscrowStat {
  label: string;
  value: string;
  caption: string;
}

interface EscrowTimelineItem {
  title: string;
  date: string;
  description: string;
}

interface EscrowRecord {
  id: string;
  project: string;
  client: string;
  amount: string;
  securedAmount: string;
  releaseDate: string;
  milestone: string;
  progress: number;
  status: EscrowStatus;
  priority: EscrowPriority;
  description: string;
  timeline: EscrowTimelineItem[];
}

@Component({
  selector: 'app-escrow',
  templateUrl: './escrow.component.html',
  styleUrls: ['./escrow.component.css']
})
export class EscrowComponent implements OnInit {
  escrowStats: EscrowStat[] = [];
  escrowRecords: EscrowRecord[] = [];
  selectedEscrow: EscrowRecord | null = null;
  loading = false;
  error = '';

  readonly statusFilters: Array<EscrowStatus | 'All'> = [
    'All',
    'Secured',
    'Pending Release',
    'Released',
    'Under Review'
  ];

  selectedStatus: EscrowStatus | 'All' = 'All';

  constructor(
    private milestoneService: MilestoneService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const userRole = this.authService.getCurrentAuthUser()?.role;
    if (userRole !== 'ADMIN') {
      this.router.navigate(['/app/finance/wallet']);
      return;
    }
    
    this.loadEscrowCases();
  }

  loadEscrowCases(): void {
    this.loading = true;
    this.milestoneService.getAll(0, 100).subscribe({
      next: (response: any) => {
        const data: Milestone[] = Array.isArray(response) ? response : response?.content || [];
        this.escrowRecords = data.map(m => this.mapToEscrowRecord(m));
        this.updateStats();
        
        if (this.escrowRecords.length > 0) {
          this.selectedEscrow = this.escrowRecords[0];
        }
        this.loading = false;
      },
      error: (err: any) => {
        this.error = 'Erreur lors du chargement des jalons d\'Escrow';
        console.error(err);
        this.loading = false;
      }
    });
  }

  mapToEscrowRecord(m: Milestone): EscrowRecord {
    let status: EscrowStatus = 'Secured';
    let priority: EscrowPriority = 'Low';
    let progress = 0;

    switch(m.status) {
      case 'PENDING':
        status = 'Secured';
        progress = 10;
        break;
      case 'IN_PROGRESS':
      case 'STARTED':
        status = 'Secured';
        progress = 50;
        break;
      case 'SUBMITTED':
        status = 'Pending Release';
        priority = 'Medium';
        progress = 80;
        break;
      case 'APPROVED':
      case 'AUTO_APPROVED':
        status = 'Released';
        progress = 100;
        break;
      case 'REJECTED':
        status = 'Under Review';
        priority = 'High';
        progress = 60;
        break;
    }

    return {
      id: `ESC-ML-${m.id}`,
      project: `Contrat #${m.contractId}`,
      client: 'Participant', // Should be dynamically provided by API usually
      amount: `${m.montant} DT`,
      securedAmount: `${m.montant} DT`,
      releaseDate: m.deadline ? new Date(m.deadline as string).toLocaleDateString() : 'Non définie',
      milestone: m.titre || 'Sans titre',
      progress: progress,
      status: status,
      priority: priority,
      description: m.description || `Gestion de l'escrow pour le jalon ${m.id}`,
      timeline: []
    };
  }

  updateStats(): void {
    const totalSecuredValue = this.escrowRecords
      .filter(r => ['Secured', 'Pending Release', 'Under Review'].includes(r.status))
      .reduce((sum, r) => sum + parseFloat(r.amount.replace(' DT', '') || '0'), 0);

    const activeCasesCount = this.escrowRecords.filter(r => r.status !== 'Released').length;

    this.escrowStats = [
      {
        label: 'Fonds Sécurisés',
        value: `${totalSecuredValue} DT`,
        caption: 'Protégés sur le compte Escrow en attente de validation'
      },
      {
        label: 'Dossiers Escrow Actifs',
        value: `${activeCasesCount}`,
        caption: 'Jalons en cours de traitement via notre workflow par tranches'
      },
      {
        label: 'Total des Jalons',
        value: `${this.escrowRecords.length}`,
        caption: 'Tous les jalons créés sur la plateforme'
      }
    ];
  }

  get filteredEscrowRecords(): EscrowRecord[] {
    if (this.selectedStatus === 'All') {
      return this.escrowRecords;
    }
    return this.escrowRecords.filter(item => item.status === this.selectedStatus);
  }

  selectStatus(status: EscrowStatus | 'All'): void {
    this.selectedStatus = status;
    this.syncSelectedEscrow();
  }

  selectEscrow(item: EscrowRecord): void {
    this.selectedEscrow = item;
  }

  getStatusClass(status: EscrowStatus): string {
    switch (status) {
      case 'Secured': return 'status-secured';
      case 'Pending Release': return 'status-pending-release';
      case 'Released': return 'status-released';
      case 'Under Review': return 'status-under-review';
      default: return '';
    }
  }

  getPriorityClass(priority: EscrowPriority): string {
    switch (priority) {
      case 'Low': return 'priority-low';
      case 'Medium': return 'priority-medium';
      case 'High': return 'priority-high';
      default: return '';
    }
  }

  trackByLabel(index: number, item: EscrowStat): string {
    return item.label;
  }

  trackByEscrow(index: number, item: EscrowRecord): string {
    return item.id;
  }

  private syncSelectedEscrow(): void {
    const visibleRecords = this.filteredEscrowRecords;
    if (!visibleRecords.length) return;

    if (this.selectedEscrow) {
      const stillExists = visibleRecords.some(item => item.id === this.selectedEscrow!.id);
      if (!stillExists) {
        this.selectedEscrow = visibleRecords[0];
      }
    } else {
      this.selectedEscrow = visibleRecords[0];
    }
  }
}