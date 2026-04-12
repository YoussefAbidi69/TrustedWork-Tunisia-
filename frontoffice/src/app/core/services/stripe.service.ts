import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StripeService {
  private apiUrl = 'http://localhost:8083/api/v1/payments';
  private stripe: Stripe | null = null;

  constructor(private http: HttpClient) {
    if (!environment.simulationMode) {
      this.initStripe();
    }
  }

  private async initStripe() {
    this.stripe = await loadStripe(environment.stripePublicKey);
  }

  createPaymentIntent(contractId: number, email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/create-intent`, null, {
      params: { contractId: contractId.toString(), email }
    });
  }

  confirmPayment(paymentIntentId: string, contractId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/confirm`, null, {
      params: { paymentIntentId, contractId: contractId.toString() }
    });
  }

  async confirmStripePayment(clientSecret: string) {
    if (environment.simulationMode) {
      console.log('🔧 SIMULATION MODE: Simulating successful payment');
      return { error: null, paymentIntent: { status: 'succeeded' } };
    }
    
    if (!this.stripe) {
      await this.initStripe();
    }
    return this.stripe!.confirmPayment({
      clientSecret,
      confirmParams: {
        return_url: `${window.location.origin}/payment/result`
      }
    });
  }

  getPaymentStatus(paymentIntentId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/status/${paymentIntentId}`);
  }
}