import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

const CACHE_KEY = 'profile_complete_checked';

export const completeProfileGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const userService = inject(UserService);
  const router      = inject(Router);

  if (!authService.isLoggedIn()) return true;

  // Résultat mis en cache dans sessionStorage — persiste entre les navigations
  // mais est effacé à la fermeture du navigateur (contrairement à localStorage)
  const cached = sessionStorage.getItem(CACHE_KEY);
  if (cached !== null) {
    // 'true' = profil complet → accès autorisé
    return cached === 'true' ? true : router.createUrlTree(['/auth/complete-profile']);
  }

  return userService.checkProfileComplete().pipe(
    map((res: { incomplete: boolean }) => {
      const isComplete = !res.incomplete;
      sessionStorage.setItem(CACHE_KEY, String(isComplete));

      if (!isComplete) {
        router.navigate(['/auth/complete-profile']);
        return false;
      }
      return true;
    }),
    catchError(() => {
      // En cas d'erreur (400/403) — considérer complet, ne pas bloquer
      sessionStorage.setItem(CACHE_KEY, 'true');
      return of(true);
    })
  );
};