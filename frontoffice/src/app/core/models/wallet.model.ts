export interface Wallet {
  id: number;
  userCin: string;
  balance: number;
  totalEarned: number;
  totalSpent: number;
  totalCommissionPaid: number;
  stripeAccountId: string;
  stripeAccountStatus: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface Transaction {
  id: number;
  reference: string;
  type: string;
  montant: number;
  description: string;
  status: string;
  createdAt: Date;
}