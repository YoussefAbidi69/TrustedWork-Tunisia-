import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FinanceRoutingModule } from './finance-routing.module';
import { WalletComponent } from './wallet/wallet.component';
import { TransactionsComponent } from './transactions/transactions.component';
import { EscrowComponent } from './escrow/escrow.component';
import { PaymentsHistoryComponent } from './payments-history/payments-history.component';


import { CheckoutComponent } from './checkout/checkout';
import { PaymentResultComponent } from './payment-result/payment-result';
import { FormsModule } from '@angular/forms';

@NgModule({
  declarations: [
    WalletComponent,
    TransactionsComponent,
    EscrowComponent,
    PaymentsHistoryComponent,
    CheckoutComponent,
    PaymentResultComponent
  ],
  imports: [
    CommonModule,
    FinanceRoutingModule,
    FormsModule
  ]
})
export class FinanceModule { }
