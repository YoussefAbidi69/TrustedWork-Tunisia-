import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { WalletComponent } from './wallet/wallet.component';
import { TransactionsComponent } from './transactions/transactions.component';
import { EscrowComponent } from './escrow/escrow.component';
import { PaymentsHistoryComponent } from './payments-history/payments-history.component';
import { CheckoutComponent } from './checkout/checkout';
import { PaymentResultComponent } from './payment-result/payment-result';
import { PaymentListComponent } from './payment/payment-list/payment-list';

const routes: Routes = [
  {
    path: 'wallet',
    component: WalletComponent
  },
  {
    path: 'payment-list',
    component: PaymentListComponent
  },
  {
    path: 'transactions',
    component: TransactionsComponent
  },
  {
    path: 'escrow',
    component: EscrowComponent
  },
  {
    path: 'payments-history',
    component: PaymentsHistoryComponent
  },
  {
    path: 'checkout/:contractId',
    component: CheckoutComponent
  },
  {
    path: 'payment-result',
    component: PaymentResultComponent
  },
  {
    path: '',
    redirectTo: 'wallet',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FinanceRoutingModule { }