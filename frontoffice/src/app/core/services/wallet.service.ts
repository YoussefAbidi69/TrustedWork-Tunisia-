import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Wallet, Transaction } from '../models/wallet.model';

import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private endpoint = '/v1/wallets';

  constructor(private api: ApiService) {}

  getWallet(userCin: string | number): Observable<Wallet> {
    return this.api.get<Wallet>(`${this.endpoint}/user/${userCin}`);
  }

  getTransactions(userCin: string | number): Observable<Transaction[]> {
    return this.api.get<Transaction[]>(`${this.endpoint}/user/${userCin}/transactions`);
  }

  credit(userCin: string | number, amount: number): Observable<Wallet> {
    return this.api.post<Wallet>(`${this.endpoint}/user/${userCin}/credit`, null, {
      params: { amount: amount.toString() }
    });
  }

  createStripeAccount(userCin: string | number, email: string, country: string = 'FR'): Observable<any> {
    return this.api.post(`${this.endpoint}/stripe/connect/${userCin}`, null, {
      params: { email, country }
    });
  }

  getStripeStatus(userCin: string | number): Observable<any> {
    return this.api.get(`${this.endpoint}/stripe/status/${userCin}`);
  }

  getOnboardingLink(userCin: string | number): Observable<any> {
    return this.api.get(`${this.endpoint}/stripe/onboarding/${userCin}`);
  }
}
