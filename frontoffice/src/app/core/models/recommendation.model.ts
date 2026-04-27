export interface ProjectRequest {
  description: string;
  budget: number;
  deadlineDays: number;
  category: string;
  optimizationMode: string;
}

export interface FreelancerRecommendation {
  freelancerId: string;
  name: string;
  category: string;
  experienceLevel: string;
  hourlyRateUsd: number;
  avgRating: number;
  successProba: number;
  semanticScore: number;
  finalScore: number;
  cin: string;
  skills: string[];
}