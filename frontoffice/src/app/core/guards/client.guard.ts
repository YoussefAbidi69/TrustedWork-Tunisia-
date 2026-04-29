import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const clientGuard: CanActivateFn = (): boolean | UrlTree => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.getCurrentAuthUser();
  if (user?.role?.toUpperCase() === 'CLIENT') {
    return true;
  }
  return router.createUrlTree(['/app/dashboard']);
};
