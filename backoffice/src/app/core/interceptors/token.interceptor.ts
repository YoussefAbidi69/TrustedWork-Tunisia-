import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // ⚡ Ne pas injecter le JWT pour les APIs externes (Gemini, Stripe, etc.)
    const isExternalApi = req.url.includes('googleapis.com') ||
                          req.url.includes('generativelanguage') ||
                          req.url.includes('stripe.com');

    const token =
      localStorage.getItem('access_token') ||
      localStorage.getItem('token');

    let request = req;
    if (token && !isExternalApi) {
      request = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Redirection vers login ou gestion d'erreur si nécessaire
        if (error.status === 401) {
          // this.authService.logout();
        }
        return throwError(() => error);
      })
    );
  }
}