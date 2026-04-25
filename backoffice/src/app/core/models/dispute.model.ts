export interface Dispute {
  id?: number;
  reference?: string;
  contractId: number;
  milestoneId?: number | null;
  motif: string;
  preuvesPlaignant?: string;
  preuvesDefense?: string;
  status: string;
  decision?: string;
  montantRembourse?: number;
  montantLibere?: number;
  arbitreId?: string;
  openedAt?: string;
  resolvedAt?: string;
}

export interface DisputeEvidence {
  id?: number;
  disputeId: number;
  fileName: string;
  originalFilename?: string;
  fileType?: string;
  contentType?: string;
  sizeBytes?: number;
  uploadedAt?: string;
  uploadedBy?: string;
}

export interface DisputeCreateRequest {
  contractId: number;
  milestoneId: number | null;
  motif: string;
  preuvesPlaignant?: string;
}

export interface DisputeResolveRequest {
  status: 'RESOLVED_CLIENT' | 'RESOLVED_FREELANCER' | 'SPLIT' | 'DISMISSED';
  decision: string;
  montantRembourse: number;
  montantLibere: number;
}

export interface DisputeAiRecommendation {
  disputeId: number;
  suggestedDecision: 'RESOLVED_CLIENT' | 'RESOLVED_FREELANCER' | 'SPLIT' | 'DISMISSED';
  confidenceScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  summary: string;
  reasoning: string;
  suggestedMontantRembourse: number;
  suggestedMontantLibere: number;
  keyFactors: string[];
  generatedAt: string;
  fallback: boolean;
}
