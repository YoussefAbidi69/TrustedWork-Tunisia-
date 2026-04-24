import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';

interface AuditLog {
  id?: number;
  action?: string;
  eventType?: string;
  event_type?: string;
  actorEmail?: string;
  targetUser?: string;
  ipAddress?: string;
  ip_address?: string;
  createdAt?: string;
  details?: string;
}

@Component({
  selector: 'app-audit-logs',
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.css']
})
export class AuditLogsComponent implements OnInit {
  logs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  loading = true;
  searchQuery = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    this.userService.getAuditLogs().subscribe({
      next: (data) => {
        this.logs = data || [];
        this.applyFilter();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement audit logs:', error);
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    const q = this.searchQuery.trim().toLowerCase();

    if (!q) {
      this.filteredLogs = [...this.logs];
      return;
    }

    this.filteredLogs = this.logs.filter((log) => {
      const eventType = (log.eventType || log.event_type || '').toLowerCase();
      const actorEmail = (log.actorEmail || '').toLowerCase();
      const targetUser = (log.targetUser || '').toLowerCase();
      const ipAddress = (log.ipAddress || log.ip_address || '').toLowerCase();
      const action = (log.action || '').toLowerCase();
      const details = (log.details || '').toLowerCase();

      return (
        eventType.includes(q) ||
        actorEmail.includes(q) ||
        targetUser.includes(q) ||
        ipAddress.includes(q) ||
        action.includes(q) ||
        details.includes(q)
      );
    });
  }

  onSearch(): void {
    this.applyFilter();
  }

  getEventType(log: AuditLog): string {
    return log.eventType || log.event_type || 'UNKNOWN';
  }

  getIp(log: AuditLog): string {
    return log.ipAddress || log.ip_address || '—';
  }

  getBadgeClass(type: string): string {
    const map: Record<string, string> = {
      LOGIN: 'badge-accent',
      KYC: 'badge-warning',
      SUSPEND: 'badge-danger',
      '2FA': 'badge-success',
      TRUST: 'badge-info'
    };

    return map[type] || 'badge-muted';
  }
}