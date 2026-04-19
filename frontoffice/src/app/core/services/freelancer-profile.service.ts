import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  FreelancerProfile,
  Skill,
  PortfolioItem,
  Certification,
  Endorsement,
  ProfileReview,
  WorkExperience,
  Education,
  CompletenessResponse,
  CareerPathResponse,
  SkillGapResponse,
  SkillCategory,
  AddProfileReviewRequest,
  ReplyToReviewRequest,
  ProfileReviewSummary
} from '../models/freelancer.model';

/**
 * Service HTTP — communication avec le freelancer-profile-service (port 8082)
 * URLs alignées sur les controllers backend réels après audit complet.
 */
@Injectable({
  providedIn: 'root'
})
export class FreelancerProfileService {

  private readonly BASE_URL      = 'http://localhost:8082/api';
  private readonly USER_BASE_URL = 'http://localhost:8081/api';

  constructor(private readonly http: HttpClient) {}

  // =========================================================
  // PROFILE — /api/profiles
  // =========================================================

  // Alias vers user-service — utilisé par reviews.component pour résoudre les noms
  getUserIdentity(userId: number): Observable<any> {
    return this.http.get<any>(`${this.USER_BASE_URL}/identity/users/${userId}`);
  }

  createProfile(data: Partial<FreelancerProfile>): Observable<FreelancerProfile> {
    return this.http.post<FreelancerProfile>(`${this.BASE_URL}/profiles`, data);
  }

  getProfileById(profileId: number): Observable<FreelancerProfile> {
    return this.http.get<FreelancerProfile>(`${this.BASE_URL}/profiles/${profileId}`);
  }

  getProfileByUserId(userId: number): Observable<FreelancerProfile> {
    return this.http.get<FreelancerProfile>(`${this.BASE_URL}/profiles/user/${userId}`);
  }

  updateProfile(userId: number, data: Partial<FreelancerProfile>): Observable<FreelancerProfile> {
    return this.http.put<FreelancerProfile>(`${this.BASE_URL}/profiles/user/${userId}`, data);
  }

  updateAvailability(userId: number, status: string): Observable<FreelancerProfile> {
    return this.http.patch<FreelancerProfile>(
      `${this.BASE_URL}/profiles/user/${userId}/availability?status=${status}`,
      {}
    );
  }

  deleteProfile(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/profiles/user/${userId}`);
  }

  getCompleteness(userId: number): Observable<CompletenessResponse> {
    return this.http.get<CompletenessResponse>(`${this.BASE_URL}/profiles/user/${userId}/completeness`);
  }

  getAllPublicProfiles(): Observable<FreelancerProfile[]> {
    return this.http.get<FreelancerProfile[]>(`${this.BASE_URL}/profiles`);
  }

  getRankingByRegion(region: string): Observable<FreelancerProfile[]> {
    return this.http.get<FreelancerProfile[]>(`${this.BASE_URL}/profiles/ranking/${region}`);
  }

  searchProfiles(filters: {
    region?: string;
    availability?: 'AVAILABLE' | 'BUSY' | 'ON_VACATION' | '';
    minRate?: number | null;
    maxRate?: number | null;
  }): Observable<FreelancerProfile[]> {
    let params = new HttpParams();

    if (filters.region) {
      params = params.set('region', filters.region);
    }

    if (filters.availability) {
      params = params.set('availability', filters.availability);
    }

    if (filters.minRate !== null && filters.minRate !== undefined) {
      params = params.set('minRate', filters.minRate.toString());
    }

    if (filters.maxRate !== null && filters.maxRate !== undefined) {
      params = params.set('maxRate', filters.maxRate.toString());
    }

    return this.http.get<FreelancerProfile[]>(`${this.BASE_URL}/profiles/search`, { params });
  }

  // =========================================================
  // SKILLS — /api/skills
  // =========================================================

  getMySkills(userId: number): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.BASE_URL}/skills/user/${userId}`);
  }

  getSkillsByUserId(userId: number): Observable<Skill[]> {
    return this.getMySkills(userId);
  }

  addSkill(
    userId: number,
    data: { name: string; category: SkillCategory; examScore: number }
  ): Observable<Skill> {
    return this.http.post<Skill>(`${this.BASE_URL}/skills/user/${userId}`, data);
  }

  deleteSkill(skillId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/skills/${skillId}/user/${userId}`);
  }

  getSkillAuthenticity(skillId: number): Observable<number> {
    return this.http.get<number>(`${this.BASE_URL}/skills/${skillId}/authenticity`);
  }

  getSkillGaps(userId: number): Observable<SkillGapResponse> {
    return this.http.get<SkillGapResponse>(`${this.BASE_URL}/skills/user/${userId}/gaps`);
  }

  updateExamScore(skillId: number, userId: number, examScore: number): Observable<Skill> {
  return this.http.patch<Skill>(
    `${this.BASE_URL}/skills/${skillId}/user/${userId}/exam-score`,
    { examScore }
  );
}

  // =========================================================
  // PORTFOLIO — /api/portfolio
  // =========================================================

  getMyPortfolio(userId: number): Observable<PortfolioItem[]> {
    return this.http.get<PortfolioItem[]>(`${this.BASE_URL}/portfolio/user/${userId}`);
  }

  getPortfolioByUserId(userId: number): Observable<PortfolioItem[]> {
    return this.getMyPortfolio(userId);
  }

  getPinnedPortfolio(userId: number): Observable<PortfolioItem[]> {
    return this.http.get<PortfolioItem[]>(`${this.BASE_URL}/portfolio/user/${userId}/pinned`);
  }

  addPortfolioItem(userId: number, data: Partial<PortfolioItem>): Observable<PortfolioItem> {
    return this.http.post<PortfolioItem>(`${this.BASE_URL}/portfolio/user/${userId}`, data);
  }

  updatePortfolioItem(itemId: number, userId: number, data: Partial<PortfolioItem>): Observable<PortfolioItem> {
    return this.http.put<PortfolioItem>(`${this.BASE_URL}/portfolio/${itemId}/user/${userId}`, data);
  }

  deletePortfolioItem(itemId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/portfolio/${itemId}/user/${userId}`);
  }

  pinPortfolioItem(itemId: number, userId: number): Observable<PortfolioItem> {
    return this.http.patch<PortfolioItem>(`${this.BASE_URL}/portfolio/${itemId}/user/${userId}/pin`, {});
  }

  unpinPortfolioItem(itemId: number, userId: number): Observable<PortfolioItem> {
    return this.http.patch<PortfolioItem>(`${this.BASE_URL}/portfolio/${itemId}/user/${userId}/unpin`, {});
  }

  // =========================================================
  // CERTIFICATIONS — /api/certifications
  // =========================================================

  getMyCertifications(userId: number): Observable<Certification[]> {
    return this.http.get<Certification[]>(`${this.BASE_URL}/certifications/user/${userId}`);
  }

  getCertificationsByUserId(userId: number): Observable<Certification[]> {
    return this.getMyCertifications(userId);
  }

  addCertification(userId: number, data: Partial<Certification>): Observable<Certification> {
    return this.http.post<Certification>(`${this.BASE_URL}/certifications/user/${userId}`, data);
  }

  updateCertification(certId: number, userId: number, data: Partial<Certification>): Observable<Certification> {
    return this.http.put<Certification>(`${this.BASE_URL}/certifications/${certId}/user/${userId}`, data);
  }

  deleteCertification(certId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/certifications/${certId}/user/${userId}`);
  }

  // =========================================================
  // ENDORSEMENTS — /api/endorsements
  // =========================================================

  getEndorsementsBySkill(skillId: number): Observable<Endorsement[]> {
    return this.http.get<Endorsement[]>(`${this.BASE_URL}/endorsements/skill/${skillId}`);
  }

  addEndorsement(
    skillId: number,
    data: { endorserId: number; comment?: string | null }
  ): Observable<Endorsement> {
    return this.http.post<Endorsement>(`${this.BASE_URL}/endorsements/skill/${skillId}`, data);
  }

  endorseSkill(
    skillId: number,
    data: { endorserId: number; comment?: string | null }
  ): Observable<Endorsement> {
    return this.addEndorsement(skillId, data);
  }

  countEndorsements(skillId: number): Observable<number> {
    return this.http.get<number>(`${this.BASE_URL}/endorsements/skill/${skillId}/count`);
  }

  // =========================================================
  // REVIEWS — /api/reviews
  // =========================================================

  getReviews(profileId: number): Observable<ProfileReview[]> {
    return this.http.get<ProfileReview[]>(`${this.BASE_URL}/reviews/profiles/${profileId}`);
  }

  getVisibleReviews(profileId: number): Observable<ProfileReview[]> {
    return this.getReviews(profileId);
  }

  getAverageRating(profileId: number): Observable<number> {
    return this.http.get<number>(`${this.BASE_URL}/reviews/profiles/${profileId}/average`);
  }

  getReviewSummary(profileId: number): Observable<ProfileReviewSummary> {
    return this.http.get<ProfileReviewSummary>(`${this.BASE_URL}/reviews/profiles/${profileId}/summary`);
  }

  addReview(profileId: number, data: AddProfileReviewRequest): Observable<ProfileReview> {
    return this.http.post<ProfileReview>(`${this.BASE_URL}/reviews/profiles/${profileId}`, data);
  }

  addProfileReview(profileId: number, data: AddProfileReviewRequest): Observable<ProfileReview> {
    return this.addReview(profileId, data);
  }

  replyToReview(
    reviewId: number,
    freelancerUserId: number,
    data: ReplyToReviewRequest
  ): Observable<ProfileReview> {
    return this.http.put<ProfileReview>(
      `${this.BASE_URL}/reviews/${reviewId}/reply?freelancerUserId=${freelancerUserId}`,
      data
    );
  }

  // =========================================================
  // WORK EXPERIENCE — /api/work-experiences
  // =========================================================

  getMyWorkExperiences(userId: number): Observable<WorkExperience[]> {
    return this.http.get<WorkExperience[]>(`${this.BASE_URL}/work-experiences/user/${userId}`);
  }

  getWorkExperiencesByUserId(userId: number): Observable<WorkExperience[]> {
    return this.getMyWorkExperiences(userId);
  }

  getWorkExperienceById(expId: number, userId: number): Observable<WorkExperience> {
    return this.http.get<WorkExperience>(`${this.BASE_URL}/work-experiences/${expId}/user/${userId}`);
  }

  addWorkExperience(userId: number, data: Partial<WorkExperience>): Observable<WorkExperience> {
    return this.http.post<WorkExperience>(`${this.BASE_URL}/work-experiences/user/${userId}`, data);
  }

  updateWorkExperience(expId: number, userId: number, data: Partial<WorkExperience>): Observable<WorkExperience> {
    return this.http.put<WorkExperience>(`${this.BASE_URL}/work-experiences/${expId}/user/${userId}`, data);
  }

  deleteWorkExperience(expId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/work-experiences/${expId}/user/${userId}`);
  }

  getTotalWorkExperienceDuration(userId: number): Observable<number> {
    return this.http.get<number>(`${this.BASE_URL}/work-experiences/user/${userId}/total-duration`);
  }

  // =========================================================
  // EDUCATION — /api/educations
  // =========================================================

  getMyEducations(userId: number): Observable<Education[]> {
    return this.http.get<Education[]>(`${this.BASE_URL}/educations/user/${userId}`);
  }

  addEducation(userId: number, data: Partial<Education>): Observable<Education> {
    return this.http.post<Education>(`${this.BASE_URL}/educations/user/${userId}`, data);
  }

  updateEducation(eduId: number, userId: number, data: Partial<Education>): Observable<Education> {
    return this.http.put<Education>(`${this.BASE_URL}/educations/${eduId}/user/${userId}`, data);
  }

  deleteEducation(eduId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/educations/${eduId}/user/${userId}`);
  }

  // =========================================================
  // RECOMMENDATIONS — /api/recommendations
  // =========================================================

  getCareerPath(userId: number): Observable<CareerPathResponse> {
    return this.http.get<CareerPathResponse>(`${this.BASE_URL}/recommendations/user/${userId}/career-path`);
  }

  getSkillGapFromRecommendations(userId: number): Observable<SkillGapResponse> {
    return this.http.get<SkillGapResponse>(`${this.BASE_URL}/recommendations/user/${userId}/skill-gap`);
  }

  // =========================================================
  // REPORTS — /api/reports
  // =========================================================

  reportProfile(
    profileId: number,
    data: { reporterId: number; category: string; description: string }
  ): Observable<any> {
    return this.http.post(`${this.BASE_URL}/reports/profile/${profileId}`, data);
  }

  // =========================================================
  // PROFILE VIEWS — /api/views
  // =========================================================

  recordProfileView(profileId: number, viewerId?: number): Observable<any> {
    let params = new HttpParams();
    if (viewerId !== undefined && viewerId !== null) {
      params = params.set('viewerId', String(viewerId));
    }
    return this.http.post(`${this.BASE_URL}/views/profiles/${profileId}`, null, { params });
  }

  getProfileViewsCount(profileId: number): Observable<number> {
    return this.http.get<number>(`${this.BASE_URL}/views/profiles/${profileId}/count`);
  }

  getProfileViewsAnalytics(profileId: number): Observable<any> {
    return this.http.get<any>(`${this.BASE_URL}/views/profiles/${profileId}/analytics`);
  }

  // =========================================================
  // NOTIFICATIONS — /api/notifications
  // =========================================================

  getUnreadNotifications(userId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.BASE_URL}/notifications/user/${userId}/unread`);
  }

  getUnreadNotificationsCount(userId: number): Observable<number> {
    return this.http.get<number>(`${this.BASE_URL}/notifications/user/${userId}/count`);
  }

  markAllNotificationsAsRead(userId: number): Observable<void> {
    return this.http.put<void>(`${this.BASE_URL}/notifications/user/${userId}/read-all`, {});
  }

  // =========================================================
  // USER SERVICE — endpoints publics (port 8081)
  // Appelés directement sans passer par ApiService
  // /users/{id}/trust-level est en permitAll() dans user-service SecurityConfig
  // =========================================================

  getUserTrustLevel(userId: number): Observable<{ userId: number; trustLevel: number }> {
    return this.http.get<{ userId: number; trustLevel: number }>(
      `${this.USER_BASE_URL}/users/${userId}/trust-level`
    );
  }


  exportMyCv(userId: number): void {
  this.http.get(`${this.BASE_URL}/export/profiles/${userId}/cv`, {
    responseType: 'blob'
  }).subscribe(blob => {
    const url = globalThis.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `cv-${userId}.pdf`;
    a.click();
    globalThis.URL.revokeObjectURL(url);
  });
}


// =========================================================
// ML SERVICE — /api/ml
// =========================================================

getMlTrustScore(userId: number, kycVerified = false, twoFactorEnabled = false): Observable<any> {
  return this.http.get<any>(
    `${this.BASE_URL}/ml/profiles/user/${userId}/trust-score`,
    { params: { kycVerified: String(kycVerified), twoFactorEnabled: String(twoFactorEnabled) } }
  );
}
}


