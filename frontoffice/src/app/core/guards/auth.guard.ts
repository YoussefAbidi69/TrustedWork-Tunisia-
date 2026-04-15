import { CanActivateChildFn, CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

function checkAuthentication(stateUrl: string): boolean | UrlTree {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: stateUrl }
  });
}

export const authGuard: CanActivateFn = (_route, state) => {
  return checkAuthentication(state.url);
};

export const authChildGuard: CanActivateChildFn = (_childRoute, state) => {
  return checkAuthentication(state.url);
};