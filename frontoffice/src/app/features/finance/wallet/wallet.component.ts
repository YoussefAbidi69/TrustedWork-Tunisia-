import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { WalletService } from '../../../core/services/wallet.service';
import { Wallet, Transaction } from '../../../core/models/wallet.model';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { ConnectedUserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.component.html',
  styleUrls: ['./wallet.component.css']
})
export class WalletComponent implements OnInit {
  wallet: Wallet | null = null;
  transactions: Transaction[] = [];
  stripeStatus: string = '';
  stripeStatusMessage: string = '';
  loading: boolean = false;
  txLoading: boolean = false;
  walletError: string = '';
  userCin: string = '';

  constructor(
    private walletService: WalletService,
    private router: Router,
    public authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.userService.getCurrentUser().subscribe({
      next: (user: ConnectedUserResponse) => {
        if (user && user.cin) {
          this.userCin = user.cin.toString();
          this.loadWallet();
          this.loadStripeStatus();
          this.loadTransactions();
        } else {
          this.walletError = 'CIN non trouvé pour cet utilisateur.';
          this.loading = false;
        }
      },
      error: (err: any) => {
        this.walletError = 'Impossible de récupérer votre profil utilisateur.';
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadWallet(): void {
    this.loading = true;
    this.walletError = '';
    this.walletService.getWallet(this.userCin).subscribe({
      next: (wallet) => {
        this.wallet = wallet;
        this.loading = false;
        console.log('✅ Wallet loaded:', wallet);
      },
      error: (err) => {
        console.error('❌ Error loading wallet:', err);
        this.loading = false;
        if (err.status === 404) {
          this.walletError = 'Portefeuille non trouvé. Contactez l\'administrateur.';
        } else if (err.status === 401) {
          this.walletError = 'Session expirée. Veuillez vous reconnectez.';
        } else {
          this.walletError = `Erreur lors du chargement (${err.status || 'réseau'})`;
        }
      }
    });
  }

  loadStripeStatus(): void {
    this.walletService.getStripeStatus(this.userCin).subscribe({
      next: (res) => {
        this.stripeStatus = res.status || res;
        this.updateStatusMessage();
      },
      error: (err) => {
        console.error('Error loading stripe status:', err);
        this.stripeStatus = 'NOT_CREATED';
        this.updateStatusMessage();
      }
    });
  }

  loadTransactions(): void {
    this.txLoading = true;
    this.walletService.getTransactions(this.userCin).subscribe({
      next: (txs) => {
        this.transactions = Array.isArray(txs) ? txs : [];
        this.txLoading = false;
      },
      error: (err) => {
        this.transactions = [];
        this.txLoading = false;
      }
    });
  }

  refresh(): void {
    this.loadWallet();
    this.loadTransactions();
  }

  updateStatusMessage(): void {
    switch (this.stripeStatus) {
      case 'ACTIVE':
        this.stripeStatusMessage = 'Votre compte Stripe est actif. Vous pouvez recevoir des paiements.';
        break;
      case 'PENDING':
        this.stripeStatusMessage = 'Votre compte Stripe est en cours de vérification.';
        break;
      case 'INCOMPLETE':
        this.stripeStatusMessage = 'Veuillez finaliser votre inscription Stripe.';
        break;
      default:
        this.stripeStatusMessage = 'Configurez Stripe pour recevoir vos paiements.';
    }
  }

  setupStripe(): void {
    // Navigate to a settings or dummy onboarding page since it's not fully ported
    this.router.navigate(['/app/finance/wallet']);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'PENDING': return 'status-pending';
      case 'INCOMPLETE': return 'status-incomplete';
      default: return 'status-default';
    }
  }

  addTestFunds(): void {
    const amount = prompt('Combien souhaitez-vous ajouter ? (Simulation DEV)', '1000');
    if (amount && !isNaN(Number(amount)) && Number(amount) > 0) {
      this.walletService.credit(this.userCin, Number(amount)).subscribe({
        next: (wallet) => {
          this.wallet = wallet;
          alert(`✅ ${amount} DT ajoutés avec succès ! Nouveau solde : ${wallet.balance} DT`);
        },
        error: (err) => {
          console.error(err);
          alert('❌ Erreur lors de l\'ajout de fonds : ' + (err.error?.message || err.message));
        }
      });
    }
  }
}