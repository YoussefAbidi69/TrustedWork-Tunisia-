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
  ProfileReviewSummary,
  ProfileReport,
  WorkExperience,
  Education,
  CompletenessResponse,
  CareerPathResponse,
  SkillGapResponse,
  SkillGapRecommendation
} from '../models/freelancer.model';

@Injectable({ providedIn: 'root' })
export class FreelancerProfileService {

  private baseUrl = '/api';
    constructor(private http: HttpClient) {}

  // ────────────────── PROFILS ──────────────────

  getAllProfiles(): Observable<FreelancerProfile[]> {
    return this.http.get<FreelancerProfile[]>(`${this.baseUrl}/profiles`);
  }

  getAllPublicProfiles(): Observable<FreelancerProfile[]> {
    return this.getAllProfiles();
  }

  getProfileById(profileId: number): Observable<FreelancerProfile> {
    return this.http.get<FreelancerProfile>(`${this.baseUrl}/profiles/${profileId}`);
  }

  getProfileByUserId(userId: number): Observable<FreelancerProfile> {
    const safeUserId = Math.trunc(userId);
    return this.http.get<FreelancerProfile>(`${this.baseUrl}/profiles/user/${safeUserId}`);
  }

  getRankingByRegion(region: string): Observable<FreelancerProfile[]> {
    return this.http.get<FreelancerProfile[]>(`${this.baseUrl}/profiles/ranking/${region}`);
  }

  getCompleteness(userId: number): Observable<CompletenessResponse> {
    return this.http.get<CompletenessResponse>(`${this.baseUrl}/profiles/user/${userId}/completeness`);
  }

  deleteProfile(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/profiles/user/${userId}`);
  }

  updateAvailability(userId: number, status: string): Observable<FreelancerProfile> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<FreelancerProfile>(
      `${this.baseUrl}/profiles/user/${userId}/availability`,
      null,
      { params }
    );
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

    return this.http.get<FreelancerProfile[]>(
      `${this.baseUrl}/profiles/search`,
      { params }
    );
  }

  // ────────────────── SKILLS ──────────────────

  getSkillsByUserId(userId: number): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.baseUrl}/skills/user/${userId}`);
  }

  getSkillGaps(userId: number): Observable<SkillGapResponse> {
    return this.http.get<SkillGapResponse>(`${this.baseUrl}/skills/user/${userId}/gaps`);
  }

  getSkillAuthenticity(skillId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/skills/${skillId}/authenticity`);
  }

  deleteSkill(skillId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/skills/${skillId}/user/${userId}`);
  }

  // ────────────────── PORTFOLIO ──────────────────

  getPortfolio(userId: number): Observable<PortfolioItem[]> {
    return this.http.get<PortfolioItem[]>(`${this.baseUrl}/portfolio/user/${userId}`);
  }

  getPinnedPortfolio(userId: number): Observable<PortfolioItem[]> {
    return this.http.get<PortfolioItem[]>(`${this.baseUrl}/portfolio/user/${userId}/pinned`);
  }

  pinPortfolioItem(itemId: number, userId: number): Observable<PortfolioItem> {
    return this.http.patch<PortfolioItem>(`${this.baseUrl}/portfolio/${itemId}/user/${userId}/pin`, {});
  }

  unpinPortfolioItem(itemId: number, userId: number): Observable<PortfolioItem> {
    return this.http.patch<PortfolioItem>(`${this.baseUrl}/portfolio/${itemId}/user/${userId}/unpin`, {});
  }

  deletePortfolioItem(itemId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/portfolio/${itemId}/user/${userId}`);
  }

  // ────────────────── CERTIFICATIONS ──────────────────

  getCertifications(userId: number): Observable<Certification[]> {
    return this.http.get<Certification[]>(`${this.baseUrl}/certifications/user/${userId}`);
  }

  deleteCertification(certId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/certifications/${certId}/user/${userId}`);
  }

  // ────────────────── ENDORSEMENTS ──────────────────

  getEndorsementsBySkill(skillId: number): Observable<Endorsement[]> {
    return this.http.get<Endorsement[]>(`${this.baseUrl}/endorsements/skill/${skillId}`);
  }

  countEndorsements(skillId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/endorsements/skill/${skillId}/count`);
  }

  // ────────────────── WORK EXPERIENCES ──────────────────

  getWorkExperiences(userId: number): Observable<WorkExperience[]> {
    return this.http.get<WorkExperience[]>(`${this.baseUrl}/work-experiences/user/${userId}`);
  }

  getWorkExperienceById(expId: number, userId: number): Observable<WorkExperience> {
    return this.http.get<WorkExperience>(`${this.baseUrl}/work-experiences/${expId}/user/${userId}`);
  }

  addWorkExperience(userId: number, data: Partial<WorkExperience>): Observable<WorkExperience> {
    return this.http.post<WorkExperience>(`${this.baseUrl}/work-experiences/user/${userId}`, data);
  }

  updateWorkExperience(expId: number, userId: number, data: Partial<WorkExperience>): Observable<WorkExperience> {
    return this.http.put<WorkExperience>(`${this.baseUrl}/work-experiences/${expId}/user/${userId}`, data);
  }

  deleteWorkExperience(expId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/work-experiences/${expId}/user/${userId}`);
  }

  getTotalWorkExperienceDuration(userId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/work-experiences/user/${userId}/total-duration`);
  }

  // ────────────────── EDUCATIONS ──────────────────

  getEducations(userId: number): Observable<Education[]> {
    return this.http.get<Education[]>(`${this.baseUrl}/educations/user/${userId}`);
  }

  addEducation(userId: number, data: Partial<Education>): Observable<Education> {
    return this.http.post<Education>(`${this.baseUrl}/educations/user/${userId}`, data);
  }

  updateEducation(eduId: number, userId: number, data: Partial<Education>): Observable<Education> {
    return this.http.put<Education>(`${this.baseUrl}/educations/${eduId}/user/${userId}`, data);
  }

  deleteEducation(eduId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/educations/${eduId}/user/${userId}`);
  }

  // ────────────────── REVIEWS ──────────────────

  getReviewsByProfile(profileId: number): Observable<ProfileReview[]> {
    return this.http.get<ProfileReview[]>(`${this.baseUrl}/reviews/profiles/${profileId}`);
  }

  getReviews(profileId: number): Observable<ProfileReview[]> {
    return this.getReviewsByProfile(profileId);
  }

  getAllReviews(): Observable<ProfileReview[]> {
    return this.http.get<ProfileReview[]>(`${this.baseUrl}/reviews`);
  }

  getAverageRating(profileId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/reviews/profiles/${profileId}/average`);
  }

  getReviewSummary(profileId: number): Observable<ProfileReviewSummary> {
    return this.http.get<ProfileReviewSummary>(`${this.baseUrl}/reviews/profiles/${profileId}/summary`);
  }

  replyToReview(reviewId: number, freelancerUserId: number, reply: string): Observable<ProfileReview> {
    return this.http.put<ProfileReview>(
      `${this.baseUrl}/reviews/${reviewId}/reply`,
      { reply },
      { params: new HttpParams().set('freelancerUserId', freelancerUserId) }
    );
  }

  hideReview(reviewId: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/reviews/${reviewId}/hide`, {});
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/reviews/${reviewId}`);
  }

  resolveReview(reviewId: number): Observable<ProfileReview> {
    return this.http.patch<ProfileReview>(
      `${this.baseUrl}/reviews/${reviewId}/resolve`,
      {}
    );
  }

  rejectReview(reviewId: number): Observable<ProfileReview> {
    return this.http.patch<ProfileReview>(
      `${this.baseUrl}/reviews/${reviewId}/reject`,
      {}
    );
  }

  restoreReview(reviewId: number) {
    return this.http.put(
      `${this.baseUrl}/reviews/${reviewId}/restore`,
      {}
    );
  }

  // ────────────────── REPORTS (ADMIN) ──────────────────

  getPendingReports(): Observable<ProfileReport[]> {
    return this.http.get<ProfileReport[]>(`${this.baseUrl}/reports/pending`);
  }

  getAllReports(): Observable<ProfileReport[]> {
    return this.http.get<ProfileReport[]>(`${this.baseUrl}/reports`);
  }

  resolveReport(reportId: number, status: string): Observable<ProfileReport> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<ProfileReport>(
      `${this.baseUrl}/reports/${reportId}/resolve`,
      null,
      { params }
    );
  }

  updateReportStatus(
    reportId: number,
    status: 'IN_REVIEW' | 'RESOLVED' | 'REJECTED'
  ): Observable<ProfileReport> {
    return this.http.patch<ProfileReport>(
      `${this.baseUrl}/reports/${reportId}/status?status=${status}`,
      {}
    );
  }

  // ────────────────── RECOMMENDATIONS ──────────────────

  getCareerPath(userId: number): Observable<CareerPathResponse> {
    return this.http.get<CareerPathResponse>(`${this.baseUrl}/recommendations/user/${userId}/career-path`);
  }

  getSkillGapRecommendations(userId: number): Observable<SkillGapRecommendation> {
    return this.http.get<SkillGapRecommendation>(`${this.baseUrl}/recommendations/user/${userId}/skill-gap`);
  }

  // ────────────────── DASHBOARD / TRENDING ──────────────────

  getTrendingProfiles(): Observable<FreelancerProfile[]> {
    return this.getAllProfiles();
  }

  getPlatformStats(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/admin/dashboard/module02`);
  }

  exportProfilesExcel(): void {
    this.http.get(`${this.baseUrl}/export/admin/profiles/excel`, {
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) return;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'profiles.xlsx';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Export Excel erreur :', err.status)
    });
  }
  
  exportAdminReportPdf(): void {
    this.http.get(`${this.baseUrl}/export/admin/report/pdf`, {
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) return;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'admin-report.pdf';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Export PDF erreur :', err.status)
    });
  }
}