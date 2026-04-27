export interface FinancialMetrics {
  contractId: number;
  milestonesCount: number;
  storedMontantTotal: number;
  computedMontantTotal: number;
  delta: number;
  mismatch: boolean;
}
