import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('token');

  if (!token) {
    // Redirection vers la landing page frontoffice au lieu de /auth/login
    window.location.href = 'http://localhost:4200';
    return false;
  }

  return true;
};