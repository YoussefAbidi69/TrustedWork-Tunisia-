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
  showForm = false;

  // Formulaire d'ajout
  newItem = {
    title: '',
    description: '',
    projectUrl: '',
    imageUrl: '',
    technologies: '',
    completionDate: ''
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
  loadPortfolio(): void {
    this.isLoading = true;
    this.profileService.getMyPortfolio(this.currentUserId).subscribe({
      next: (items) => {
        this.portfolioItems = items;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors du chargement du portfolio';
        this.isLoading = false;
      }
    });
  }

  addItem(): void {
    if (!this.newItem.title.trim()) return;

    this.profileService.addPortfolioItem(this.currentUserId, this.newItem).subscribe({
      next: (item) => {
        this.portfolioItems.push(item);
        this.resetForm();
        this.showForm = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors de l\'ajout du projet';
      }
    });
  }

  deleteItem(itemId: number): void {
    if (!confirm('Supprimer ce projet ?')) return;

    this.profileService.deletePortfolioItem(itemId, this.currentUserId).subscribe({
      next: () => {
        this.portfolioItems = this.portfolioItems.filter(i => i.id !== itemId);
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression';
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
      completionDate: ''
    };
  }

  getTechArray(technologies: string): string[] {
    if (!technologies) return [];
    return technologies.split(',').map(t => t.trim()).filter(t => t.length > 0);
  }
}