import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RecommendationService } from '../../../../core/services/recommendation.service';
import { FreelancerRecommendation } from '../../../../core/models/recommendation.model';

@Component({
  selector: 'app-project-form',
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.css']
})
export class ProjectFormComponent implements OnInit {
  descriptionForm!: FormGroup;
  budgetForm!: FormGroup;
  modeForm!: FormGroup;
  recommendations: FreelancerRecommendation[] = [];
  isLoading = false;
  isMLServiceAvailable = false;
  currentStep = 0;
  submitted = false;

  categories = [
    { value: 'dev', label: '💻 Développement' },
    { value: 'design', label: '🎨 Design' },
    { value: 'marketing', label: '📢 Marketing' },
    { value: 'data', label: '📊 Data' },
    { value: 'writing', label: '✍️ Rédaction' }
  ];

  optimizationModes = [
    { value: 'best', label: '🏆 Meilleur score global', description: 'Trouve les freelancers avec la plus haute probabilité de succès.' },
    { value: 'fastest', label: '⚡ Le plus rapide disponible', description: 'Priorise les freelancers disponibles immédiatement avec un bon historique de ponctualité.' },
    { value: 'best_value', label: '💰 Meilleur rapport qualité-prix', description: 'Optimise le rapport entre qualité, budget et taux horaire.' },
    { value: 'lowest_risk', label: '🛡️ Risque minimal', description: 'Priorise les freelancers avec le plus faible taux de litiges et haute fiabilité.' }
  ];

  constructor(
    private fb: FormBuilder,
    private recommendationService: RecommendationService
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.checkMLService();
  }

  initForms(): void {
    this.descriptionForm = this.fb.group({
      description: ['', [Validators.required, Validators.minLength(10)]],
      category: ['dev', Validators.required]
    });

    this.budgetForm = this.fb.group({
      budget: [3000, [Validators.required, Validators.min(100)]],
      deadlineDays: [30, [Validators.required, Validators.min(1), Validators.max(365)]]
    });

    this.modeForm = this.fb.group({
      optimizationMode: ['best', Validators.required]
    });
  }

  checkMLService(): void {
    this.recommendationService.healthCheck().subscribe({
      next: (result) => {
        this.isMLServiceAvailable = result;
        console.log('✅ Service ML disponible');
      },
      error: () => {
        this.isMLServiceAvailable = false;
        console.error('❌ Service ML indisponible');
      }
    });
  }

  onStepChange(index: number): void {
    this.currentStep = index;
  }

  onSubmit(): void {
    if (this.descriptionForm.valid && this.budgetForm.valid && this.modeForm.valid) {
      this.isLoading = true;
      this.recommendations = [];
      this.submitted = true;

      const request = {
        description: this.descriptionForm.get('description')?.value,
        category: this.descriptionForm.get('category')?.value,
        budget: this.budgetForm.get('budget')?.value,
        deadlineDays: this.budgetForm.get('deadlineDays')?.value,
        optimizationMode: this.modeForm.get('optimizationMode')?.value
      };

      console.log('📤 Envoi de la requête:', request);

      this.recommendationService.getRecommendations(request).subscribe({
        next: (results) => {
          this.recommendations = results;
          this.isLoading = false;
          console.log('✅ Résultats reçus:', results.length);
        },
        error: (error) => {
          console.error('❌ Erreur:', error);
          this.isLoading = false;
          alert('Erreur lors de la recherche. Vérifiez que le service ML est démarré.');
        }
      });
    }
  }

  getProgressWidth(): number {
    return ((this.currentStep + 1) / 3) * 100;
  }
  getModeDescription(): string {
  const mode = this.modeForm.get('optimizationMode')?.value;
  const modes: { [key: string]: string } = {
    'best': '🏆 Trouve les freelancers avec la plus haute probabilité de succès.',
    'fastest': '⚡ Priorise les freelancers disponibles immédiatement avec un bon historique de ponctualité.',
    'best_value': '💰 Optimise le rapport entre qualité, budget et taux horaire.',
    'lowest_risk': '🛡️ Priorise les freelancers avec le plus faible taux de litiges et haute fiabilité.'
  };
  return modes[mode] || '';
}

  viewProfile(freelancerId: string): void {
    // À implémenter selon votre logique
    console.log('Voir profil:', freelancerId);
  }

  inviteFreelancer(freelancer: FreelancerRecommendation): void {
    // À implémenter selon votre logique
    console.log('Inviter:', freelancer.name);
  }
}