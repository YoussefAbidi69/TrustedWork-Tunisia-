import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-payment-result',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './payment-result.html',
  styleUrls: ['./payment-result.css']
})
export class PaymentResultComponent implements OnInit {
  status: 'success' | 'cancel' = 'success';
  contractId: number | null = null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['status'] === 'cancel') {
        this.status = 'cancel';
      } else {
        this.status = 'success';
      }
      this.contractId = params['contractId'] ? Number(params['contractId']) : null;
    });
  }
}