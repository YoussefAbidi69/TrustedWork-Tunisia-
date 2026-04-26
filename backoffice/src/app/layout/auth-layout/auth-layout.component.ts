import { Component } from '@angular/core';

@Component({
  selector: 'app-auth-layout',
  templateUrl: './auth-layout.component.html',
  styleUrls: ['./auth-layout.component.css']
})
export class AuthLayoutComponent {
  modules = [
    { icon: 'fa-user-shield',   label: 'Identity & KYC' },
    { icon: 'fa-briefcase',     label: 'Freelancer Profiles' },
    { icon: 'fa-file-contract', label: 'Contracts' },
    { icon: 'fa-star',          label: 'Reviews & Reputation' },
    { icon: 'fa-building',      label: 'Agencies' },
    { icon: 'fa-calendar-days', label: 'Events' },
    { icon: 'fa-user-tie',      label: 'Recruitment' },
  ];
}