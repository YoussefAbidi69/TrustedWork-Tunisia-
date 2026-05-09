export interface Contract {
  id?: number;
  reference: string;
  clientCin: string;
  freelancerCin: string;
  clientWalletCin: string;
  freelancerWalletCin: string;
  projectId: number | null;
  projectTitle: string;
  description: string;
  montantTotal: number;
  slaFreelancerHeures: number;
  slaClientJours: number;
  dateSignature?: Date | string;
  dateDebut: Date | string;
  dateFin: Date | string;
  commissionRate: number;
  status: string;
  cancelledAt?: Date | string;
  cancellationReason?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}