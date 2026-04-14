import { Component, OnInit } from '@angular/core';
import { WalletService } from '../../../core/services/wallet.service';
import { UserService } from '../../../core/services/user.service';
import { Transaction } from '../../../core/models/wallet.model';
import { ConnectedUserResponse } from '../../../core/models/user.model';

type PaymentHistoryStatus = 'Paid' | 'Scheduled' | 'Processing' | 'Cancelled';
type PaymentHistoryMethod = 'Bank Transfer' | 'Card' | 'Escrow Release' | 'Wallet Settlement' | 'Wallet';

interface PaymentHistoryStat {
  label: string;
  value: string;
  caption: string;
}

interface PaymentHistoryTimelineItem {
  title: string;
  date: string;
  description: string;
}

interface PaymentHistoryRecord {
  id: string;
  title: string;
  client: string;
  amount: string;
  netAmount: string;
  method: PaymentHistoryMethod;
  status: PaymentHistoryStatus;
  period: string;
  paidAt: string;
  reference: string;
  description: string;
  timeline: PaymentHistoryTimelineItem[];
}

@Component({
  selector: 'app-payments-history',
  templateUrl: './payments-history.component.html',
  styleUrls: ['./payments-history.component.css']
})
export class PaymentsHistoryComponent implements OnInit {
  paymentStats: PaymentHistoryStat[] = [
    {
      label: 'Montant total',
      value: '0 DT',
      caption: 'Total des transactions historisées réussies'
    },
    {
      label: 'Mois en cours',
      value: '0 DT',
      caption: 'Valeur totale enregistrée sur la période'
    },
    {
      label: 'Paiement moyen',
      value: '0 DT',
      caption: 'Valeur moyenne des transactions validées'
    }
  ];

  readonly statusFilters: Array<PaymentHistoryStatus | 'All'> = [
    'All',
    'Paid',
    'Scheduled',
    'Processing',
    'Cancelled'
  ];

  selectedStatus: PaymentHistoryStatus | 'All' = 'All';
  paymentHistory: PaymentHistoryRecord[] = [];
  selectedPayment: PaymentHistoryRecord | null = null;
  loading: boolean = true;

  constructor(
    private walletService: WalletService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user: ConnectedUserResponse) => {
        if (user && user.cin) {
          this.loadTransactions(user.cin.toString(), user);
        } else {
          this.loading = false;
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadTransactions(userCin: string, user: ConnectedUserResponse): void {
    this.walletService.getTransactions(userCin).subscribe({
      next: (txs: Transaction[]) => {
        const transactions = Array.isArray(txs) ? txs : [];
        
        let totalAmount = 0;
        
        this.paymentHistory = transactions.map(tx => {
          totalAmount += tx.montant;
          return {
            id: tx.reference ? `#${tx.reference}` : `PAY-${tx.id}`,
            title: tx.description || 'Paiement de transaction',
            client: user.nom ? `${user.prenom} ${user.nom}` : 'TrustedWork',
            amount: `${tx.montant} DT`,
            netAmount: `${tx.montant} DT`,
            method: 'Wallet',
            status: this.mapStatus(tx.status),
            period: new Date(tx.createdAt).toLocaleDateString(),
            paidAt: new Date(tx.createdAt).toLocaleString(),
            reference: tx.reference || `REF-${tx.id}`,
            description: tx.description || (tx.type === 'CREDIT' ? 'Dépôt de compte' : 'Paiement de mission'),
            timeline: [
              {
                title: 'Transaction enregistrée',
                date: new Date(tx.createdAt).toLocaleString(),
                description: 'La transaction a été reçue par le réseau.'
              },
              {
                title: tx.status === 'PROCESSED' || tx.status === 'SUCCESS' ? 'Confirmé' : 'En attente',
                date: new Date(tx.createdAt).toLocaleString(),
                description: tx.status === 'PROCESSED' || tx.status === 'SUCCESS' ? 'Fonds mis à disposition avec succès.' : 'Traitement côté serveur.'
              }
            ]
          };
        });

        // Update stats
        if (this.paymentHistory.length > 0) {
          this.selectedPayment = this.paymentHistory[0];
          this.paymentStats[0].value = `${totalAmount.toFixed(2)} DT`;
          this.paymentStats[2].value = `${(totalAmount / this.paymentHistory.length).toFixed(2)} DT`;
        }

        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading history:', err);
        this.loading = false;
      }
    });
  }

  printReceipt(): void {
    if (!this.selectedPayment) return;
    
    // Simple print implementation
    const printContent = `
      ============================================
      REÇU DE PAIEMENT - TRUSTEDWORK
      ============================================
      Référence: ${this.selectedPayment.id}
      Date: ${this.selectedPayment.paidAt}
      Client: ${this.selectedPayment.client}
      Montant: ${this.selectedPayment.amount}
      Description: ${this.selectedPayment.description}
      Statut: ${this.selectedPayment.status}
      ============================================
      Merci d'utiliser TrustedWork.
    `;
    
    const printWindow = window.open('', '_blank', 'height=600,width=800');
    if (printWindow) {
      printWindow.document.write('<html><head><title>Reçu ' + this.selectedPayment.id + '</title>');
      printWindow.document.write('<style>body{font-family:monospace;padding:40px;white-space:pre;}</style>');
      printWindow.document.write('</head><body>');
      printWindow.document.write(printContent);
      printWindow.document.write('</body></html>');
      printWindow.document.close();
      printWindow.focus();
      printWindow.print();
    }
  }

  mapStatus(apiStatus: string): PaymentHistoryStatus {
    if (!apiStatus) return 'Processing';
    if (apiStatus.toUpperCase() === 'PROCESSED' || apiStatus.toUpperCase() === 'SUCCESS') return 'Paid';
    if (apiStatus.toUpperCase() === 'PENDING') return 'Processing';
    if (apiStatus.toUpperCase() === 'FAILED') return 'Cancelled';
    return 'Processing';
  }

  get filteredPayments(): PaymentHistoryRecord[] {
    if (this.selectedStatus === 'All') {
      return this.paymentHistory;
    }
    return this.paymentHistory.filter(
      (item) => item.status === this.selectedStatus
    );
  }

  selectStatus(status: PaymentHistoryStatus | 'All'): void {
    this.selectedStatus = status;
    this.syncSelectedPayment();
  }

  selectPayment(payment: PaymentHistoryRecord): void {
    this.selectedPayment = payment;
  }

  getStatusClass(status: PaymentHistoryStatus): string {
    switch (status) {
      case 'Paid':
        return 'status-paid';
      case 'Scheduled':
        return 'status-scheduled';
      case 'Processing':
        return 'status-processing';
      case 'Cancelled':
        return 'status-cancelled';
      default:
        return '';
    }
  }

  trackByLabel(index: number, item: PaymentHistoryStat): string {
    return item.label;
  }

  trackByPayment(index: number, item: PaymentHistoryRecord): string {
    return item.id;
  }

  private syncSelectedPayment(): void {
    const visiblePayments = this.filteredPayments;

    if (!visiblePayments.length) {
       this.selectedPayment = null;
       return;
    }

    if (this.selectedPayment) {
       const stillExists = visiblePayments.some(
         (item) => item.id === this.selectedPayment!.id
       );
       if (!stillExists) {
         this.selectedPayment = visiblePayments[0];
       }
    } else {
       this.selectedPayment = visiblePayments[0];
    }
  }
}