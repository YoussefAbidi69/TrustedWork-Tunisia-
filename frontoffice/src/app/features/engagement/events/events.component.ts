import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EventService } from '../services/event.service';
import { GamificationService } from '../services/gamification.service';
import { EventDTO } from '../models/engagement.models';

@Component({
  selector: 'app-events',
  templateUrl: './events.component.html',
  styleUrls: ['./events.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class EventsComponent implements OnInit {
  events: EventDTO[] = [];
  filteredEvents: EventDTO[] = [];
  loading = true;
  error = false;
  searchTerm = '';
  selectedType = '';
  selectedGov = '';
  highlightedEventId: number | null = null;

  governorates = [
    'Tunis','Ariana','Ben Arous','Manouba','Nabeul','Zaghouan','Bizerte',
    'Béja','Jendouba','Kef','Siliana','Sousse','Monastir','Mahdia','Sfax',
    'Kairouan','Kasserine','Sidi Bouzid','Gabès','Medenine','Tataouine',
    'Gafsa','Tozeur','Kébili'
  ];

  types = ['HACKATHON', 'MEETUP', 'WEBINAR'];

  registeringId: number | null = null;
  successMsg = '';
  registeredEventIds: number[] = [];

  constructor(
    private eventService: EventService,
    private gamService: GamificationService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['highlight']) {
        this.highlightedEventId = +params['highlight'];
      }
    });
    this.loadEvents();
    this.loadMyRegistrations();
  }

  loadMyRegistrations(): void {
    this.eventService.getMyRegistrations().subscribe({
      next: (ids) => this.registeredEventIds = ids,
      error: (err) => console.error('Error loading registrations:', err)
    });
  }

  loadEvents(): void {
    this.loading = true;
    this.eventService.getAllEvents().subscribe({
      next: (data) => {
        this.events = data;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredEvents = this.events.filter(e => {
      // Si on a un highlight, on ne montre que cet événement
      if (this.highlightedEventId && e.id === this.highlightedEventId) {
        return true;
      }
      // Sinon on applique les filtres normaux (mais on cache les autres si un highlight est actif pour simuler une vue "Détails")
      if (this.highlightedEventId) return false;

      const matchSearch = !this.searchTerm ||
        (e.title && e.title.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (e.city && e.city.toLowerCase().includes(this.searchTerm.toLowerCase()));
      const matchType = !this.selectedType || e.type === this.selectedType;
      const matchGov = !this.selectedGov || e.governorate === this.selectedGov;
      return matchSearch && matchType && matchGov;
    });
  }

  clearHighlight(): void {
    this.highlightedEventId = null;
    this.applyFilters();
  }

  onSearch(): void { this.applyFilters(); }
  onTypeChange(): void { this.applyFilters(); }
  onGovChange(): void { this.applyFilters(); }

  register(event: EventDTO): void {
    console.log('Tentative d\'inscription pour l\'événement:', event);
    if (!event.id) return;
    this.registeringId = event.id;
    this.eventService.registerToEvent(event.id).subscribe({
      next: () => {
        this.registeringId = null;
        this.successMsg = `Inscription à "${event.title}" confirmée ! (+10 XP)`;
        setTimeout(() => this.successMsg = '', 5000);
        
        // Trigger Premium Gamification Logic
        this.gamService.notifyXpGain(10, 'Event Inscription');
        
        this.loadEvents();
        this.loadMyRegistrations();
      },
      error: (err) => {
        this.registeringId = null;
        console.error('Registration error:', err);
        const msg = err?.error?.message || "Une erreur est survenue lors de l'inscription. Vérifiez que vous êtes connecté.";
        alert(msg);
      }
    });
  }

  getTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      HACKATHON: 'fa-laptop-code',
      MEETUP: 'fa-people-group',
      WEBINAR: 'fa-video'
    };
    return icons[type] || 'fa-calendar';
  }

  getStatusClass(status: string): string {
    const cls: Record<string, string> = {
      UPCOMING: 'badge-upcoming',
      ONGOING: 'badge-ongoing',
      COMPLETED: 'badge-completed',
      CANCELLED: 'badge-cancelled'
    };
    return cls[status] || '';
  }

  isFull(event: EventDTO): boolean {
    return event.registeredCount >= event.capacity;
  }
}
