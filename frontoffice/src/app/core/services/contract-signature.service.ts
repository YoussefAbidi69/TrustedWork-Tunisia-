import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SignatureStatus, SignatureResponse } from '../models/signature.model';

const API = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class ContractSignatureService {

  constructor(private http: HttpClient) {}

  /**
   * 1. Verrouille le contrat (interdit modification/ajout/suppression jalons après).
   */
  finalizeContract(contractId: number): Observable<any> {
    return this.http.patch(`${API}/contracts/${contractId}/finalize`, {});
  }

  /**
   * 2. Envoyer la demande de signature (envoie 2 emails avec 2 liens/token).
   * Le contrat passe en PENDING_SIGNATURE.
   */
  sendSignatureRequests(contractId: number): Observable<any> {
    return this.http.post(`${API}/contracts/${contractId}/signature-requests`, {});
  }


  getPublicRequest(requestId: string, token: string): Observable<any> {
    return this.http.get(`${API}/signing/requests/${requestId}`, {
      params: { token }
    });
  }

  getPublicDocument(requestId: string, token: string): Observable<Blob> {
    return this.http.get(`${API}/signing/requests/${requestId}/document`, {
      params: { token },
      responseType: 'blob'
    });
  }

 
  submitPublicSignature(requestId: string, payload: {
    token: string,
    signatureType: 'DRAWN' | 'TYPED',
    signaturePayload: string
  }): Observable<SignatureResponse> {
    return this.http.post<SignatureResponse>(
      `${API}/signing/requests/${requestId}/sign`,
      payload
    );
  }

  /**
   * Récupérer l'état de signature actuel (pour interface interne).
   */
  getStatus(contractId: number): Observable<SignatureStatus> {
    return this.http.get<SignatureStatus>(`${API}/contracts/${contractId}/signature`);
  }

  /**
   * Télécharger le document PDF du contrat.
   */
  downloadPdf(contractId: number): Observable<Blob> {
    return this.http.get(`${API}/contracts/${contractId}/document`, {
      responseType: 'blob'
    });
  }
}
