import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { StripeService } from '../../../core/services/stripe.service';
import { ContractService } from '../../../core/services/contract.service';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout.html',
  styleUrls: ['./checkout.css']
})
export class CheckoutComponent implements OnInit {
  contractId: number = 0;
  contract: any = null;
  amount: number = 0;
  email: string = '';
  
  // Champs carte bancaire
  cardNumber: string = '';
  expiryDate: string = '';
  cvc: string = '';
  cardName: string = '';
  
  loading: boolean = false;
  error: string = '';
  simulationMode = environment.simulationMode;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private stripeService: StripeService,
    private contractService: ContractService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.contractId = Number(this.route.snapshot.queryParams['contractId'] || this.route.snapshot.params['contractId']);
    this.loadContract();
    // Récupérer l'email réel du client depuis la session
    this.email = this.authService.getEmail() || 'client@trustedwork.com';
    
    // Remplir les champs par défaut pour le test
    if (this.simulationMode) {
      this.fillTestCard('4242');
    }
    
    if (this.simulationMode) {
      console.log('🔧 SIMULATION MODE ENABLED 🔧');
    }
  }

  loadContract(): void {
    if (!this.contractId) {
      this.error = 'ID de contrat manquant';
      return;
    }
    this.contractService.getById(this.contractId).subscribe({
      next: (contract) => {
        this.contract = contract;
        this.amount = contract.montantTotal;
        if (contract.status === 'ACTIVE' || contract.status === 'COMPLETED') {
          this.error = 'Ce contrat a déjà été payé et est actif.';
        }
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement du contrat';
        console.error(err);
      }
    });
  }

  // Remplir avec une carte de test
  fillTestCard(type: string): void {
    switch(type) {
      case '4242':
        this.cardNumber = '4242 4242 4242 4242';
        this.expiryDate = '12/30';
        this.cvc = '123';
        this.cardName = 'Client Test';
        break;
      case '4000':
        this.cardNumber = '4000 0025 0000 3155';
        this.expiryDate = '12/30';
        this.cvc = '123';
        this.cardName = 'Client Test';
        break;
      case '0002':
        this.cardNumber = '4000 0000 0000 0002';
        this.expiryDate = '12/30';
        this.cvc = '123';
        this.cardName = 'Client Test';
        break;
      default:
        break;
    }
  }

  async onSubmit() {
    if (this.contract?.status === 'ACTIVE' || this.contract?.status === 'COMPLETED') {
      this.error = 'Paiement impossible : ce contrat est déjà actif.';
      return;
    }
    // Validation des champs
    if (!this.cardNumber || !this.expiryDate || !this.cvc) {
      this.error = 'Veuillez remplir tous les champs de la carte';
      return;
    }

    this.loading = true;
    this.error = '';

    try {
      // 1. Créer le PaymentIntent
      const intent = await this.stripeService.createPaymentIntent(this.contractId, this.email).toPromise();
      
      console.log('Payment intent created:', intent);
      
      if (this.simulationMode) {
        // En simulation, vérifier le numéro de carte
        const cardLast4 = this.cardNumber.replace(/\s/g, '').slice(-4);
        
        // Simuler différents résultats selon la carte
        if (cardLast4 === '0002') {
          this.error = 'Paiement refusé (carte de test)';
          this.loading = false;
          return;
        } else if (cardLast4 === '3155') {
          // Carte 3D Secure - simuler succès quand même
          console.log('3D Secure simulation - payment would require authentication');
        }
        
        // Paiement réussi en simulation
        this.stripeService.confirmPayment(intent.paymentIntentId, this.contractId).subscribe({
          next: () => {
            this.router.navigate(['/payment/result'], { 
              queryParams: { status: 'success', contractId: this.contractId }
            });
          },
          error: (err) => {
            this.error = err.error?.error || 'Erreur lors de la confirmation';
            this.loading = false;
          }
        });
        return;
      }
      
      // Mode réel Stripe
      const result = await this.stripeService.confirmStripePayment(intent.clientSecret);
      
      if (result.error) {
        this.error = result.error.message || 'Erreur de paiement';
        this.loading = false;
      } else {
        this.stripeService.confirmPayment(intent.paymentIntentId, this.contractId).subscribe({
          next: () => {
            this.router.navigate(['/payment/result'], { 
              queryParams: { status: 'success', contractId: this.contractId }
            });
          },
          error: (err) => {
            this.error = err.error?.error || 'Erreur lors de la confirmation';
            this.loading = false;
          }
        });
      }
    } catch (err: any) {
      console.error('Payment error:', err);
      this.error = err.message || 'Erreur lors du paiement';
      this.loading = false;
    }
  }
}