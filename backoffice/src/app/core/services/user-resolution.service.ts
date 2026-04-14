import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';

/**
 * Service de résolution userId → nom complet.
 * Appelle GET /identity/users/{id} sur le user-service.
 * Met en cache les résultats pour éviter les appels redondants.
 */
@Injectable({ providedIn: 'root' })
export class UserResolutionService {

  // URL de l'endpoint inter-services (pas de /api — directement /identity)
  private baseUrl = '/api/identity/users';

  // Cache en mémoire : userId → nom complet
  private cache = new Map<number, string>();

  constructor(private http: HttpClient) {}

  /**
   * Retourne le nom complet d'un utilisateur.
   * Si déjà en cache, retourne immédiatement sans appel HTTP.
   * Fallback : "User #<id>" si user-service indisponible.
   */
  getFullName(userId: number): Observable<string> {
    // Vérifier le cache d'abord
    if (this.cache.has(userId)) {
      return of(this.cache.get(userId)!);
    }

    return this.http.get<any>(`${this.baseUrl}/${userId}`).pipe(
      map(user => {
        const firstName = (user.firstName || '').trim();
        const lastName  = (user.lastName  || '').trim();
        return `${firstName} ${lastName}`.trim() || `User #${userId}`;
      }),
      tap(fullName => this.cache.set(userId, fullName)),
      catchError(() => {
        // Fallback propre si user-service indisponible
        const fallback = `User #${userId}`;
        this.cache.set(userId, fallback);
        return of(fallback);
      })
    );
  }

  /**
   * Retourne les initiales à partir d'un nom complet.
   * Utilisé pour les avatars dans les tableaux.
   */
  getInitials(fullName: string): string {
    const parts = fullName.trim().split(' ').filter(p => p.length > 0);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return (parts[0]?.[0] || 'U').toUpperCase();
  }

  /**
   * Vide le cache (utile si on reload les données)
   */
  clearCache(): void {
    this.cache.clear();
  }
}