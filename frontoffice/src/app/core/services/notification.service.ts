import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {

  private readonly BASE_URL = 'http://localhost:8082/api/notifications';

  private readonly countSubject   = new BehaviorSubject<number>(0);
  private readonly messagesSubject = new BehaviorSubject<any[]>([]);

  count$    = this.countSubject.asObservable();
  messages$ = this.messagesSubject.asObservable();

  private stompClient: any = null;

  constructor(
    private readonly authService: AuthService,
    private readonly http: HttpClient
  ) {}

  connect(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user?.userId) return;
    if (this.stompClient?.active) return;

    const userId = user.userId;
    this.chargerNotificationsPersistees(userId);
    this.connecterWebSocket(userId);
  }

  private chargerNotificationsPersistees(userId: number): void {
    this.http.get<any[]>(`${this.BASE_URL}/user/${userId}/unread`).subscribe({
      next: (notifications) => {
        if (notifications?.length) {
          this.messagesSubject.next(notifications);
          this.countSubject.next(notifications.length);
          // Son discret au chargement si notifications en attente
          this.jouerSon();
          console.log(`[NotificationService] ${notifications.length} notification(s) chargée(s)`);
        }
      },
      error: (err) => console.error('[NotificationService] Erreur chargement :', err)
    });
  }

  private connecterWebSocket(userId: number): void {
    const topic = `/topic/user/${userId}/notifications`;

    import('@stomp/stompjs').then(({ Client }) => {
      this.stompClient = new Client({
        brokerURL: 'ws://localhost:8082/ws',
        reconnectDelay: 5000,

        onConnect: () => {
          console.log('[NotificationService] WebSocket connecté → topic :', topic);

          this.stompClient.subscribe(topic, (frame: { body: string }) => {
            try {
              const notification = JSON.parse(frame.body);
              console.log('[NotificationService] Notification reçue :', notification);

              const current = this.messagesSubject.getValue();
              this.messagesSubject.next([notification, ...current]);
              this.countSubject.next(this.countSubject.getValue() + 1);

              // Son de notification temps réel
              this.jouerSon();
            } catch (e) {
              console.error('[NotificationService] Erreur parsing :', e);
            }
          });
        },

        onStompError: (frame: any) => {
          console.error('[NotificationService] Erreur STOMP :', frame);
        }
      });

      this.stompClient.activate();
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
  } catch (e) {
    // Son non disponible (politique autoplay du navigateur) — ne pas bloquer l'application
    console.warn('[NotificationService] Son non disponible :', e);
  }
}

  resetCount(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user?.userId) return;

    this.countSubject.next(0);

    this.http.put(`${this.BASE_URL}/user/${user.userId}/read-all`, {}).subscribe({
      next: () => console.log('[NotificationService] Notifications marquées comme lues'),
      error: (err) => console.error('[NotificationService] Erreur markAllAsRead :', err)
    });
  }

  getMessages(): any[] {
    return this.messagesSubject.getValue();
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
  }
}