import { CanActivateChildFn, CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

function checkAuthentication(): boolean | UrlTree {
  const authService = inject(AuthService);
  const router      = inject(Router);

  // Non connecté → login
  if (!authService.isLoggedIn()) {
    return router.createUrlTree(['/auth/login']);
  }

  // Admin connecté sur le frontoffice → redirige vers backoffice
  const user = authService.getCurrentAuthUser();
  if (user?.role === 'ADMIN') {
    window.location.href = 'http://localhost:4201';
    return false;
  }

  return true;
}

export const authGuard: CanActivateFn = (_route, _state) => {
  return checkAuthentication();
};

export const authChildGuard: CanActivateChildFn = (_childRoute, _state) => {
  return checkAuthentication();
};