import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Milestone } from '../models/milestone.model';
import { DeliveryProofResponse, DeliveryProofSubmitRequest } from '../models/delivery-proof.model';

@Injectable({
  providedIn: 'root'
})
export class MilestoneService {

  private endpoint = 'milestones';

  constructor(private api: ApiService) {}

  getAll(page: number = 0, size: number = 10): Observable<any> {
    return this.api.get<any>(this.endpoint, { page, size });
  }

  getById(id: number): Observable<Milestone> {
    return this.api.get<Milestone>(`${this.endpoint}/${id}`);
  }

  getByContractId(contractId: number): Observable<Milestone[]> {
    return this.api.get<Milestone[]>(`${this.endpoint}/contract/${contractId}`);
  }

  create(milestone: Milestone): Observable<Milestone> {
    return this.api.post<Milestone>(this.endpoint, milestone);
  }

  update(id: number, milestone: Milestone): Observable<Milestone> {
    return this.api.put<Milestone>(`${this.endpoint}/${id}`, milestone);
  }

  updateStatus(id: number, status: string): Observable<Milestone> {
    return this.api.patch<Milestone>(`${this.endpoint}/${id}/status?status=${status}`);
  }

  start(id: number): Observable<Milestone> {
    return this.api.post<Milestone>(`${this.endpoint}/${id}/start`, {});
  }

  submit(id: number, proof?: DeliveryProofSubmitRequest): Observable<Milestone> {
    return this.api.post<Milestone>(`${this.endpoint}/${id}/submit`, proof ?? {});
  }

  getDeliveryProof(id: number): Observable<DeliveryProofResponse> {
    return this.api.get<DeliveryProofResponse>(`${this.endpoint}/${id}/delivery-proof`);
  }

  approve(id: number): Observable<Milestone> {
    return this.api.post<Milestone>(`${this.endpoint}/${id}/approve`, {});
  }

  reject(id: number, reason: string, newDeadline?: string): Observable<Milestone> {
    const params: any = { reason };
    if (newDeadline) {
      params.newDeadline = newDeadline;
    }
    return this.api.post<Milestone>(`${this.endpoint}/${id}/reject`, {}, params);
  }

  updateRejectedDeadline(id: number, newDeadline: string): Observable<Milestone> {
    return this.api.patch<Milestone>(`${this.endpoint}/${id}/deadline`, {}, { newDeadline });
  }

  resubmit(id: number): Observable<Milestone> {
    return this.api.post<Milestone>(`${this.endpoint}/${id}/submit`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
