import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  Comment,
  Post,
  PostStatus,
  PostType,
  Report,
  Vote,
  VoteType
} from '../models/community.model';

/**
 * HTTP client for ms-community Post, Comment, Vote, and Report APIs
 * (paths under /api match CommunityController, PostController, etc.)
 */
@Injectable({
  providedIn: 'root'
})
export class PostService {
  private readonly baseUrl = `${environment.msCommunity}/api`;

  constructor(private http: HttpClient) {}

  getAll(params?: {
    communityId?: number;
    type?: PostType;
    status?: PostStatus;
    /** When set, each post includes myVote for that user */
    voterId?: number;
  }): Observable<Post[]> {
    let httpParams = new HttpParams();
    if (params?.communityId != null) {
      httpParams = httpParams.set('communityId', String(params.communityId));
    }
    if (params?.type != null) {
      httpParams = httpParams.set('type', params.type);
    }
    if (params?.status != null) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params?.voterId != null) {
      httpParams = httpParams.set('voterId', String(params.voterId));
    }
    return this.http.get<Post[]>(`${this.baseUrl}/posts`, { params: httpParams });
  }

  getById(id: number, voterId?: number): Observable<Post> {
    let params = new HttpParams();
    if (voterId != null) {
      params = params.set('voterId', String(voterId));
    }
    return this.http.get<Post>(`${this.baseUrl}/posts/${id}`, { params });
  }

  create(payload: Partial<Post>): Observable<Post> {
    return this.http.post<Post>(`${this.baseUrl}/posts`, payload);
  }

  update(id: number, payload: Partial<Post>): Observable<Post> {
    return this.http.put<Post>(`${this.baseUrl}/posts/${id}`, payload);
  }

  publish(id: number): Observable<Post> {
    return this.http.post<Post>(`${this.baseUrl}/posts/${id}/publish`, {});
  }

  delete(id: number, userId: number): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.delete<void>(`${this.baseUrl}/posts/${id}`, { params });
  }

  /** Upload a course PDF; returns absolute fileUrl for {@link Post.fileUrl}. */
  uploadCoursePdf(file: File): Observable<{ fileUrl: string }> {
    const body = new FormData();
    body.append('file', file, file.name);
    const url = `${this.baseUrl}/course-files`;
    return this.http.post<unknown>(url, body).pipe(
      map((res) => {
        const fileUrl = PostService.extractUploadFileUrl(res);
        if (!fileUrl) {
          throw new HttpErrorResponse({
            status: 502,
            statusText: 'Bad Gateway',
            url,
            error: {
              message:
                'Upload response did not include a usable file URL. Open the browser Network tab, select the course-files request, and check the JSON body.'
            }
          });
        }
        return { fileUrl };
      })
    );
  }

  private static extractUploadFileUrl(res: unknown): string {
    if (res == null || typeof res !== 'object') {
      return '';
    }
    const o = res as Record<string, unknown>;
    const a = o['fileUrl'];
    const b = o['file_url'];
    const raw =
      (typeof a === 'string' ? a : '') ||
      (typeof b === 'string' ? b : '') ||
      '';
    return raw.trim();
  }

  getComments(postId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.baseUrl}/comments/post/${postId}`);
  }

  addComment(postId: number, content: string, userId: number): Observable<Comment> {
    return this.http.post<Comment>(`${this.baseUrl}/comments/post/${postId}`, {
      content,
      userId
    });
  }

  vote(postId: number, type: VoteType, userId: number): Observable<Vote> {
    const params = new HttpParams()
      .set('postId', String(postId))
      .set('type', type)
      .set('userId', String(userId));
    return this.http.post<Vote>(`${this.baseUrl}/votes`, null, { params });
  }

  report(
    postId: number,
    reason: string,
    description: string,
    reportedBy: number
  ): Observable<Report> {
    const params = new HttpParams()
      .set('reportedBy', String(reportedBy))
      .set('postId', String(postId))
      .set('reason', reason)
      .set('description', description);
    return this.http.post<Report>(`${this.baseUrl}/reports`, null, { params });
  }
}
