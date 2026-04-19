import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface AIContractDraft {
  projectTitle: string;
  description: string;
  montantTotal: number;
  dateDebut: string;
  dateFin: string;
  slaFreelancerHeures: number;
  slaClientJours: number;
}

export interface AIMilestoneDraft {
  titre: string;
  description: string;
  montant: number;
  deadline: string;
}

export interface AIMilestoneContext {
  prompt: string;
  contractTitle: string;
  contractDescription: string;
  remainingBudget: number;
  contractDeadline: string;
  existingMilestones: string[];
}

@Injectable({ providedIn: 'root' })
export class AIService {
  constructor(private api: ApiService) { }

  generateContractDraft(prompt: string): Observable<AIContractDraft> {
    return this.api.post<AIContractDraft>('contracts/ai/generate', { prompt });
  }

  generateMilestoneDraft(context: AIMilestoneContext): Observable<AIMilestoneDraft> {
    return this.api.post<AIMilestoneDraft>('milestones/ai/generate', context);
  }
} 
