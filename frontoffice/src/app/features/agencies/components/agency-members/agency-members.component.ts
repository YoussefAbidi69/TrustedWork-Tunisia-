import { Component, Input, OnInit } from '@angular/core';
import { AgencyMemberService } from '../../services/agency-member.service';
import { AgencyMember, MemberRole } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-agency-members',
  templateUrl: './agency-members.component.html',
  styleUrls: ['./agency-members.component.css']
})
export class AgencyMembersComponent implements OnInit {
  @Input() agencyId!: number;
  members: AgencyMember[] = [];
  loading = true;

  constructor(private memberService: AgencyMemberService) {}

  ngOnInit(): void {
    if (this.agencyId) {
      this.loadMembers();
    }
  }

  loadMembers(): void {
    this.loading = true;
    this.memberService.getMembersByAgency(this.agencyId).subscribe({
      next: (data) => {
        this.members = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  getRoleBadgeClass(role: string): string {
    return role === MemberRole.LEAD ? 'badge--danger' : 'badge--azure';
  }

  getStatusBadgeClass(status: string): string {
    return status === 'ACTIVE' ? 'badge--success' : 'badge--warning';
  }

  removeMember(memberId: number): void {
    if (confirm('Voulez-vous vraiment retirer ce membre de l\'agence ?')) {
      this.memberService.removeMember(memberId).subscribe({
        next: () => this.loadMembers(),
        error: (err) => console.error(err)
      });
    }
  }
}
