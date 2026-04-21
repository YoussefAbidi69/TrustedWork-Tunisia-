<div class="notif-wrapper">

  <!-- Bouton cloche -->
  <button
    type="button"
    class="notification-bell"
    title="Notifications"
    (click)="togglePanel(); $event.stopPropagation()">
    <i class="fas fa-bell"></i>
    <span *ngIf="count > 0" class="notification-bell__count">{{ displayCount }}</span>
    <span *ngIf="hasPulse && count > 0" class="notification-bell__pulse"></span>
  </button>

  <!-- Panneau dropdown -->
  <div class="notif-panel" *ngIf="isOpen" (click)="$event.stopPropagation()">

    <div class="notif-panel__header">
      <strong>Notifications</strong>
      <span class="notif-panel__badge" *ngIf="messages.length > 0">
        {{ messages.length }}
      </span>
    </div>

    <!-- Liste des notifications -->
    <div class="notif-panel__list" *ngIf="messages.length > 0">
      <div
        class="notif-item"
        *ngFor="let msg of messages">
        <div class="notif-item__icon" [style.color]="getIconColor(msg.type)">
          <i [class]="getIcon(msg.type)"></i>
        </div>
        <div class="notif-item__body">
          <p class="notif-item__message">{{ msg.message }}</p>
          <span class="notif-item__time">
            {{ formatDate(msg.createdAt) }}
          </span>
        </div>
      </div>
    </div>

    <!-- État vide -->
    <div class="notif-panel__empty" *ngIf="messages.length === 0">
      <i class="fas fa-bell-slash"></i>
      <p>Aucune notification</p>
    </div>

  </div>
</div>