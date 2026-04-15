import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('token');
  const role = (localStorage.getItem('role') || '').trim();

  if (!token) {
    router.navigate(['/auth/login']);
    return false;
  }

  const isAdmin = role === 'ADMIN' || role === 'ROLE_ADMIN';
  if (!isAdmin) {
    localStorage.clear();
    router.navigate(['/auth/login']);
    return false;
  }

  return true;
};