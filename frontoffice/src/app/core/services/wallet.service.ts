import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Wallet, Transaction } from '../models/wallet.model';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private apiUrl = 'http://localhost:8083/api/v1/wallets';

  constructor(private http: HttpClient) {}

  getWallet(userCin: string | number): Observable<Wallet> {
    return this.http.get<Wallet>(`${this.apiUrl}/user/${userCin}`);
  }

  getTransactions(userCin: string | number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/user/${userCin}/transactions`);
  }

  credit(userCin: string | number, amount: number): Observable<Wallet> {
    return this.http.post<Wallet>(`${this.apiUrl}/user/${userCin}/credit`, null, {
      params: { amount: amount.toString() }
    });
  }

  createStripeAccount(userCin: string | number, email: string, country: string = 'FR'): Observable<any> {
    return this.http.post(`${this.apiUrl}/stripe/connect/${userCin}`, null, {
      params: { email, country }
    });
  }

  getStripeStatus(userCin: string | number): Observable<any> {
    return this.http.get(`${this.apiUrl}/stripe/status/${userCin}`);
  }

  getOnboardingLink(userCin: string | number): Observable<any> {
    return this.http.get(`${this.apiUrl}/stripe/onboarding/${userCin}`);
  }
}