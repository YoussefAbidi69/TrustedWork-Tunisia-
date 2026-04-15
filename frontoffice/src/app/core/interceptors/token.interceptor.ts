import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  const isAuthRequest =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register');

  if (token && !isAuthRequest) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });

    console.log('[Interceptor] Token ajouté à la requête :', req.url);
    return next(clonedRequest);
  }

  return next(req);
};