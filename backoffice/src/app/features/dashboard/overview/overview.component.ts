import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { UserService, DashboardStats } from '../../../core/services/user.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

interface StatCard {
  label:      string;
  value:      string;
  change:     string;
  changeType: 'up' | 'down' | 'flat';
  icon:       string;
  iconClass:  string;
}

interface RecentActivity {
  user:     string;
  initials: string;
  action:   string;
  time:     string;
  type:     'kyc' | 'review' | 'contract' | 'badge';
}

interface Module02Stat {
  label: string;
  value: string;
  icon:  string;
  color: string;
}

@Component({
  selector:    'app-overview',
  templateUrl: './overview.component.html',
  styleUrls:   ['./overview.component.css']
})
export class OverviewComponent implements OnInit, AfterViewInit, OnDestroy {

  currentTime = '';
  kycCount    = 0;
  private clockInterval: any;

  // ── Stats Module 01 (user-service) ──
  stats: StatCard[] = [
    { label: 'Total Users',     value: '0', change: '', changeType: 'flat', icon: 'fa-users',      iconClass: 'accent'  },
    { label: 'Active Users',    value: '0', change: '', changeType: 'flat', icon: 'fa-user-check', iconClass: 'success' },
    { label: 'KYC Pending',     value: '0', change: '', changeType: 'flat', icon: 'fa-id-card',    iconClass: 'warning' },
    { label: 'Suspended Users', value: '0', change: '', changeType: 'flat', icon: 'fa-ban',        iconClass: 'danger'  },
    { label: 'Freelancers',     value: '0', change: '', changeType: 'flat', icon: 'fa-briefcase',  iconClass: 'info'    },
    { label: 'Clients',         value: '0', change: '', changeType: 'flat', icon: 'fa-building',   iconClass: 'gold'    }
  ];

  // ── Stats Module 02 (freelancer-profile-service) ──
  module02Stats: Module02Stat[] = [
    { label: 'Profils freelancer', value: '—', icon: 'fa-address-card',  color: '#3b82f6' },
    { label: 'Reports en attente', value: '—', icon: 'fa-flag',          color: '#ef4444' },
    { label: 'Profils suspendus',  value: '—', icon: 'fa-ban',           color: '#f97316' },
    { label: 'Complétude moyenne', value: '—', icon: 'fa-circle-check',  color: '#10b981' },
    { label: 'Disponibles',        value: '—', icon: 'fa-circle-dot',    color: '#22c55e' },
    { label: 'Risk Score moyen',   value: '—', icon: 'fa-shield-halved', color: '#8b5cf6' }
  ];

  module02Loading = true;

  recentActivities: RecentActivity[] = [
    { user: 'Ahmed Ben Ali',   initials: 'AB', action: 'KYC submitted — awaiting review',     time: '2 min ago',  type: 'kyc'      },
    { user: 'Sarra Trabelsi',  initials: 'ST', action: 'Received 5★ review on contract #482', time: '8 min ago',  type: 'review'   },
    { user: 'Mohamed Gharbi',  initials: 'MG', action: 'Contract #489 signed — 2,400 DT',     time: '15 min ago', type: 'contract' },
    { user: 'Ines Mansouri',   initials: 'IM', action: 'Badge unlocked',                      time: '32 min ago', type: 'badge'    }
  ];

  modules = [
    { name: 'User Service',        port: '8081', status: 'online',  endpoints: 24, icon: 'fa-user-shield'  },
    { name: 'Freelancer Service',  port: '8082', status: 'online',  endpoints: 32, icon: 'fa-address-card' },
    { name: 'Review Service',      port: '8085', status: 'online',  endpoints: 18, icon: 'fa-star'         },
    { name: 'Contract Service',    port: '8083', status: 'offline', endpoints: 21, icon: 'fa-file-contract'},
    { name: 'Event Service',       port: '8087', status: 'offline', endpoints: 12, icon: 'fa-calendar-days'},
    { name: 'Recruit Service',     port: '8089', status: 'offline', endpoints: 16, icon: 'fa-user-tie'     }
  ];

  constructor(
    private userService: UserService,
    private freelancerService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadModule02Stats();
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 1000);
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.drawAllSparklines(), 300);
  }

  ngOnDestroy(): void {
    if (this.clockInterval) clearInterval(this.clockInterval);
  }

  updateClock(): void {
    this.currentTime = new Date().toLocaleTimeString('en-GB', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    });
  }

  // ── Module 01 : stats utilisateurs ──
  loadStats(): void {
    this.userService.getDashboardStats().subscribe({
      next: (data: DashboardStats) => {
        this.kycCount = data.kycPending;
        this.stats = [
          { label: 'Total Users',     value: '0', change: '', changeType: 'flat', icon: 'fa-users',      iconClass: 'accent'  },
          { label: 'Active Users',    value: '0', change: '', changeType: 'flat', icon: 'fa-user-check', iconClass: 'success' },
          { label: 'KYC Pending',     value: '0', change: '', changeType: 'flat', icon: 'fa-id-card',    iconClass: 'warning' },
          { label: 'Suspended Users', value: '0', change: '', changeType: 'flat', icon: 'fa-ban',        iconClass: 'danger'  },
          { label: 'Freelancers',     value: '0', change: '', changeType: 'flat', icon: 'fa-briefcase',  iconClass: 'info'    },
          { label: 'Clients',         value: '0', change: '', changeType: 'flat', icon: 'fa-building',   iconClass: 'gold'    }
        ];
        const targets = [
          data.totalUsers, data.activeUsers, data.kycPending,
          data.suspendedUsers, data.totalFreelancers, data.totalClients
        ];
        setTimeout(() => this.animateCounters(targets), 80);
        setTimeout(() => this.drawAllSparklines(), 80);
      },
      error: (err) => console.error('Erreur dashboard stats:', err)
    });
  }

  // ── Module 02 : stats profils freelancer ──
  loadModule02Stats(): void {
    this.module02Loading = true;

    forkJoin({
      profiles: this.freelancerService.getAllProfiles().pipe(catchError(() => of([]))),
      reports:  this.freelancerService.getPendingReports().pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ profiles, reports }) => {
        const all: any[] = profiles || [];
        const total      = all.length;

        const pendingReports    = (reports || []).length;
        const suspendedCount    = all.filter((p: any) => p.suspended).length;
        const availableCount    = all.filter((p: any) => p.availabilityStatus === 'AVAILABLE').length;
        const avgCompleteness   = total
          ? Math.round(all.reduce((s: number, p: any) => s + (p.completenessScore || 0), 0) / total)
          : 0;
        const avgRiskScore      = total
          ? Math.round(all.reduce((s: number, p: any) => s + (p.riskScore || 0), 0) / total)
          : 0;

        this.module02Stats = [
          { label: 'Profils freelancer', value: String(total),              icon: 'fa-address-card', color: '#3b82f6' },
          { label: 'Reports en attente', value: String(pendingReports),     icon: 'fa-flag',         color: '#ef4444' },
          { label: 'Profils suspendus',  value: String(suspendedCount),     icon: 'fa-ban',          color: '#f97316' },
          { label: 'Complétude moyenne', value: avgCompleteness + '%',      icon: 'fa-circle-check', color: '#10b981' },
          { label: 'Disponibles',        value: String(availableCount),     icon: 'fa-circle-dot',   color: '#22c55e' },
          { label: 'Risk Score moyen',   value: String(avgRiskScore) + '/100', icon: 'fa-shield-halved', color: '#8b5cf6' }
        ];

        this.module02Loading = false;
      },
      error: () => { this.module02Loading = false; }
    });
  }

  // ── Animation compteurs ──
  private animateCounters(targets: number[]): void {
    const steps = 30, interval = 40;
    this.stats.forEach((stat, i) => {
      const target = targets[i];
      if (target === 0) return;
      const increment = target / steps;
      let step = 0;
      const timer = setInterval(() => {
        step++;
        stat.value = String(Math.min(Math.round(increment * step), target));
        if (step >= steps) { stat.value = String(target); clearInterval(timer); }
      }, interval);
    });
  }

  // ── Sparklines ──
  private drawAllSparklines(): void {
    const configs: Record<string, { base: number; color: string }> = {
      accent:  { base: 12000, color: 'rgba(16,185,129,1)'  },
      success: { base: 8800,  color: 'rgba(52,211,153,1)'  },
      warning: { base: 180,   color: 'rgba(251,191,36,1)'  },
      danger:  { base: 40,    color: 'rgba(248,113,113,1)' },
      info:    { base: 7200,  color: 'rgba(56,189,248,1)'  },
      gold:    { base: 5000,  color: 'rgba(245,158,11,1)'  },
    };
    Object.entries(configs).forEach(([key, cfg]) => {
      const data = this.generateSparkData(cfg.base, cfg.base * 0.03, 20);
      this.drawSparkline('spark-' + key, data, cfg.color);
    });
  }

  private generateSparkData(base: number, variance: number, count: number): number[] {
    const data: number[] = [];
    let val = base;
    for (let i = 0; i < count; i++) {
      val += (Math.random() - 0.45) * variance;
      val = Math.max(base * 0.6, val);
      data.push(val);
    }
    return data;
  }

  private drawSparkline(canvasId: string, data: number[], color: string): void {
    const canvas = document.getElementById(canvasId) as HTMLCanvasElement;
    if (!canvas) return;
    const dpr  = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width  = rect.width  * dpr;
    canvas.height = rect.height * dpr;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.scale(dpr, dpr);
    const w = rect.width, h = rect.height;
    const min = Math.min(...data), max = Math.max(...data);
    const range = max - min || 1, pad = 2;
    const points = data.map((v, i) => ({
      x: pad + (i / (data.length - 1)) * (w - pad * 2),
      y: pad + (1 - (v - min) / range) * (h - pad * 2)
    }));
    const grad = ctx.createLinearGradient(0, 0, 0, h);
    grad.addColorStop(0, color.replace(',1)', ',0.2)'));
    grad.addColorStop(1, color.replace(',1)', ',0)'));
    ctx.beginPath();
    ctx.moveTo(points[0].x, h);
    points.forEach(p => ctx.lineTo(p.x, p.y));
    ctx.lineTo(points[points.length - 1].x, h);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();
    ctx.beginPath();
    points.forEach((p, i) => i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y));
    ctx.strokeStyle = color;
    ctx.lineWidth   = 1.5;
    ctx.lineJoin    = 'round';
    ctx.lineCap     = 'round';
    ctx.stroke();
    const last = points[points.length - 1];
    ctx.beginPath();
    ctx.arc(last.x, last.y, 2.5, 0, Math.PI * 2);
    ctx.fillStyle = color;
    ctx.fill();
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      kyc: 'fa-id-card', review: 'fa-star', contract: 'fa-file-contract', badge: 'fa-trophy'
    };
    return icons[type] || 'fa-circle';
  }

  getActivityClass(type: string): string {
    const classes: Record<string, string> = {
      kyc: 'warning', review: 'accent', contract: 'success', badge: 'gold'
    };
    return classes[type] || 'accent';
  }
  /**
 * Génère une largeur aléatoire pour les barres d'intelligence (effet visuel)
 */
getRandomBarWidth(index: number): number {
  // Valeurs prédéfinies pour un look réaliste
  const widths = [78, 45, 23, 89, 65, 54];
  return widths[index] || Math.floor(Math.random() * 60) + 30;
}

/**
 * Récupère la valeur d'une stat Module 02 par index
 */
getModule02Value(index: number): string {
  return this.module02Stats[index]?.value || '0';
}
}