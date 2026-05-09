import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class StripeService {
  private endpoint = '/v1/payments';
  private stripe: Stripe | null = null;
  private simulationMode = false;

  constructor(private api: ApiService) {
    this.initStripe();
  }

  private async initStripe() {
    if (!this.simulationMode) {
      this.stripe = await loadStripe('pk_test_51P8y... (remplacez par votre clé si besoin)');
    }
  }

  createCheckoutSession(contractId: number, amount: number, successUrl: string, cancelUrl: string): Observable<any> {
    return this.api.post(`${this.endpoint}/create-checkout-session`, null, {
      params: { 
        contractId: contractId.toString(), 
        amount: amount.toString(),
        successUrl,
        cancelUrl
      }
    });
  }

  createPaymentIntent(contractId: number, email: string): Observable<any> {
    return this.api.post(`${this.endpoint}/create-intent`, null, {
      params: { contractId: contractId.toString(), email }
    });
  }

  confirmPayment(paymentIntentId: string, contractId: number): Observable<any> {
    return this.api.post(`${this.endpoint}/confirm`, null, {
      params: { paymentIntentId, contractId: contractId.toString() }
    });
  }

  async confirmStripePayment(clientSecret: string): Promise<any> {
    if (this.simulationMode) {
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

  async redirectToCheckout(sessionId: string): Promise<void> {
    if (this.simulationMode) {
      console.log('Mode Simulation : redirection simulée vers Stripe');
      return;
    }
    
    if (!this.stripe) {
      throw new Error('Stripe n\'a pas été initialisé correctement');
    }

    const { error } = await (this.stripe as any).redirectToCheckout({ sessionId });
    
    if (error) {
      console.error('Erreur de redirection Stripe:', error);
      throw error;
    }
  }

  getPaymentStatus(paymentIntentId: string): Observable<any> {
    return this.api.get(`${this.endpoint}/status/${paymentIntentId}`);
  }
}
