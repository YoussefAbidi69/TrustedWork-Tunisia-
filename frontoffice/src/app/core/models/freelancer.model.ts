
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

export interface Skill {
  id: number;
  name: string;
  level: 'JUNIOR' | 'CONFIRMED' | 'EXPERT';
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
  comment: string;
  endorsedAt: string;
}

export interface ProfileReview {
  id: number;
  clientId: number;
  rating: number;
  comment: string;
  status: 'VISIBLE' | 'HIDDEN' | 'FLAGGED';
  reviewedAt: string;
}

export interface WorkExperience {
  id: number;
  jobTitle: string;
  company: string;
  description: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
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