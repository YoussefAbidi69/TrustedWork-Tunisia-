import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Correction : La méthode dans ton AuthService est isLoggedIn()
  if (authService.isLoggedIn()) {
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};