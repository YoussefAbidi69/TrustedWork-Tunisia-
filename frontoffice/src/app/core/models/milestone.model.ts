export interface Milestone {
    id?: number;
    contractId: number;
    ordre?: number;
    titre: string;
    description: string;
    montant: number;
    deadline: Date | string | null;
    startedAt?: Date | string | null;
    submittedAt?: Date | string | null;
    validatedAt?: Date | string | null;
    status: string;
    rejectionReason?: string;
}