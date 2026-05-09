import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Dispute, DisputeEvidence, DisputeCreateRequest, DisputeResolveRequest } from '../models/dispute.model';

@Injectable({ providedIn: 'root' })
export class DisputeService {
  private endpoint = '/v1/disputes';

  constructor(private api: ApiService) {}

  // ─── CRUD ────────────────────────────────────────────

  /** Lister tous les litiges (de l'utilisateur courant) */
  getAll(): Observable<Dispute[]> {
    return this.api.get<Dispute[]>(this.endpoint);
  }

  /** Créer une dispute */
  create(payload: DisputeCreateRequest): Observable<Dispute> {
    return this.api.post<Dispute>(this.endpoint, payload);
  }

  /** Lister par contrat */
  getByContractId(contractId: number): Observable<Dispute[]> {
    return this.api.get<Dispute[]>(this.endpoint, {
      params: { contractId: contractId.toString() }
    });
  }

  /** Lister par milestone */
  getByMilestoneId(milestoneId: number): Observable<Dispute[]> {
    return this.api.get<Dispute[]>(`${this.endpoint}/milestone/${milestoneId}`);
  }

  /** Détail d'une dispute */
  getById(id: number): Observable<Dispute> {
    return this.api.get<Dispute>(`${this.endpoint}/${id}`);
  }

  // ─── ACTIONS ─────────────────────────────────────────

  /** Répondre (défendeur) */
  respond(disputeId: number, preuvesDefense: string): Observable<Dispute> {
    return this.api.post<Dispute>(`${this.endpoint}/${disputeId}/respond`, { preuvesDefense });
  }

  /** Assigner un arbitre (admin) */
  assign(disputeId: number, arbitreId?: string): Observable<Dispute> {
    return this.api.post<Dispute>(`${this.endpoint}/${disputeId}/assign`, { arbitreId: arbitreId || null });
  }

  /** Résoudre (admin) */
  resolve(disputeId: number, payload: DisputeResolveRequest): Observable<Dispute> {
    return this.api.post<Dispute>(`${this.endpoint}/${disputeId}/resolve`, payload);
  }

  // ─── EVIDENCE (pièces jointes) ───────────────────────

  /** Upload fichier (FormData, pas de Content-Type manuel) */
  uploadEvidence(disputeId: number, file: File): Observable<DisputeEvidence> {
    const fd = new FormData();
    fd.append('file', file);
    return this.api.post<DisputeEvidence>(`${this.endpoint}/${disputeId}/evidence`, fd);
  }

  /** Lister les pièces jointes */
  listEvidence(disputeId: number): Observable<DisputeEvidence[]> {
    return this.api.get<DisputeEvidence[]>(`${this.endpoint}/${disputeId}/evidence`);
  }

  /** Télécharger une pièce jointe avec le JWT actif */
  downloadEvidenceFile(disputeId: number, evidenceId: number): Observable<any> {
    return this.api.get(`${this.endpoint}/${disputeId}/evidence/${evidenceId}/download`, {
      responseType: 'blob',
      observe: 'response'
    });
  }
}
