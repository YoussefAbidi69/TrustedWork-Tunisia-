export interface RecommendationFilter {
  minScore?: number;
  skills?: string[];
  availability?: string;
  sortBy?: 'score' | 'trust' | 'availability';
  search?: string;
}

export interface ScoreBreakdown {
  skillMatch: number;
  trust: number;
  availability: number;
  experience: number;
  similarity: number;
  location: number;
}

export interface FreelancerRecommendation {
  freelancerId: number;
  firstName: string;
  lastName: string;
  email: string;
  headline: string;
  bio: string;
  location: string;
  skills: string[];
  availability: string | null;
  trustLevel: number;
  kycStatus: string;
  photo: string | null;
  recommendationScore: number;
  scoreBreakdown: ScoreBreakdown;
  explanation: string;
  alreadyInvited: boolean;
  invitationStatus: string | null;
}

export interface RecommendationResponse {
  agencyId: number;
  agencyName: string;
  totalCandidates: number;
  page: number;
  size: number;
  recommendations: FreelancerRecommendation[];
}
