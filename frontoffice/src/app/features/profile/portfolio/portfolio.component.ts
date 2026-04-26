import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { PortfolioItem } from '../../../core/models/freelancer.model';

/**
 * Composant Portfolio — affichage et gestion des projets réalisés
 */
@Component({
  selector: 'app-portfolio',
  templateUrl: './portfolio.component.html',
  styleUrls: ['./portfolio.component.css']
})
export class PortfolioComponent implements OnInit {
  portfolioItems: PortfolioItem[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  showForm = false;
  isSubmitting = false;
  isPinning = false;

  readonly maxPinnedItems = 3;

  newItem = {
    title: '',
    description: '',
    projectUrl: '',
    imageUrl: '',
    technologies: '',
    completionDate: '',
    pinned: false
  };

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadPortfolio();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  get projectsCountLabel(): string {
    return `${this.portfolioItems.length} projet(s)`;
  }

  get hasItems(): boolean {
    return this.portfolioItems.length > 0;
  }

  get titleLength(): number {
    return this.newItem.title.trim().length;
  }

  get pinnedItems(): PortfolioItem[] {
    return this.portfolioItems.filter(item => item.pinned);
  }

  get regularItems(): PortfolioItem[] {
    return this.portfolioItems.filter(item => !item.pinned);
  }

  get pinnedCount(): number {
    return this.pinnedItems.length;
  }

  get canPinMore(): boolean {
    return this.pinnedCount < this.maxPinnedItems;
  }

  loadPortfolio(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.getMyPortfolio(this.currentUserId).subscribe({
      next: (items) => {
        this.portfolioItems = this.sortPortfolioItems(items ?? []);
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = error?.error?.message || 'Erreur lors du chargement du portfolio.';
        this.isLoading = false;
      }
    });
  }

  addItem(): void {
    if (!this.newItem.title.trim()) {
      this.errorMessage = 'Le titre du projet est obligatoire.';
      return;
    }

    if (this.newItem.pinned && !this.canPinMore) {
      this.errorMessage = 'Vous ne pouvez pas épingler plus de 3 projets.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.addPortfolioItem(this.currentUserId, this.newItem).subscribe({
      next: (item) => {
        this.portfolioItems = this.sortPortfolioItems([item, ...this.portfolioItems]);
        this.successMessage = 'Projet ajouté avec succès.';
        this.resetForm();
        this.showForm = false;
        this.isSubmitting = false;
      },
      error: (error) => {
        this.errorMessage = error?.error?.message || 'Erreur lors de l’ajout du projet.';
        this.isSubmitting = false;
      }
    });
  }

  pinItem(item: PortfolioItem): void {
    if (item.pinned) {
      return;
    }

    if (!this.canPinMore) {
      this.errorMessage = 'Vous avez déjà atteint la limite de 3 projets épinglés.';
      return;
    }

    this.isPinning = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.pinPortfolioItem(item.id, this.currentUserId).subscribe({
      next: (updatedItem) => {
        this.replacePortfolioItem(updatedItem);
        this.successMessage = 'Projet épinglé avec succès.';
        this.isPinning = false;
      },
      error: (error) => {
        this.errorMessage = error?.error?.message || 'Erreur lors de l’épinglage du projet.';
        this.isPinning = false;
      }
    });
  }

  unpinItem(item: PortfolioItem): void {
    if (!item.pinned) {
      return;
    }

    this.isPinning = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.unpinPortfolioItem(item.id, this.currentUserId).subscribe({
      next: (updatedItem) => {
        this.replacePortfolioItem(updatedItem);
        this.successMessage = 'Projet désépinglé avec succès.';
        this.isPinning = false;
      },
      error: (error) => {
        this.errorMessage = error?.error?.message || 'Erreur lors du désépinglage du projet.';
        this.isPinning = false;
      }
    });
  }

  deleteItem(itemId: number): void {
    if (!confirm('Supprimer ce projet ?')) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.deletePortfolioItem(itemId, this.currentUserId).subscribe({
      next: () => {
        this.portfolioItems = this.portfolioItems.filter(item => item.id !== itemId);
        this.successMessage = 'Projet supprimé avec succès.';
      },
      error: (error) => {
        this.errorMessage = error?.error?.message || 'Erreur lors de la suppression.';
      }
    });
  }

  resetForm(): void {
    this.newItem = {
      title: '',
      description: '',
      projectUrl: '',
      imageUrl: '',
      technologies: '',
      completionDate: '',
      pinned: false
    };
  }

  getTechArray(technologies: string): string[] {
    if (!technologies) {
      return [];
    }

    return technologies
      .split(',')
      .map(tech => tech.trim())
      .filter(tech => tech.length > 0);
  }

  getProjectScoreLabel(score?: number): string {
    const safeScore = score ?? 0;

    if (safeScore >= 100) {
      return 'Excellent';
    }
    if (safeScore >= 80) {
      return 'Très bon';
    }
    if (safeScore >= 60) {
      return 'Bon';
    }
    if (safeScore >= 40) {
      return 'Moyen';
    }
    return 'À compléter';
  }

  getProjectScoreWidth(score?: number): string {
    const safeScore = Math.max(0, Math.min(100, score ?? 0));
    return `${safeScore}%`;
  }

  hasProjectLink(item: PortfolioItem): boolean {
    return !!item.projectUrl?.trim();
  }

  hasProjectImage(item: PortfolioItem): boolean {
    return !!item.imageUrl?.trim();
  }

  trackByProjectId(index: number, item: PortfolioItem): number {
    return item.id;
  }

  private replacePortfolioItem(updatedItem: PortfolioItem): void {
    this.portfolioItems = this.sortPortfolioItems(
      this.portfolioItems.map(item => item.id === updatedItem.id ? updatedItem : item)
    );
  }

  private sortPortfolioItems(items: PortfolioItem[]): PortfolioItem[] {
    return [...items].sort((a, b) => {
      if (a.pinned !== b.pinned) {
        return a.pinned ? -1 : 1;
      }

      const dateA = a.completionDate ? new Date(a.completionDate).getTime() : 0;
      const dateB = b.completionDate ? new Date(b.completionDate).getTime() : 0;

      if (dateA !== dateB) {
        return dateB - dateA;
      }

      return b.id - a.id;
    });
  }
}