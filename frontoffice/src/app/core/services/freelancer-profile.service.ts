import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
  SkillGapResponse
} from '../models/freelancer.model';

/**
 * Service HTTP — communication avec le freelancer-profile-service (port 8082)
 */
@Injectable({
  providedIn: 'root'
})
export class FreelancerProfileService {

  private readonly BASE_URL = 'http://localhost:8082/api';

  constructor(private http: HttpClient) {}

  // ===== PROFIL =====

  createProfile(data: Partial<FreelancerProfile>): Observable<FreelancerProfile> {
    return this.http.post<FreelancerProfile>(`${this.BASE_URL}/profiles`, data);
  }

  getProfileByUserId(userId: number): Observable<FreelancerProfile> {
    return this.http.get<FreelancerProfile>(`${this.BASE_URL}/profiles/user/${userId}`);
  }

  updateProfile(userId: number, data: Partial<FreelancerProfile>): Observable<FreelancerProfile> {
    return this.http.put<FreelancerProfile>(`${this.BASE_URL}/profiles/user/${userId}`, data);
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

  // ===== SKILLS =====

  getMySkills(userId: number): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.BASE_URL}/skills/user/${userId}`);
  }

  addSkill(userId: number, data: { name: string; examScore: number }): Observable<Skill> {
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

  // ===== PORTFOLIO =====

  getMyPortfolio(userId: number): Observable<PortfolioItem[]> {
    return this.http.get<PortfolioItem[]>(`${this.BASE_URL}/portfolio/user/${userId}`);
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

  // ===== CERTIFICATIONS =====

  getMyCertifications(userId: number): Observable<Certification[]> {
    return this.http.get<Certification[]>(`${this.BASE_URL}/certifications/user/${userId}`);
  }

  addCertification(userId: number, data: Partial<Certification>): Observable<Certification> {
    return this.http.post<Certification>(`${this.BASE_URL}/certifications/user/${userId}`, data);
  }

  deleteCertification(certId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/certifications/${certId}/user/${userId}`);
  }

  // ===== ENDORSEMENTS =====

  getEndorsementsBySkill(skillId: number): Observable<Endorsement[]> {
    return this.http.get<Endorsement[]>(`${this.BASE_URL}/endorsements/skill/${skillId}`);
  }

  addEndorsement(skillId: number, data: { endorserId: number; comment: string }): Observable<Endorsement> {
    return this.http.post<Endorsement>(`${this.BASE_URL}/endorsements/skill/${skillId}`, data);
  }

  // ===== REVIEWS =====

  getReviews(profileId: number): Observable<ProfileReview[]> {
    return this.http.get<ProfileReview[]>(`${this.BASE_URL}/reviews/profile/${profileId}`);
  }

  getAverageRating(profileId: number): Observable<number> {
    return this.http.get<number>(`${this.BASE_URL}/reviews/profile/${profileId}/average`);
  }

  addReview(profileId: number, data: { clientId: number; rating: number; comment: string }): Observable<ProfileReview> {
    return this.http.post<ProfileReview>(`${this.BASE_URL}/reviews/profile/${profileId}`, data);
  }

  // ===== WORK EXPERIENCE =====

  getMyWorkExperiences(userId: number): Observable<WorkExperience[]> {
    return this.http.get<WorkExperience[]>(`${this.BASE_URL}/work-experiences/user/${userId}`);
  }

  addWorkExperience(userId: number, data: Partial<WorkExperience>): Observable<WorkExperience> {
    return this.http.post<WorkExperience>(`${this.BASE_URL}/work-experiences/user/${userId}`, data);
  }

  deleteWorkExperience(expId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/work-experiences/${expId}/user/${userId}`);
  }

  // ===== RECOMMENDATIONS =====

  getCareerPath(userId: number): Observable<CareerPathResponse> {
    return this.http.get<CareerPathResponse>(`${this.BASE_URL}/recommendations/user/${userId}/career-path`);
  }

  // ===== EDUCATION =====

  getMyEducations(userId: number): Observable<Education[]> {
    return this.http.get<Education[]>(`${this.BASE_URL}/educations/user/${userId}`);
  }

  addEducation(userId: number, data: Partial<Education>): Observable<Education> {
    return this.http.post<Education>(`${this.BASE_URL}/educations/user/${userId}`, data);
  }

  deleteEducation(eduId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/educations/${eduId}/user/${userId}`);
  }

  // ===== REPORTS =====

reportProfile(profileId: number, data: { reporterId: number; reason: string }): Observable<any> {
  return this.http.post(`${this.BASE_URL}/reports/profile/${profileId}`, data);
}
}