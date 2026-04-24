// ─── ENUMS ───────────────────────────────────────────────────────────────────

export type EventType = 'HACKATHON' | 'MEETUP' | 'WEBINAR';
export type EventStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'CANCELLED';
export type RegistrationStatus = 'REGISTERED' | 'ATTENDED' | 'ABSENT';
export type BadgeRarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
export type ChallengeStatus = 'ACTIVE' | 'COMPLETED' | 'EXPIRED';

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
  registeredUserIds?: number[];
}

export interface GrowthProfileDTO {
  userId: number;
  xpPoints: number;
  xpToNextLevel: number;
  level: number;
  engagementScore: number;
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
  ownerIds?: number[];
}

export interface LeaderboardDTO {
  userId: number;
  governorate: string;
  engagementScore: number;
  rank: number;
}

export interface ChallengeDTO {
  id?: number;
  title: string;
  description: string;
  xpReward: number;
  deadline: string;
  challengeTypeCode?: string;
  status: ChallengeStatus;
}
