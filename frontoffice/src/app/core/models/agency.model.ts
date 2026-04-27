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
  EN_COURS = 'EN_COURS',
  EN_ATTENTE = 'EN_ATTENTE',
  TERMINE = 'TERMINE',
  ANNULE = 'ANNULE'
}

export enum ProjectPriority {
  FAIBLE = 'FAIBLE',
  MOYENNE = 'MOYENNE',
  HAUTE = 'HAUTE'
}

export enum TaskStatus {
  BACKLOG = 'BACKLOG',
  A_FAIRE = 'A_FAIRE',
  EN_COURS = 'EN_COURS',
  REVIEW = 'REVIEW',
  TERMINE = 'TERMINE',
  ANNULE = 'ANNULE'
}

export enum TaskPriority {
  FAIBLE = 'FAIBLE',
  MOYENNE = 'MOYENNE',
  HAUTE = 'HAUTE',
  URGENTE = 'URGENTE'
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
  sector?: string;
  website?: string;
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
  sector?: string;
  website?: string;
  active: boolean;
}

export interface AgencyMember {
  id: number;
  userId: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  photo?: string;
  userSkills?: string;
  role: MemberRole;
  status: MemberStatus;
  workloadScore: number;
  skills?: string;
  joinedAt: string;
}

export interface AssignedMember {
  memberId: number;
  userId: number;
  firstName?: string;
  lastName?: string;
  photo?: string;
}

export interface TeamProject {
  id: number;
  agencyId: number;
  creatorMemberId: number;
  name: string;
  description?: string;
  budget?: number;
  status: ProjectStatus;
  priority: ProjectPriority;
  progress: number;
  active: boolean;
  startDate?: string;
  endDate?: string;
  assignedMembers?: AssignedMember[];
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
  receiverName?: string;
  receiverEmail?: string;
}

export interface Task {
  id: number;
  agencyId: number;
  projectId: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignedMember?: AssignedMember;
  assignedMemberId?: number;
  createdAt?: string;
  updatedAt?: string;
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
  ownsAnAgency?: boolean;
  memberships: AgencyMembershipSummary[];
  pendingInvitationCount?: number;
  pendingInvitations?: any[];
}

export type JoinRequestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED';

export interface AgencyJoinRequest {
  id: number;
  agencyId: number;
  agencyName?: string;
  requesterId: number;
  requesterFirstName?: string;
  requesterLastName?: string;
  requesterEmail?: string;
  requesterPhoto?: string;
  requesterSkills?: string;
  requesterHeadline?: string;
  status: JoinRequestStatus;
  message?: string;
  requestedAt: string;
  respondedAt?: string;
}

export interface AgencyMemberRanking {
  memberId: number;
  fullName: string;
  avatarUrl?: string;
  averageCompletionScore: number;
  completedTaskCount: number;
}

export interface AgencyAnalytics {
  totalTasks: number;
  completedTasks: number;
  cancelledTasks: number;
  averageTaskDays: number;
  topMembers: AgencyMemberRanking[];
}
