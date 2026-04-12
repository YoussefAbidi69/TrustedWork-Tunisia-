// --- ProfileResponse ---
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

// --- SkillResponse ---
export interface Skill {
  id: number;
  name: string;
  level: string;
  authenticityScore: number;
  examScore: number;
  endorsementCount: number;
}

// --- PortfolioResponse ---
export interface PortfolioItem {
  id: number;
  title: string;
  description: string;
  projectUrl: string;
  imageUrl: string;
  technologies: string;
  completionDate: string;
}

// --- CertificationResponse ---
export interface Certification {
  id: number;
  title: string;
  issuer: string;
  type: string;
  issueDate: string;
  expiryDate: string;
  certificateUrl: string;
  isExpired: boolean;
}

// --- EndorsementResponse ---
export interface Endorsement {
  id: number;
  endorserId: number;
  comment: string;
  endorsedAt: string;
}

// --- ReviewResponse ---
export interface ProfileReview {
  id: number;
  clientId: number;
  rating: number;
  comment: string;
  status: string;
  reviewedAt: string;
}

// --- ProfileReport ---
export interface ProfileReport {
  id: number;
  reporterId: number;
  reason: string;
  status: 'PENDING' | 'RESOLVED' | 'REJECTED';
  reportedAt: string;
  resolvedAt: string | null;
  profile?: {
    id: number;
    userId: number;
    headline: string;
    region: string;
  };
}

// --- WorkExperienceResponse ---
export interface WorkExperience {
  id: number;
  jobTitle: string;
  company: string;
  description: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
}

// --- EducationResponse ---
export interface Education {
  id: number;
  degree: string;
  institution: string;
  fieldOfStudy: string;
  graduationYear: number;
}

// --- CompletenessResponse ---
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

// --- CareerPathResponse ---
export interface CareerPathResponse {
  detectedPath: string;
  description: string;
  nextSteps: string[];
  currentSkills: string[];
  missingSkills: string[];
}

// --- SkillGapResponse ---
export interface SkillGapResponse {
  mySkills: string[];
  topSkills: string[];
  gapSkills: string[];
  gapCount: number;
}

// --- SkillGapRecommendation (endpoint recommendations/skill-gap) ---
export interface SkillGapRecommendation {
  missingSkills: string[];
  recommendedCourses: string[];
  marketDemand: { [skill: string]: number };
}