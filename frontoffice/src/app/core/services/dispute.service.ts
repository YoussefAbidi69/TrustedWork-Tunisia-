import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Dispute, DisputeEvidence, DisputeCreateRequest, DisputeResolveRequest } from '../models/dispute.model';

const API = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class DisputeService {

  constructor(private http: HttpClient) {}

  // ─── CRUD ────────────────────────────────────────────

  /** Lister tous les litiges (de l'utilisateur courant) */
  getAll(): Observable<Dispute[]> {
    return this.http.get<Dispute[]>(`${API}/disputes`);
  }

  /** Créer une dispute */
  create(payload: DisputeCreateRequest): Observable<Dispute> {
    return this.http.post<Dispute>(`${API}/disputes`, payload);
  }

  /** Lister par contrat */
  getByContractId(contractId: number): Observable<Dispute[]> {
    return this.http.get<Dispute[]>(`${API}/disputes`, {
      params: { contractId: contractId.toString() }
    });
  }

  /** Lister par milestone */
  getByMilestoneId(milestoneId: number): Observable<Dispute[]> {
    return this.http.get<Dispute[]>(`${API}/disputes/milestone/${milestoneId}`);
  }

  /** Détail d'une dispute */
  getById(id: number): Observable<Dispute> {
    return this.http.get<Dispute>(`${API}/disputes/${id}`);
  }

  // ─── ACTIONS ─────────────────────────────────────────

  /** Répondre (défendeur) */
  respond(disputeId: number, preuvesDefense: string): Observable<Dispute> {
    return this.http.post<Dispute>(`${API}/disputes/${disputeId}/respond`, { preuvesDefense });
  }

  /** Assigner un arbitre (admin) */
  assign(disputeId: number, arbitreId?: string): Observable<Dispute> {
    return this.http.post<Dispute>(`${API}/disputes/${disputeId}/assign`, { arbitreId: arbitreId || null });
  }

  /** Résoudre (admin) */
  resolve(disputeId: number, payload: DisputeResolveRequest): Observable<Dispute> {
    return this.http.post<Dispute>(`${API}/disputes/${disputeId}/resolve`, payload);
  }

  // ─── EVIDENCE (pièces jointes) ───────────────────────

  /** Upload fichier (FormData, pas de Content-Type manuel) */
  uploadEvidence(disputeId: number, file: File): Observable<DisputeEvidence> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<DisputeEvidence>(`${API}/disputes/${disputeId}/evidence`, fd);
  }

  /** Lister les pièces jointes */
  listEvidence(disputeId: number): Observable<DisputeEvidence[]> {
    return this.http.get<DisputeEvidence[]>(`${API}/disputes/${disputeId}/evidence`);
  }

  /** Télécharger une pièce jointe avec le JWT actif */
  downloadEvidenceFile(disputeId: number, evidenceId: number): Observable<any> {
    return this.http.get(`${API}/disputes/${disputeId}/evidence/${evidenceId}/download`, {
      responseType: 'blob',
      observe: 'response'
    });
  }
}
