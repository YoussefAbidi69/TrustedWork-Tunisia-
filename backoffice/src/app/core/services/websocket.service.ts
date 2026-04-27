import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {

  // Client STOMP avec WebSocket natif browser — SockJS retiré (incompatible Vite/Angular 18)
  private client: Client | null = null;
  private connected = false;

  // Contexte audio pour le son de notification (Web Audio API — pas de fichier externe)
  private audioCtx: AudioContext | null = null;

  constructor() {}

  connect(onConnected?: () => void): void {
    if (this.connected) {
      if (onConnected) onConnected();
      return;
    }

    this.client = new Client({
      brokerURL: 'ws://localhost:8082/ws',
      reconnectDelay: 0,
      onConnect: () => {
        this.connected = true;
        console.log('✅ WebSocket connecté');
        if (onConnected) onConnected();
      },
      onStompError: (frame) => {
        console.error('❌ Erreur WebSocket STOMP:', frame);
        this.connected = false;
      },
      onDisconnect: () => {
        this.connected = false;
      }
    });

    this.client.activate();
  }

  subscribeToReports(callback: (message: any) => void): void {
    if (!this.connected || !this.client) {
      console.warn('⚠️ WebSocket non connecté');
      return;
    }

    this.client.subscribe('/topic/admin/reports', (message: IMessage) => {
      if (message.body) {
        this.playNotificationSound(); // Son avant callback
        callback(JSON.parse(message.body));
      }
    });
  }

  /**
   * Son de notification — 3 notes do/mi/sol générées via Web Audio API.
   * Aucun fichier audio externe requis.
   */
  playNotificationSound(): void {
    try {
      if (!this.audioCtx) {
        this.audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      }

      const ctx = this.audioCtx;

      // Reprendre si suspendu (politique autoplay navigateur)
      if (ctx.state === 'suspended') {
        ctx.resume();
      }

      // Accord do-mi-sol en séquence rapide
      this.playTone(ctx, 523, 0,    0.12, 0.08); // do
      this.playTone(ctx, 659, 0.12, 0.12, 0.08); // mi
      this.playTone(ctx, 784, 0.24, 0.18, 0.10); // sol

    } catch (e) {
      console.warn('Audio non disponible:', e);
    }
  }

  /**
   * Jouer une tonalité avec enveloppe (attaque rapide, release exponentiel)
   */
  private playTone(
    ctx: AudioContext,
    freq: number,
    delay: number,
    duration: number,
    volume: number
  ): void {
    const oscillator = ctx.createOscillator();
    const gainNode   = ctx.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);

    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(freq, ctx.currentTime + delay);

    gainNode.gain.setValueAtTime(0, ctx.currentTime + delay);
    gainNode.gain.linearRampToValueAtTime(volume, ctx.currentTime + delay + 0.02);
    gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + delay + duration);

    oscillator.start(ctx.currentTime + delay);
    oscillator.stop(ctx.currentTime + delay + duration);
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
      console.log('🔌 WebSocket déconnecté');
    }
    if (this.audioCtx) {
      this.audioCtx.close();
      this.audioCtx = null;
    }
  }
}