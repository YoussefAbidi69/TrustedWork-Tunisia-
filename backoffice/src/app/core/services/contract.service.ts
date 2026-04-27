import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Contract } from '../models/contract.model';

@Injectable({
  providedIn: 'root'
})
export class ContractService {

  private endpoint = 'contracts';

  constructor(private api: ApiService) {}

  getAll(
    page: number = 0,
    size: number = 10,
    filters?: { userCin?: string; freelancerCin?: string }
  ): Observable<any> {
    const params: any = { page, size, ...(filters || {}) };
    return this.api.get<any>(this.endpoint, { params });
  }

  getById(id: number): Observable<Contract> {
    return this.api.get<Contract>(`${this.endpoint}/${id}`);
  }

  create(contract: Contract): Observable<Contract> {
    return this.api.post<Contract>(this.endpoint, contract);
  }

  update(id: number, contract: Contract): Observable<Contract> {
    return this.api.put<Contract>(`${this.endpoint}/${id}`, contract);
  }

  updateStatus(id: number, status: string): Observable<Contract> {
    return this.api.patch<Contract>(`${this.endpoint}/${id}/status?status=${status}`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }

  getMyContracts(): Observable<any> {
    return this.api.get<any>(`${this.endpoint}/me`, { params: { size: 100 } });
  }

  getSignedByFreelancer(freelancerCin: string): Observable<Contract[]> {
    return this.api.get<Contract[]>(`${this.endpoint}/freelancer/${freelancerCin}/signed`);
  }
}
