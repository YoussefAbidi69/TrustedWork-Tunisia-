/** DTOs and filter types aligned with smart-job-board REST API (port 8082). */

export type JobOfferStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'FLAGGED';
export type ApplicationStatus =
  | 'PENDING'
  | 'SHORTLISTED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN';
export type TrendDirection = 'RISING' | 'STABLE' | 'DECLINING';

export interface FraudSignalDto {
  code: string;
  message: string;
  weight: number;
}

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
  status: JobOfferStatus;
  fraudRiskScore: number;
  opportunityScore: number;
  opportunityBudgetComponent: number | null;
  opportunityDemandComponent: number | null;
  opportunityCompetitionComponent: number | null;
  publishedAt: string | null;
  expiresAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  fraudSignals: FraudSignalDto[];
  applicationCount?: number | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface JobFilters {
  category?: string;
  skills?: string[];
  budgetMin?: number;
  budgetMax?: number;
  location?: string;
  remote?: boolean;
}

export interface GenerateCoverLetterRequest {
  jobTitle: string;
  jobDescription: string;
  freelancerName: string;
  skills: string[];
  bio: string;
  pastProjects: string;
}

export interface GenerateCoverLetterResponse {
  coverLetter: string;
}

export interface CreateJobRequest {
  title: string;
  description: string;
  category: string;
  requiredSkills: string[];
  budgetMin: number;
  budgetMax: number;
  durationDays: number;
  location: string;
  remote: boolean;
  expiresAt: string;
}

export type UpdateJobRequest = CreateJobRequest;

export interface CreateApplicationRequest {
  jobOfferId: number;
  coverLetter: string;
  proposedRate: number;
  declaredSkills?: string[];
  freelancerSkills?: string[];
}

export interface MatchScoreBreakdown {
  skillMatch: number;
  reputation: number;
  successRate: number;
  budgetFit: number;
  availability: number;
  totalScore: number;
}

export interface JobApplication {
  id: number;
  jobOfferId: number;
  jobTitle: string | null;
  jobStatus: string | null;
  freelancerId: number;
  freelancerName?: string;
  coverLetter: string;
  _coverExpanded?: boolean;
  proposedRate: number;
  status: ApplicationStatus;
  appliedAt: string;
  matchScore: MatchScoreBreakdown | null;
  successProbability: number | null;
  predictionConfidence: string | null;
}

export interface RecommendationRow {
  jobOfferId: number;
  title: string;
  category: string;
  matchScore: number;
  opportunityScore: number;
  freshnessFactor: number;
  rankingScore: number;
  freshnessScore?: number;
  recommendationScore?: number;
  successProbability?: number;
  confidence?: string;
  job?: JobOffer;
  topMatchingSkills?: string[];
}

export interface MarketInsight {
  skill: string;
  count: number;
  trend: TrendDirection;
  changePercent?: number;
  lastPeriodCount?: number;
}

export interface SalaryInsight {
  skill: string;
  avgProposedRate: number;
  minRate: number;
  maxRate: number;
  medianRate: number;
  sampleCount: number;
  category?: string;
}

export interface MarketForecast {
  skill: string;
  currentDemand: number;
  forecastNextMonth: number;
  forecastIn3Months: number;
  confidence: 'LOW' | 'MEDIUM' | 'HIGH' | string;
}

export interface MarketCategory {
  category: string;
  jobCount: number;
}

export interface MarketOverview {
  activeJobPostings: number;
  avgCompetition: number;
}

export interface CareerSkillSuggestion {
  skill: string;
  combinedScore: number;
  trendComponent: number;
  coOccurrenceComponent: number;
  estimatedIncomeIncreasePercent: number;
}

export interface CareerSuggestionDto {
  id?: number;
  suggestedSkill: string;
  trendScore?: number;
  coOccurrenceRate?: number;
  estimatedIncomeImpact?: number;
  trend?: TrendDirection | string;
}

export interface MicroCurriculum {
  week: number;
  focus: string;
}

export interface CareerRoadmapStep {
  id: number;
  title: string;
  description: string;
  difficultyLevel: string;
  estimatedWeeks: number;
  hoursPerDay: number;
  incomeBoostThisStep: number;
  microCurriculum: MicroCurriculum[];
  resources: string[];
  portfolioProject: string;
  prerequisiteSkills: string[];
  skillsUnlocked: string[];
  demandLevel: string;
  color: string;
}

export interface CareerInsightResponse {
  targetRole: string;
  currentLevel: string;
  totalWeeks: number;
  totalIncomeBoost: number;
  currentRate: number;
  projectedRate: number;
  difficulty: string;
  steps: CareerRoadmapStep[];
}

export interface SuccessPrediction {
  probability: number;
  confidenceLabel: string;
  skillOverlapPercent?: number;
  reputationScore?: number;
  successRateScore?: number;
  predictionSummary?: string;
}

export interface MatchFreelancerRow {
  freelancerId: number;
  email?: string;
  totalMatchScore: number;
  skillMatch: number;
  reputation: number;
  successRate: number;
  budgetFit: number;
  availability: number;
  successProbability: number;
  predictionConfidence: string;
}

export interface SuccessPredictionRequest {
  jobOfferId: number;
  freelancerId: number;
  freelancerSkills?: string[];
}

export interface PreviewSkillsResponse {
  skills: string[];
}

/* ── Messaging ── */

export interface MessageDto {
  id: number;
  senderId: string | number;
  receiverId?: string | number;
  content: string;
  type: 'text' | 'file' | 'meet';
  fileUrl?: string;
  meetUrl?: string;
  createdAt: string;
  sentAt?: string;
  read?: boolean;
  jobOfferId?: number;
}

export interface SendMessageRequest {
  content: string;
  type?: 'text' | 'file';
  fileUrl?: string;
}

export interface ScheduleMeetRequest {
  conversationId: string;
  title: string;
  date: string;
  time: string;
  duration: number;
  note?: string;
}

export interface ScheduleMeetResponse {
  meetUrl: string;
  eventId: string;
}

export interface ConversationMessageRequest {
  content: string;
  type?: 'text' | 'file';
  fileUrl?: string;
}

export interface ConversationSummary {
  id: string;
  otherPartyId: string;
  otherPartyName: string;
  jobTitle: string;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
  jobOfferId?: number;
  peerId?: number;
  peerName?: string;
}
