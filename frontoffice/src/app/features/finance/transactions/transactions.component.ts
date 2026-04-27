import { Component, OnInit } from '@angular/core';
import { WalletService } from '../../../core/services/wallet.service';
import { UserService } from '../../../core/services/user.service';
import { Transaction } from '../../../core/models/wallet.model';
import { ConnectedUserResponse } from '../../../core/models/user.model';

type TransactionStatus = 'Completed' | 'Pending' | 'Escrow' | 'Failed';
type TransactionDirection = 'Incoming' | 'Outgoing';

interface TransactionStat {
  label: string;
  value: string;
  caption: string;
}

interface TransactionRecord {
  id: string;
  title: string;
  direction: TransactionDirection;
  amount: string;
  netAmount: string;
  status: TransactionStatus;
  date: string;
  contract: string;
  client: string;
  paymentMethod: string;
  fee: string;
  description: string;
}

@Component({
  selector: 'app-transactions',
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.css']
})
export class TransactionsComponent implements OnInit {
  transactionStats: TransactionStat[] = [
    {
      label: 'Volume total',
      value: '0 DT',
      caption: 'Flux financiers sur cette période'
    },
    {
      label: 'Fonds entrants',
      value: '0 DT',
      caption: 'Paiements, déblocages et revenus'
    },
    {
      label: 'Fonds sortants',
      value: '0 DT',
      caption: 'Frais, remboursements et retraits'
    }
  ];

  readonly statusFilters: Array<TransactionStatus | 'All'> = [
    'All',
    'Completed',
    'Pending',
    'Escrow',
    'Failed'
  ];

  readonly directionFilters: Array<TransactionDirection | 'All'> = [
    'All',
    'Incoming',
    'Outgoing'
  ];

  selectedStatus: TransactionStatus | 'All' = 'All';
  selectedDirection: TransactionDirection | 'All' = 'All';

  transactions: TransactionRecord[] = [];
  loading: boolean = true;

  constructor(
    private walletService: WalletService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user: ConnectedUserResponse) => {
        if (user && user.cin) {
          this.loadRealTransactions(user.cin.toString(), user);
        } else {
          this.loading = false;
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadRealTransactions(userCin: string, user: ConnectedUserResponse): void {
    this.walletService.getTransactions(userCin).subscribe({
      next: (txs: Transaction[]) => {
        const rawTxs = Array.isArray(txs) ? txs : [];
        let totalIncoming = 0;
        let totalOutgoing = 0;

        this.transactions = rawTxs.map(tx => {
          const isCredit = tx.type === 'CREDIT';
          if (isCredit) totalIncoming += tx.montant;
          if (!isCredit) totalOutgoing += tx.montant;

          let contractLabel = 'Contrat N/A';
          if (tx.description && tx.description.includes('Contrat')) {
             contractLabel = tx.description.split(' - ')[0] || 'Contrat lié';
          }
          if (tx.description && tx.description.includes('jalon')) {
             contractLabel = 'Jalon · ' + tx.reference;
          }

          return {
            id: tx.reference || `TX-${tx.id}`,
            title: tx.description || (isCredit ? 'Paiement reçu' : 'Frais / Paiement sortant'),
            direction: isCredit ? 'Incoming' : 'Outgoing',
            amount: `${isCredit ? '+' : '-'}${tx.montant} DT`,
            netAmount: `${tx.montant} DT`,
            status: this.mapStatus(tx.status),
            date: new Date(tx.createdAt).toLocaleDateString() + ' ' + new Date(tx.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}),
            contract: contractLabel,
            client: user.nom ? `${user.prenom} ${user.nom}` : 'Client / Projet',
            paymentMethod: 'Wallet interne',
            fee: '0 DT',
            description: tx.description || 'Paiement de transaction rattaché au contrat.'
          };
        });

        this.transactionStats[0].value = `${(totalIncoming + totalOutgoing).toFixed(2)} DT`;
        this.transactionStats[1].value = `${totalIncoming.toFixed(2)} DT`;
        this.transactionStats[2].value = `${totalOutgoing.toFixed(2)} DT`;

        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading transactions', err);
        this.loading = false;
      }
    });
  }

  mapStatus(apiStatus: string): TransactionStatus {
    if (!apiStatus) return 'Pending';
    if (apiStatus.toUpperCase() === 'PROCESSED' || apiStatus.toUpperCase() === 'SUCCESS') return 'Completed';
    if (apiStatus.toUpperCase() === 'PENDING') return 'Pending';
    if (apiStatus.toUpperCase() === 'FAILED') return 'Failed';
    return 'Pending';
  }

  get filteredTransactions(): TransactionRecord[] {
    return this.transactions.filter((transaction) => {
      const matchesStatus =
        this.selectedStatus === 'All' || transaction.status === this.selectedStatus;

      const matchesDirection =
        this.selectedDirection === 'All' ||
        transaction.direction === this.selectedDirection;

      return matchesStatus && matchesDirection;
    });
  }

  selectStatus(status: TransactionStatus | 'All'): void {
    this.selectedStatus = status;
  }

  selectDirection(direction: TransactionDirection | 'All'): void {
    this.selectedDirection = direction;
  }

  getStatusClass(status: TransactionStatus): string {
    switch (status) {
      case 'Completed': return 'badge bg-success bg-opacity-10 text-success';
      case 'Pending': return 'badge bg-warning bg-opacity-10 text-warning';
      case 'Escrow': return 'badge bg-primary bg-opacity-10 text-primary';
      case 'Failed': return 'badge bg-danger bg-opacity-10 text-danger';
      default: return 'badge bg-secondary bg-opacity-10 text-secondary';
    }
  }

  getDirectionClass(direction: TransactionDirection): string {
    switch (direction) {
      case 'Incoming': return 'text-success';
      case 'Outgoing': return 'text-danger';
      default: return '';
    }
  }

  trackByLabel(index: number, item: TransactionStat): string {
    return item.label;
  }

  trackByTransaction(index: number, item: TransactionRecord): string {
    return item.id;
  }
}