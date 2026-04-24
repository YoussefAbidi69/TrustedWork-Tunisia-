import { Component, OnInit } from '@angular/core';
import { SchedulerService, SchedulerConfig } from '../../../core/services/scheduler.service';

@Component({
  selector: 'app-scheduler-management',
  templateUrl: './scheduler-management.component.html',
  styleUrls: ['./scheduler-management.component.css']
})
export class SchedulerManagementComponent implements OnInit {

  configs: SchedulerConfig[] = [];
  loading = true;
  saving = false;

  // Set des jobName en cours d'exécution manuelle (pour le spinner)
  runningJobs = new Set<string>();

  // Job en cours d'édition (null = aucun modal ouvert)
  editingConfig: SchedulerConfig | null = null;

  // Formulaire temporaire pour l'édition
  editForm = {
    intervalMinutes: 0,
    cronExpression: '',
    enabled: true
  };

  // Message de feedback (succès / erreur)
  feedback: { type: 'success' | 'error'; message: string } | null = null;

  constructor(private schedulerService: SchedulerService) {}

  ngOnInit(): void {
    this.loadConfigs();
  }

  /** Charge toutes les configs depuis le backend */
  loadConfigs(): void {
    this.loading = true;
    this.schedulerService.getAllConfigs().subscribe({
      next: (data) => {
        this.configs = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement schedulers :', err);
        this.loading = false;
        this.showFeedback('error', 'Impossible de charger les configurations.');
      }
    });
  }

  /** Ouvre le modal d'édition pour un job */
  openEdit(config: SchedulerConfig): void {
    this.editingConfig = { ...config };
    this.editForm = {
      intervalMinutes: config.intervalMinutes,
      cronExpression: config.cronExpression,
      enabled: config.enabled
    };
    this.feedback = null;
  }

  /** Ferme le modal sans sauvegarder */
  closeEdit(): void {
    this.editingConfig = null;
    this.feedback = null;
  }

  /** Sauvegarde les modifications du job en cours d'édition */
  saveConfig(): void {
    if (!this.editingConfig) return;

    this.saving = true;
    const payload = {
      cronExpression:  this.editForm.cronExpression,
      intervalMinutes: this.editForm.intervalMinutes,
      enabled:         this.editForm.enabled
    };

    this.schedulerService.updateConfig(this.editingConfig.jobName, payload).subscribe({
      next: (updated) => {
        // Mettre à jour la liste locale sans rechargement complet
        const idx = this.configs.findIndex(c => c.jobName === updated.jobName);
        if (idx !== -1) this.configs[idx] = updated;
        this.saving = false;
        this.editingConfig = null;
        this.showFeedback('success', `Job "${updated.jobName}" mis à jour avec succès.`);
      },
      error: (err) => {
        console.error('Erreur mise à jour scheduler :', err);
        this.saving = false;
        this.showFeedback('error', 'Erreur lors de la mise à jour. Vérifiez les valeurs.');
      }
    });
  }

  /** Active ou désactive un job en un clic (toggle rapide sans modal) */
  toggleEnabled(config: SchedulerConfig): void {
    const payload = {
      cronExpression:  config.cronExpression,
      intervalMinutes: config.intervalMinutes,
      enabled:         !config.enabled
    };

    this.schedulerService.updateConfig(config.jobName, payload).subscribe({
      next: (updated) => {
        const idx = this.configs.findIndex(c => c.jobName === updated.jobName);
        if (idx !== -1) this.configs[idx] = updated;
        const etat = updated.enabled ? 'activé' : 'désactivé';
        this.showFeedback('success', `Job "${updated.jobName}" ${etat}.`);
      },
      error: () => {
        this.showFeedback('error', 'Erreur lors du changement d\'état.');
      }
    });
  }

  /** Déclenche un job immédiatement (bouton "Tester maintenant") */
  runJobNow(config: SchedulerConfig): void {
    if (this.runningJobs.has(config.jobName)) return; // évite le double-clic

    this.runningJobs.add(config.jobName);

    this.schedulerService.runJobNow(config.jobName).subscribe({
      next: (res) => {
        this.runningJobs.delete(config.jobName);
        this.showFeedback('success', `✅ Job "${config.jobName}" déclenché. Vérifiez les logs Spring Boot.`);
        // Rechargement après 2s pour afficher le lastRun mis à jour
        setTimeout(() => this.loadConfigs(), 2000);
      },
      error: () => {
        this.runningJobs.delete(config.jobName);
        this.showFeedback('error', `Erreur lors du déclenchement du job "${config.jobName}".`);
      }
    });
  }

  /** Affiche un message de feedback pendant 4 secondes */
  showFeedback(type: 'success' | 'error', message: string): void {
    this.feedback = { type, message };
    setTimeout(() => this.feedback = null, 4000);
  }

  /** Formate lastRun pour l'affichage */
  formatLastRun(lastRun: string | null): string {
    if (!lastRun) return 'Jamais exécuté';
    const d = new Date(lastRun);
    return d.toLocaleString('fr-TN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  /** Formate intervalMinutes en texte lisible */
  formatInterval(minutes: number): string {
    if (minutes < 60)   return `${minutes} min`;
    if (minutes < 1440) return `${Math.round(minutes / 60)}h`;
    if (minutes < 10080) return `${Math.round(minutes / 1440)}j`;
    if (minutes < 43200) return `${Math.round(minutes / 10080)} sem.`;
    return `${Math.round(minutes / 43200)} mois`;
  }

  /** Icône Font Awesome selon le nom du job */
  getJobIcon(jobName: string): string {
    const icons: Record<string, string> = {
      recalculateAllSkillScores:      'fa-brain',
      updateRegionalRankings:         'fa-ranking-star',
      sendProfileCompletionReminders: 'fa-envelope',
      checkCertificationExpiry:       'fa-certificate'
    };
    return icons[jobName] || 'fa-clock';
  }

  /** Couleur de badge selon le nom du job */
  getJobColor(jobName: string): string {
    const colors: Record<string, string> = {
      recalculateAllSkillScores:      'accent',
      updateRegionalRankings:         'info',
      sendProfileCompletionReminders: 'warning',
      checkCertificationExpiry:       'danger'
    };
    return colors[jobName] || 'muted';
  }
}
