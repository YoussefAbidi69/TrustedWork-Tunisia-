export interface JobOffer {
  id: number;
  clientId: number;
  title: string;
  description: string;
  category: string;
  requiredSkills: string[];
  extractedSkills: string[];
  budgetMin: number;
  budgetMax: number;
  durationDays: number | null;
  location: string | null;
  remote: boolean;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'FLAGGED';
  fraudRiskScore: number;
  opportunityScore: number;
  applicationCount?: number | null;
  publishedAt: string | null;
  expiresAt: string | null;
  createdAt: string | null;
  updatedAt?: string | null;
  offerFlags?: OfferFlag[];
}

export interface MatchScoreBreakdown {
  skillMatch?: number;
  reputation?: number;
  successRate?: number;
  budgetFit?: number;
  availability?: number;
  totalScore?: number;
}

export interface Application {
  id: number;
  jobOfferId: number;
  jobTitle: string;
  jobCategory?: string;
  jobLocation?: string;
  freelancerId: number;
  coverLetter: string;
  proposedRate: number;
  status: 'PENDING' | 'SHORTLISTED' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';
  matchScore: MatchScoreBreakdown | null;
  appliedAt: string;
}

export interface OfferFlag {
  id: number;
  jobOfferId: number;
  signalName: string;
  signalWeight: number;
  description: string;
  detectedAt: string;
}

export interface MarketInsightResponse {
  skillName: string;
  count: number;
  trend: 'RISING' | 'STABLE' | 'DECLINING';
  changePercent: number;
  lastPeriodCount: number;
}

export interface AdminMarketAnalyticsApiResponse {
  skillInsights: {
    skill: string;
    count: number;
    trend: 'RISING' | 'STABLE' | 'DECLINING';
    changePercent: number;
    lastPeriodCount: number;
  }[];
  totalPublishedJobs: number;
  platformMockAverageBudget: number;
}

export interface PlatformStatsDto {
  totalJobs: number;
  publishedJobs: number;
  totalApplications: number;
  flaggedJobs: number;
  avgMatchScore: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AdminJobFilters {
  status?: string;
  category?: string;
  search?: string;
  page?: number;
  size?: number;
  /** Spring format, e.g. `createdAt,desc` or `fraudRiskScore,asc` */
  sort?: string;
}

/** Mirrors backend JobOfferUpdateRequest */
export interface JobOfferAdminUpdatePayload {
  title: string;
  description: string;
  category: string;
  requiredSkills: string[];
  budgetMin: number;
  budgetMax: number;
  durationDays?: number | null;
  location?: string | null;
  remote: boolean;
  expiresAt?: string | null;
}
