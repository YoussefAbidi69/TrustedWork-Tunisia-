import { Component, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-community-shell',
  templateUrl: './community-shell.component.html',
  styleUrls: ['./community-shell.component.css']
})
export class CommunityShellComponent {
  communityMenuOpen = false;

  constructor(
    public route: ActivatedRoute,
    public authService: AuthService
  ) {}

  /** From parent app route `communities/:communityId` when scoped to one community. */
  get parentCommunityId(): string | null {
    let r: ActivatedRoute | null = this.route;
    while (r) {
      const id = r.snapshot.paramMap.get('communityId');
      if (id) {
        return id;
      }
      r = r.parent;
    }
    return null;
  }

  toggleCommunityMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.communityMenuOpen = !this.communityMenuOpen;
  }

  closeMenus(): void {
    this.communityMenuOpen = false;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.communityMenuOpen = false;
  }
}
