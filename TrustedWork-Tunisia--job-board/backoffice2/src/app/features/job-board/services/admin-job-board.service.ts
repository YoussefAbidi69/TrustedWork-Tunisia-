import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  AdminJobFilters,
  AdminMarketAnalyticsApiResponse,
  Application,
  JobOffer,
  JobOfferAdminUpdatePayload,
  MarketInsightResponse,
  OfferFlag,
  PageResponse,
  PlatformStatsDto
} from '../models/admin-job-board.models';

@Injectable({ providedIn: 'root' })
export class AdminJobBoardService {
  private readonly API = 'http://localhost:8082/api';

  constructor(private http: HttpClient) {}

  getAdminJobs(filters: AdminJobFilters = {}): Observable<PageResponse<JobOffer>> {
    let p = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));
    if (filters.status) {
      p = p.set('status', filters.status);
    }
    if (filters.category) {
      p = p.set('category', filters.category);
    }
    if (filters.search) {
      p = p.set('search', filters.search);
    }
    if (filters.sort) {
      p = p.set('sort', filters.sort);
    }
    return this.http
      .get<PageResponse<JobOffer>>(`${this.API}/admin/jobs`, { params: p })
      .pipe(catchError(this.handleError));
  }

  getFlaggedJobs(page = 0, size = 200): Observable<JobOffer[]> {
    const p = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http
      .get<PageResponse<JobOffer>>(`${this.API}/admin/jobs/flagged`, { params: p })
      .pipe(
        map((res) => res.content ?? []),
        catchError(this.handleError)
      );
  }

  flagJob(id: number): Observable<JobOffer> {
    return this.http
      .put<JobOffer>(`${this.API}/admin/jobs/${id}/flag`, {})
      .pipe(catchError(this.handleError));
  }

  unflagJob(id: number): Observable<JobOffer> {
    return this.http
      .put<JobOffer>(`${this.API}/admin/jobs/${id}/unflag`, {})
      .pipe(catchError(this.handleError));
  }

  forceCloseJob(id: number): Observable<JobOffer> {
    return this.http
      .put<JobOffer>(`${this.API}/admin/jobs/${id}/force-close`, {})
      .pipe(catchError(this.handleError));
  }

  editJob(id: number, body: JobOfferAdminUpdatePayload): Observable<JobOffer> {
    return this.http
      .put<JobOffer>(`${this.API}/admin/jobs/${id}`, body)
      .pipe(catchError(this.handleError));
  }

  deleteJob(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.API}/admin/jobs/${id}`)
      .pipe(catchError(this.handleError));
  }

  getJobFlags(jobId: number): Observable<OfferFlag[]> {
    return this.http
      .get<OfferFlag[]>(`${this.API}/admin/jobs/${jobId}/flags`)
      .pipe(catchError(this.handleError));
  }

  getAdminApplications(filters: {
    status?: string;
    page?: number;
    size?: number;
    minMatchScore?: number;
  } = {}): Observable<PageResponse<Application>> {
    let p = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));
    if (filters.status) {
      p = p.set('status', filters.status);
    }
    if (filters.minMatchScore != null && filters.minMatchScore > 0) {
      p = p.set('minMatchScore', String(filters.minMatchScore));
    }
    return this.http
      .get<PageResponse<Application>>(`${this.API}/admin/applications`, { params: p })
      .pipe(catchError(this.handleError));
  }

  getMarketAnalytics(): Observable<MarketInsightResponse[]> {
    return this.http
      .get<AdminMarketAnalyticsApiResponse>(`${this.API}/admin/market-analytics`)
      .pipe(
        map((r) =>
          (r.skillInsights ?? []).map((s) => ({
            skillName: s.skill,
            count: s.count,
            trend: s.trend,
            changePercent: s.changePercent,
            lastPeriodCount: s.lastPeriodCount
          }))
        ),
        catchError(this.handleError)
      );
  }

  refreshMarketData(): Observable<void> {
    return this.http
      .post<void>(`${this.API}/admin/market-insights/refresh`, {})
      .pipe(catchError(this.handleError));
  }

  getPlatformStats(): Observable<PlatformStatsDto> {
    return this.http
      .get<PlatformStatsDto>(`${this.API}/admin/stats`)
      .pipe(catchError(this.handleError));
  }

  private handleError(err: unknown): Observable<never> {
    const e = err as { error?: { message?: string } };
    const msg = e.error?.message ?? 'An admin API error occurred';
    return throwError(() => new Error(msg));
  }
}
