// ─── ENUMS ───────────────────────────────────────────────────────────────────

export type EventType = 'HACKATHON' | 'MEETUP' | 'WEBINAR';
export type EventStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELLED';
export type RegistrationStatus = 'REGISTERED' | 'ATTENDED' | 'ABSENT';
export type BadgeRarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
export type ChallengeStatus = 'ACTIVE' | 'COMPLETED' | 'EXPIRED';
export type ParticipationStatus = 'JOINED' | 'SUCCESS' | 'CLAIMED';

// ─── EVENT ───────────────────────────────────────────────────────────────────

export interface EventDTO {
  id?: number;
  title: string;
  description: string;
  type: EventType;
  city: string;
  governorate: string;
  online: boolean;
  capacity: number;
  registeredCount: number;
  startDate: string;
  endDate: string;
  status: EventStatus;
}

// ─── EVENT REGISTRATION ───────────────────────────────────────────────────────

export interface EventRegistration {
  id: number;
  event: EventDTO;
  userId: number;
  status: RegistrationStatus;
  registeredAt: string;
}

// ─── GAMIFICATION ─────────────────────────────────────────────────────────────

export interface GrowthProfileDTO {
  userId: number;
  xpPoints: number;
  level: number;
  engagementScore: number;
  currentStreak?: number;
  influenceScore?: number;
  churnRisk?: number;
}

export interface BadgeDTO {
  id: number;
  code: string;
  name: string;
  description: string;
  rarity: BadgeRarity;
  xpReward: number;
  iconUrl: string;
}

// ─── LEADERBOARD ──────────────────────────────────────────────────────────────

export interface GameStats {
  engagementScore: number;
  influenceScore?: number;
  churnRisk?: number;
}

export interface LeaderboardDTO {
  userId: number;
  governorate: string;
  engagementScore: number;
  rank: number;
  firstName?: string;
  lastName?: string;
  photo?: string;
}

// ─── CHALLENGE ────────────────────────────────────────────────────────────────

export interface ChallengeParticipationDTO {
  id?: number;
  challengeId: number;
  userId: number;
  status: ParticipationStatus;
  joinedAt?: string;
  completedAt?: string;
}

export interface ChallengeDTO {
  id: number;
  title: string;
  description: string;
  xpReward: number;
  deadline: string;
  challengeTypeCode?: string;
  status: ChallengeStatus;
  currentParticipation?: ChallengeParticipationDTO;
}
