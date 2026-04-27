import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { SignatureStatus, SignatureResponse } from '../models/signature.model';

@Injectable({ providedIn: 'root' })
export class ContractSignatureService {

  constructor(private api: ApiService) {}

  /**
   * 1. Verrouille le contrat (interdit modification/ajout/suppression jalons après).
   */
  finalizeContract(contractId: number): Observable<any> {
    return this.api.patch(`/v1/contracts/${contractId}/finalize`, {});
  }

  /**
   * 2. Envoyer la demande de signature (envoie 2 emails avec 2 liens/token).
   * Le contrat passe en PENDING_SIGNATURE.
   */
  sendSignatureRequests(contractId: number): Observable<any> {
    return this.api.post(`/v1/contracts/${contractId}/signature-requests`, {});
  }


  getPublicRequest(requestId: string, token: string): Observable<any> {
    return this.api.get(`/v1/signing/requests/${requestId}`, {
      params: { token }
    });
  }

  getPublicDocument(requestId: string, token: string): Observable<Blob> {
    return this.api.get(`/v1/signing/requests/${requestId}/document`, {
      params: { token },
      responseType: 'blob'
    });
  }

 
  submitPublicSignature(requestId: string, payload: {
    token: string,
    signatureType: 'DRAWN' | 'TYPED',
    signaturePayload: string
  }): Observable<SignatureResponse> {
    return this.api.post<SignatureResponse>(
      `/v1/signing/requests/${requestId}/sign`,
      payload
    );
  }

  /**
   * Récupérer l'état de signature actuel (pour interface interne).
   */
  getStatus(contractId: number): Observable<SignatureStatus> {
    return this.api.get<SignatureStatus>(`/v1/contracts/${contractId}/signature`);
  }

  /**
   * Télécharger le document PDF du contrat.
   */
  downloadPdf(contractId: number): Observable<Blob> {
    return this.api.get(`/v1/contracts/${contractId}/document`, {
      responseType: 'blob'
    });
  }
}
