import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EventAdminService } from '../services/event-admin.service';
import { EventDTO } from '../models/engagement.models';
import { UserService, UserDTO } from '../../../core/services/user.service';

@Component({
  selector: 'app-events-admin',
  templateUrl: './events-admin.component.html',
  styleUrls: ['./events-admin.component.css']
})
export class EventsAdminComponent implements OnInit {

  events: EventDTO[] = [];
  filteredEvents: EventDTO[] = [];   // ← nouveau
  loading = true;
  showModal = false;
  searchTerm = '';                    // ← nouveau
  activeFilter = 'ALL';              // ← nouveau

  isEditMode = false;
  selectedEventId?: number;
  userMap: Map<number, any> = new Map();

  newEvent: EventDTO = {
    title: '', description: '', type: 'MEETUP',
    city: '', governorate: '', online: false,
    capacity: 50, registeredCount: 0,
    startDate: '', endDate: '', status: 'UPCOMING'
  };

  constructor(
    private eventService: EventAdminService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadEvents();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe(users => {
      users.forEach(u => this.userMap.set(u.id, u));
    });
  }

  loadEvents(): void {
    this.loading = true;
    this.eventService.getAllEvents().subscribe({
      next: (res) => {
        this.events = res;
        this.filteredEvents = res;   // ← init filteredEvents
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  // ← nouveau : filtre combiné search + type
  applyFilter(): void {
    this.filteredEvents = this.events.filter(ev => {
      const matchType = this.activeFilter === 'ALL' || ev.type === this.activeFilter;
      const matchSearch = ev.title.toLowerCase().includes(this.searchTerm.toLowerCase());
      return matchType && matchSearch;
    });
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  openModal(): void { 
    this.isEditMode = false;
    this.selectedEventId = undefined;
    this.resetForm();
    this.showModal = true; 
  }
  closeModal(): void { this.showModal = false; }

  resetForm(): void {
    this.newEvent = {
      title: '', description: '', type: 'MEETUP',
      city: '', governorate: '', online: false,
      capacity: 50, registeredCount: 0,
      startDate: '', endDate: '', status: 'UPCOMING'
    };
  }

  editEvent(ev: EventDTO): void {
    this.isEditMode = true;
    this.selectedEventId = ev.id;
    this.newEvent = { ...ev };
    this.showModal = true;
  }

  deleteEvent(id: number): void {
    if(confirm('Are you sure you want to delete this event?')) {
      this.eventService.deleteEvent(id).subscribe({
        next: () => this.loadEvents(),
        error: (err) => {
          console.error('Error deleting event:', err);
          alert('Failed to delete the event.');
        }
      });
    }
  }

  saveEvent(): void {
    const payload = { ...this.newEvent };
    if (payload.startDate && payload.startDate.length === 16) payload.startDate += ':00';
    if (payload.endDate   && payload.endDate.length   === 16) payload.endDate   += ':00';

    if (this.isEditMode && this.selectedEventId) {
      this.eventService.updateEvent(this.selectedEventId, payload).subscribe({
        next: () => { this.closeModal(); this.loadEvents(); },
        error: (err) => {
          console.error('Error updating event:', err);
          alert('Failed to update the event.');
        }
      });
    } else {
      this.eventService.createEvent(payload).subscribe({
        next: () => { this.closeModal(); this.loadEvents(); },
        error: (err) => {
          console.error('Error creating event:', err);
          alert('Failed to create the event. Please check the dates and fields.');
        }
      });
    }
  }

  // ← nouvelles méthodes pour le template
  countByStatus(status: string): number {
    return this.events.filter(e => e.status === status).length;
  }

  totalRegistrations(): number {
    return this.events.reduce((sum, e) => sum + (e.registeredCount || 0), 0);
  }

  getEventIcon(type: string): string {
    const icons: Record<string, string> = {
      HACKATHON: 'fa-code',
      MEETUP:    'fa-users',
      WEBINAR:   'fa-video'
    };
    return icons[type] || 'fa-calendar-alt';
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      UPCOMING:  'chip-upcoming',
      ONGOING:   'chip-ongoing',
      COMPLETED: 'chip-completed',
      CANCELLED: 'chip-cancelled'
    };
    return map[status] || '';
  }

  getParticipant(id: number): UserDTO | undefined {
    return this.userMap.get(id);
  }

  getParticipantName(id: number): string {
    const user = this.getParticipant(id);
    return user ? `${user.firstName} ${user.lastName}` : `User #${id}`;
  }

  removeParticipant(eventId: number, userId: number): void {
    if (confirm('Voulez-vous vraiment retirer ce participant de l\'événement ?')) {
      this.eventService.removeRegistration(eventId, userId).subscribe({
        next: () => this.loadEvents(),
        error: (err) => console.error('Error removing participant:', err)
      });
    }
  }

  goToUserDetails(id: number): void {
    this.router.navigate(['/admin/users', id]);
  }
}