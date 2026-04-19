import { AuthUser } from './auth.model';

export enum MemberRole {
  LEAD = 'LEAD',
  MEMBER = 'MEMBER'
}

export enum MemberStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

export enum InvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  REJECTED = 'REJECTED',
  EXPIRED = 'EXPIRED'
}

export enum ProjectStatus {
  PLANNED = 'PLANNED',
  PLANNING = 'PLANNING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  ON_HOLD = 'ON_HOLD',
  CANCELLED = 'CANCELLED'
}

export enum TaskStatus {
  TODO = 'TODO',
  IN_PROGRESS = 'IN_PROGRESS',
  REVIEW = 'REVIEW',
  DONE = 'DONE',
  BLOCKED = 'BLOCKED'
}

export enum AgencyTier {
  STARTER = 'STARTER',
  PRO = 'PRO',
  ENTERPRISE = 'ENTERPRISE'
}

export interface Agency {
  id: number;
  creatorId: number;
  name: string;
  description?: string;
  logoUrl?: string;
  tier: AgencyTier;
  country?: string;
  city?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AgencyRequest {
  creatorId: number;
  name: string;
  description?: string;
  logoUrl?: string;
  country?: string;
  city?: string;
  active: boolean;
}

export interface AgencyMember {
  id: number;
  userId: number;
  role: MemberRole;
  status: MemberStatus;
  workloadScore: number;
  skills?: string;
  joinedAt: string;
}

export interface TeamProject {
  id: number;
  agencyId: number;
  creatorMemberId: number;
  title: string;
  description?: string;
  budget?: number;
  status: ProjectStatus;
  progress: number;
  active: boolean;
  startDate?: string;
  endDate?: string;
}

export interface AgencyInvitation {
  id: number;
  agencyId: number;
  senderId: number;
  receiverId: number;
  proposedRole: MemberRole;
  status: InvitationStatus;
  sentAt: string;
  respondedAt?: string;
  message?: string;
  agencyName?: string;
  senderName?: string;
}

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: string;
  progress: number;
  dueDate?: string;
}

export interface AgencyMembershipSummary {
  agencyId: number;
  agencyName: string;
  logoUrl?: string;
  role: string;
  status: string;
  joinedAt: string;
}

export interface AgencyContextDto {
  hasMemberships: boolean;
  memberships: AgencyMembershipSummary[];
}
