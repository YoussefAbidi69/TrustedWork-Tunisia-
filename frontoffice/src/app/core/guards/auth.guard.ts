import { CanActivateChildFn, CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

function checkAuthentication(): boolean | UrlTree {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  return router.createUrlTree(['/auth/login']);
}

export const authGuard: CanActivateFn = (_route, _state) => {
  return checkAuthentication();
};

export const authChildGuard: CanActivateChildFn = (_childRoute, _state) => {
  return checkAuthentication();
};