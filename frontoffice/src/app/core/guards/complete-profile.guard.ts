import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

/**
 * Guard appliqué sur toutes les routes /app/**
 * Si l'utilisateur est connecté mais que son profil Google est incomplet
 * (CIN temporaire négatif), il est redirigé vers /auth/complete-profile.
 */
export const completeProfileGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const userService = inject(UserService);
  const router = inject(Router);

  // Ne pas bloquer les utilisateurs non connectés (authGuard s'en charge)
  if (!authService.isLoggedIn()) {
    return true;
  }

  return userService.checkProfileComplete().pipe(
    map((res: { incomplete: boolean }) => {
      if (res.incomplete) {
        router.navigate(['/auth/complete-profile']);
        return false;
      }
      return true;
    }),
    catchError(() => {
      // En cas d'erreur API, on laisse passer (fail open)
      return of(true);
    })
  );
};
