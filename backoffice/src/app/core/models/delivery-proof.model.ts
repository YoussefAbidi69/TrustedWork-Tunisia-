export interface DeliveryProofSubmitRequest {
  fichiers?: string;     // ex: URLs separees par virgule
  lienDemo?: string;
  repoGit?: string;
  commentaire?: string;
  hashMD5?: string;
}

export interface DeliveryProofResponse {
  id: number;
  milestoneId: number;
  fichiers: string | null;
  lienDemo: string | null;
  repoGit: string | null;
  commentaire: string | null;
  hashMD5: string | null;
  submittedAt: string | null;
  status: 'SUBMITTED' | 'APPROVED' | 'REJECTED';
  approvedAt: string | null;
  approvedByCin: number | null;
}
