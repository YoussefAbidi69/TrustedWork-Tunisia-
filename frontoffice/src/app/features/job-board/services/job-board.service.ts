import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { JOB_BOARD_API } from '../../../core/services/api.service';
import {
  CareerInsightResponse,
  ConversationSummary,
  CreateApplicationRequest,
  CreateJobRequest,
  JobApplication,
  JobFilters,
  JobOffer,
  MarketInsight,
  MarketOverview,
  MarketForecast,
  MarketCategory,
  SalaryInsight,
  MatchFreelancerRow,
  MessageDto,
  Page,
  PreviewSkillsResponse,
  RecommendationRow,
  ConversationMessageRequest,
  ScheduleMeetRequest,
  ScheduleMeetResponse,
  SuccessPrediction,
  SuccessPredictionRequest,
  UpdateJobRequest,
  GenerateCoverLetterRequest,
  GenerateCoverLetterResponse,
} from '../models/job-board.models';

/** Typed HTTP client for the Smart Job Board service (8082). */
@Injectable({ providedIn: 'root' })
export class JobBoardService {
  private readonly base = JOB_BOARD_API;

  constructor(private http: HttpClient) {}

  getJobs(
    filters: JobFilters,
    page: number,
    size: number,
  ): Observable<Page<JobOffer>> {
    let p = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (filters.category) {
      p = p.set('category', filters.category);
    }
    if (filters.budgetMin != null) {
      p = p.set('budgetMin', String(filters.budgetMin));
    }
    if (filters.budgetMax != null) {
      p = p.set('budgetMax', String(filters.budgetMax));
    }
    if (filters.location) {
      p = p.set('location', filters.location);
    }
    if (filters.remote != null) {
      p = p.set('remote', String(filters.remote));
    }
    if (filters.skills?.length) {
      for (const s of filters.skills) {
        p = p.append('skills', s);
      }
    }
    return this.http.get<Page<JobOffer>>(`${this.base}/jobs`, { params: p });
  }

  getJobById(id: number): Observable<JobOffer> {
    return this.http.get<JobOffer>(`${this.base}/jobs/${id}`);
  }

  getMyJobs(page: number, size: number): Observable<Page<JobOffer>> {
    const p = new HttpParams()
      .set('mine', 'true')
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<Page<JobOffer>>(`${this.base}/jobs`, { params: p });
  }

  createJob(dto: CreateJobRequest): Observable<JobOffer> {
    return this.http.post<JobOffer>(`${this.base}/jobs`, dto);
  }

  updateJob(id: number, dto: UpdateJobRequest): Observable<JobOffer> {
    return this.http.put<JobOffer>(`${this.base}/jobs/${id}`, dto);
  }

  publishJob(id: number): Observable<JobOffer> {
    return this.http.post<JobOffer>(`${this.base}/jobs/${id}/publish`, {});
  }

  closeJob(id: number): Observable<JobOffer> {
    return this.http.post<JobOffer>(`${this.base}/jobs/${id}/close`, {});
  }

  deleteJob(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/jobs/${id}`);
  }

  previewSkillsFromDescription(
    description: string,
  ): Observable<PreviewSkillsResponse> {
    return this.http.post<PreviewSkillsResponse>(
      `${this.base}/jobs/preview-skills`,
      { description },
    );
  }

  getJobMatches(id: number): Observable<MatchFreelancerRow[]> {
    return this.http.get<MatchFreelancerRow[]>(
      `${this.base}/jobs/${id}/matches`,
    );
  }

  getJobMatch(id: number): Observable<MatchFreelancerRow[]> {
    return this.http.get<MatchFreelancerRow[]>(
      `${this.base}/jobs/${id}/match`,
    );
  }

  /** ADMIN — triggers server-side market aggregation snapshot */
  refreshMarketInsights(): Observable<MarketInsight[]> {
    return this.http.post<MarketInsight[]>(
      `${this.base}/market-insights/refresh`,
      {},
    );
  }

  submitApplication(dto: CreateApplicationRequest): Observable<JobApplication> {
    const body = {
      jobOfferId: dto.jobOfferId,
      coverLetter: dto.coverLetter,
      proposedRate: dto.proposedRate,
      freelancerSkills: dto.freelancerSkills ?? dto.declaredSkills ?? [],
    };
    return this.http
      .post<JobApplication>(`${this.base}/applications`, body)
      .pipe(
        catchError((err) => {
          const msg =
            err.error?.message ??
            err.error?.error ??
            'Failed to submit application';
          return throwError(() => Object.assign(err, { userMessage: msg }));
        }),
      );
  }

  applyToJob(
    jobId: number,
    payload: { coverLetter: string; proposedRate: number; freelancerSkills?: string[] },
  ): Observable<JobApplication> {
    return this.http
      .post<JobApplication>(`${this.base}/jobs/${jobId}/apply`, payload)
      .pipe(
        catchError((err) => {
          const msg =
            err.error?.message ??
            err.error?.error ??
            'Failed to submit application';
          return throwError(() => Object.assign(err, { userMessage: msg }));
        }),
      );
  }

  getJobApplications(jobId: number): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>(
      `${this.base}/jobs/${jobId}/applications`,
    );
  }

  getMyApplications(): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>(`${this.base}/applications/my`);
  }

  updateApplicationStatus(
    id: number,
    status: string,
  ): Observable<JobApplication> {
    return this.http.put<JobApplication>(
      `${this.base}/applications/${id}/status`,
      { status },
    );
  }

  withdrawApplication(id: number): Observable<JobApplication> {
    return this.http.post<JobApplication>(
      `${this.base}/applications/${id}/withdraw`,
      {},
    );
  }

  getRecommendations(
    freelancerId: number,
    skills: string[] = [],
  ): Observable<RecommendationRow[]> {
    let p = new HttpParams();
    for (const s of skills) {
      if (s?.trim()) {
        p = p.append('skills', s.trim());
      }
    }
    return this.http.get<RecommendationRow[]>(
      `${this.base}/recommendations/${freelancerId}`,
      { params: p },
    );
  }

  getMarketInsights(): Observable<MarketInsight[]> {
    return this.http.get<MarketInsight[]>(`${this.base}/market-insights`);
  }

  getMarketOverview(): Observable<MarketOverview> {
    return this.http.get<MarketOverview>(`${this.base}/market/overview`);
  }

  getSalaryInsights(): Observable<SalaryInsight[]> {
    return this.http.get<SalaryInsight[]>(`${this.base}/market/salary-insights`);
  }

  getMarketForecast(): Observable<MarketForecast[]> {
    return this.http.get<MarketForecast[]>(`${this.base}/market/forecast`);
  }

  getMarketCategories(skills: string[]): Observable<MarketCategory[]> {
    let params = new HttpParams();
    for (const skill of skills || []) {
      if (skill?.trim()) {
        params = params.append('skills', skill.trim());
      }
    }
    return this.http.get<MarketCategory[]>(`${this.base}/market/categories`, { params });
  }

  getCareerInsights(
    freelancerId: number,
    skills: string[],
  ): Observable<CareerInsightResponse> {
    let p = new HttpParams();
    for (const s of skills) {
      p = p.append('skills', s);
    }
    return this.http.get<CareerInsightResponse>(
      `${this.base}/career-insights/${freelancerId}`,
      { params: p },
    );
  }

  getCareerTrajectory(skills: string[]): Observable<CareerInsightResponse> {
    return this.http.post<CareerInsightResponse>(
      `${this.base}/career/trajectory`,
      { skills: skills || [] },
    );
  }

  getSuccessPrediction(
    dto: SuccessPredictionRequest,
  ): Observable<SuccessPrediction> {
    const p = new HttpParams().set('freelancerId', String(dto.freelancerId));
    return this.http.get<SuccessPrediction>(
      `${this.base}/jobs/${dto.jobOfferId}/success-prediction`,
      { params: p },
    );
  }

  postSuccessPrediction(
    dto: SuccessPredictionRequest & { freelancerSkills?: string[] },
  ): Observable<SuccessPrediction> {
    return this.http.post<SuccessPrediction>(
      `${this.base}/success-prediction`,
      dto,
    );
  }

  generateCoverLetter(
    req: GenerateCoverLetterRequest,
  ): Observable<GenerateCoverLetterResponse> {
    return this.http.post<GenerateCoverLetterResponse>(
      `${this.base}/generate-cover-letter`,
      req,
    );
  }

  /* ── Messaging ── */

  sendMessage(
    conversationIdOrReq: string | {
      jobOfferId: number;
      receiverId: number;
      content: string;
      type?: 'text' | 'file';
      fileUrl?: string;
    },
    req?: ConversationMessageRequest,
  ): Observable<MessageDto> {
    if (typeof conversationIdOrReq === 'string') {
      return this.http.post<MessageDto>(
        `${this.base}/messages/conversations/${conversationIdOrReq}`,
        req!,
      );
    }
    return this.http.post<MessageDto>(`${this.base}/messages`, conversationIdOrReq);
  }

  getConversation(conversationId: string): Observable<MessageDto[]>;
  getConversation(jobId: number, peerId: number): Observable<MessageDto[]>;
  getConversation(conversationIdOrJobId: string | number, peerId?: number): Observable<MessageDto[]> {
    if (typeof conversationIdOrJobId === 'number') {
      const p = new HttpParams()
        .set('jobId', String(conversationIdOrJobId))
        .set('peerId', String(peerId!));
      return this.http.get<MessageDto[]>(`${this.base}/messages`, { params: p });
    }
    return this.http.get<MessageDto[]>(
      `${this.base}/messages/conversations/${conversationIdOrJobId}`,
    );
  }

  getConversations(): Observable<ConversationSummary[]> {
    return this.http.get<ConversationSummary[]>(
      `${this.base}/messages/conversations`,
    );
  }

  getUnreadCount(): Observable<{ unread: number }> {
    return this.http.get<{ unread: number }>(
      `${this.base}/messages/unread-count`,
    );
  }

  scheduleMeet(req: ScheduleMeetRequest): Observable<ScheduleMeetResponse> {
    return this.http.post<ScheduleMeetResponse>(
      `${this.base}/messages/schedule-meet`,
      req,
    );
  }

  markMessagesRead(jobId: number, peerId: number): Observable<any> {
    const p = new HttpParams()
      .set('jobId', String(jobId))
      .set('peerId', String(peerId));
    return this.http.put(`${this.base}/messages/read`, null, { params: p });
  }
}
