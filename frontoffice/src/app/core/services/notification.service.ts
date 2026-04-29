import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth.service';
import { UserService } from './user.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {

  private readonly BASE_URL = '/api/notifications';

  private readonly countSubject   = new BehaviorSubject<number>(0);
  private readonly messagesSubject = new BehaviorSubject<any[]>([]);

  count$    = this.countSubject.asObservable();
  messages$ = this.messagesSubject.asObservable();

  private stompClient: any = null;
  private serviceAvailable = true;

  constructor(
    private readonly authService: AuthService,
    private readonly http: HttpClient,
    private readonly userService: UserService
  ) {}

  connect(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user?.userId) return;
    if (this.stompClient?.active) return;

    const userId = user.userId;
    this.chargerNotificationsPersistees(userId);
    this.connecterWebSocket(userId);

    // Initialiser le polling des notifications contrats (ms-contract-servicee)
    this.userService.getCurrentDashboardUser().subscribe({
      next: (u) => {
        if (u && u.cin) {
          this.pollContractNotifications(String(u.cin));
        }
      },
      error: () => {}
    });
  }

  private chargerNotificationsPersistees(userId: number): void {
    this.http.get<any[]>(`${this.BASE_URL}/user/${userId}/unread`).subscribe({
      next: (notifications) => {
        this.serviceAvailable = true;
        if (notifications?.length) {
          this.messagesSubject.next(notifications);
          this.countSubject.next(notifications.length);
          // Son discret au chargement si notifications en attente
          this.jouerSon();
          console.log(`[NotificationService] ${notifications.length} notification(s) chargée(s)`);
        }
      },
      error: (err: any) => {
        // 503 = service hors ligne, 0 = réseau, ne pas afficher d'erreur
        if (err.status === 503 || err.status === 0) {
          this.serviceAvailable = false;
          console.warn('[NotificationService] Service de notifications indisponible (mode dégradé).');
        } else {
          console.warn('[NotificationService] Erreur chargement notifications :', err.status);
        }
      }
    });
  }

  // ─── NOTIFICATIONS CONTRATS (ms-contract-servicee) ───
  private pollContractNotifications(cin: string): void {
    const fetchContractNotifs = () => {
      const headers = new HttpHeaders().set('X-User-Cin', cin);
      this.http.get<any[]>('/api/v1/contracts/notifications/unread', { headers }).subscribe({
        next: (notifications) => {
          if (notifications && notifications.length > 0) {
            // Merge with existing avoiding duplicates by ID
            const current = this.messagesSubject.getValue();
            let newNotifs = 0;
            
            notifications.forEach(n => {
              const isDuplicate = current.some(c => c.id === n.id && c.title === n.title);
              if (!isDuplicate) {
                current.unshift(n);
                newNotifs++;
              }
            });

            if (newNotifs > 0) {
              this.messagesSubject.next([...current]);
              this.countSubject.next(this.countSubject.getValue() + newNotifs);
              this.jouerSon();
              console.log(`[NotificationService] ${newNotifs} nouvelle(s) notif(s) contrat(s) !`);
            }
          }
        },
        error: () => {} // Silencieux si ms-contract-servicee n'est pas dispo
      });
    };

    // Premier appel immédiat
    fetchContractNotifs();

    // Puis toutes les 15 secondes
    setInterval(fetchContractNotifs, 15000);
  }

  private connecterWebSocket(userId: number): void {
    if (!this.serviceAvailable) {
      // Réessayer après 30s si le service était indisponible
      setTimeout(() => {
        const user = this.authService.getCurrentAuthUser();
        if (user?.userId) {
          this.chargerNotificationsPersistees(user.userId);
          this.connecterWebSocket(user.userId);
        }
      }, 30000);
      return;
    }

    const topic = `/topic/user/${userId}/notifications`;

    import('@stomp/stompjs').then(({ Client }) => {
      this.stompClient = new Client({
        brokerURL: 'ws://localhost:8082/ws',
        reconnectDelay: 15000,

        onConnect: () => {
          console.log('[NotificationService] WebSocket connecté → topic :', topic);

          this.stompClient.subscribe(topic, (frame: { body: string }) => {
            try {
              const notification = JSON.parse(frame.body);
              const current = this.messagesSubject.getValue();
              this.messagesSubject.next([notification, ...current]);
              this.countSubject.next(this.countSubject.getValue() + 1);

              // Son de notification temps réel
              this.jouerSon();
            } catch (e) {
              // Ignorer les erreurs de parsing
            }
          });
        },

        onStompError: () => {
          // WebSocket indisponible — silencieux
        },

        onWebSocketError: () => {
          // Port 8082 non accessible — silencieux
        }
      });

      try {
        this.stompClient.activate();
      } catch {
        // Ignorer si le WebSocket échoue
      }
    }).catch(() => {
      // @stomp/stompjs non disponible
    });
  }

  /**
   * Joue un son "ding" via Web Audio API — aucun fichier externe requis.
   */
  private jouerSon(): void {
    try {
      const ctx  = new AudioContext();
      //  Resume obligatoire si le contexte est suspendu (politique Chrome autoplay)
      ctx.resume().then(() => {
        const osc  = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(880,  ctx.currentTime);
        osc.frequency.setValueAtTime(1100, ctx.currentTime + 0.1);
        gain.gain.setValueAtTime(0.3, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.4);
      });
    } catch {
      // Son non disponible — ne pas bloquer l'application
    }
  }

  resetCount(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user?.userId) return;

    this.countSubject.next(0);

    // Marquer lues côté profile service
    this.http.put(`${this.BASE_URL}/user/${user.userId}/read-all`, {}).subscribe({
      next: () => console.log('[NotificationService] Notifications profil marquées comme lues'),
      error: () => {} // Silencieux si le service est indisponible
    });

    // Marquer lues côté contract service
    this.userService.getCurrentDashboardUser().subscribe({
      next: (u) => {
        if (u && u.cin) {
          const headers = new HttpHeaders().set('X-User-Cin', String(u.cin));
          this.http.put('/api/v1/contracts/notifications/mark-all-read', {}, { headers }).subscribe({
            next: () => console.log('[NotificationService] Notifications contrats marquées comme lues'),
            error: () => {}
          });
        }
      },
      error: () => {}
    });
  }

  getMessages(): any[] {
    return this.messagesSubject.getValue();
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
  }
}
