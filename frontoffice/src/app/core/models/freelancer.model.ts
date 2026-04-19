export interface FreelancerProfile {
  id: number;
  userId: number;
  headline: string;
  bio: string;
  avatarUrl: string;
  hourlyRate: number;
  availabilityStatus: 'AVAILABLE' | 'BUSY' | 'ON_VACATION';
  visibility: 'PUBLIC' | 'PRIVATE' | 'CONNECTIONS_ONLY';
  projectType: 'SHORT_TERM' | 'LONG_TERM' | 'BOTH';
  completenessScore: number;
  region: string;
  regionalRank: number;
  totalViews: number;
  createdAt: string;
  updatedAt: string;
}

export type SkillCategory =
  | 'FRONTEND'
  | 'BACKEND'
  | 'FULLSTACK'
  | 'MOBILE'
  | 'DEVOPS'
  | 'CLOUD'
  | 'DATA'
  | 'AI'
  | 'DESIGN'
  | 'SECURITY'
  | 'OTHER';

export type SkillLevel =
  | 'JUNIOR'
  | 'INTERMEDIATE'
  | 'CONFIRMED'
  | 'EXPERT';

export interface Skill {
  id: number;
  name: string;
  category: SkillCategory;
  level: SkillLevel;
  authenticityScore: number;
  examScore: number;
  endorsementCount: number;
}

export interface PortfolioItem {
  id: number;
  title: string;
  description: string;
  projectUrl: string;
  imageUrl: string;
  technologies: string;
  completionDate: string;
  pinned: boolean;
  projectScore: number;
}

export interface Certification {
  id: number;
  title: string;
  issuer: string;
  type: 'EXTERNAL' | 'INTERNAL' | 'ACADEMIC';
  issueDate: string;
  expiryDate: string;
  certificateUrl: string;
  isExpired: boolean;
}

export interface Endorsement {
  id: number;
  endorserId: number;
  comment: string | null;
  endorsedAt: string;
}

export type ReviewStatus = 'VISIBLE' | 'HIDDEN' | 'FLAGGED';

export interface ProfileReview {
  id: number;
  clientId: number;
  rating: number;
  comment: string;
  freelancerReply?: string | null;
  flagged: boolean;
  flagReason?: string | null;
  status: ReviewStatus;
  reviewedAt: string;
  updatedAt?: string | null;
}

export interface AddProfileReviewRequest {
  clientId: number;
  rating: number;
  comment: string;
}

export interface ReplyToReviewRequest {
  reply: string;
}

export interface ProfileReviewSummary {
  profileId: number;
  averageRating: number;
  totalReviews: number;
  fiveStarCount: number;
  fourStarCount: number;
  threeStarCount: number;
  twoStarCount: number;
  oneStarCount: number;
}

export interface WorkExperience {
  id: number;
  jobTitle: string;
  company: string;
  location?: string;
  description?: string;
  startDate: string;
  endDate?: string | null;
  isCurrent: boolean;

  periodLabel?: string;
  durationLabel?: string;
  durationInMonths?: number;

  createdAt?: string;
  updatedAt?: string;
}

export interface Education {
  id: number;
  degree: string;
  institution: string;
  fieldOfStudy: string;
  graduationYear: number;
}

export interface CompletenessResponse {
  score: number;
  bioScore: number;
  avatarScore: number;
  skillsScore: number;
  portfolioScore: number;
  certifScore: number;
  workExpScore: number;
  suggestions: string[];
}

export interface CareerPathResponse {
  detectedPath: string;
  description: string;
  nextSteps: string[];
  currentSkills: string[];
  missingSkills: string[];
}

export interface SkillGapResponse {
  mySkills: string[];
  topSkills: string[];
  gapSkills: string[];
  gapCount: number;
}