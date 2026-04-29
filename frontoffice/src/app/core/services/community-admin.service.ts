import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface CommunityDTO {
  id: number;
  name: string;
  description?: string;
  managerId?: number;  // alias
  createdBy?: number;  // actual backend field (CommunityResponse.createdBy)
  rules?: string;
  createdAt?: string;
  status?: string;
}

export interface PostDTO {
  id: number;
  title: string;
  content: string;
  authorId?: number;     // frontend alias — backend sends createdBy
  createdBy?: number;   // actual backend field
  communityId?: number;
  createdAt?: string;
  status?: string;
  reportCount?: number;
  upvoteCount?: number;
  downvoteCount?: number;
}

export interface CommentDTO {
  id: number;
  content: string;
  postId?: number;
  authorId?: number;  // alias
  userId?: number;    // actual backend field (CommentResponse.userId)
  createdAt?: string;
  status?: string;
}

export interface BlockDTO {
  id: number;
  sectionId?: number;
  title?: string;
  content?: string;
  fileUrl?: string;
  orderIndex?: number;
  type?: string;
  blockerId?: number;
  blockedId?: number;
  reason?: string;
  createdAt?: string;
}

export interface ReportDTO {
  id: number;
  reporterId?: number;  // alias
  reportedBy?: number;  // actual backend field
  reportedId?: number;
  postId?: number;
  type: string; // 'POST', 'COMMENT', 'USER'
  reason: string;
  status: 'OPEN' | 'RESOLVED' | 'REJECTED';
  createdAt?: string;
}

export interface CourseReportDTO {
  id: number;
  reporterId?: number;  // alias
  reportedBy?: number;  // actual backend field
  courseId: number;
  reason: string;
  status: 'OPEN' | 'RESOLVED' | 'REJECTED';
  createdAt?: string;
}

export interface VoteDTO {
  id: number;
  voterId: number;
  postId?: number;
  commentId?: number;
  type: 'UPVOTE' | 'DOWNVOTE';
  createdAt?: string;
}

export interface CourseVoteDTO {
  id: number;
  voterId: number;
  courseId: number;
  type: 'UPVOTE' | 'DOWNVOTE';
  createdAt?: string;
}

export interface ContributionDTO {
  id: number;
  userId: number;
  action: string;
  points: number;
  createdAt?: string;
}

export interface CourseDTO {
  id: number;
  title: string;
  description: string;
  authorId?: number;
  communityId?: number;
  status: string;
  createdAt?: string;
}

export interface CourseCommentDTO {
  id: number;
  content: string;
  courseId: number;
  authorId?: number;  // alias
  userId?: number;    // actual backend field (CourseCommentResponse.userId)
  status?: string;
  createdAt?: string;
}

export interface SectionDTO {
  id: number;
  courseId: number;
  title: string;
  orderIndex: number;
  blocks?: BlockDTO[];   // already embedded in SectionResponse from backend
  createdAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CommunityAdminService {
  private readonly baseUrl = environment.communityUrl;

  constructor(private http: HttpClient, private auth: AuthService) {}

  // Communities
  getCommunities(): Observable<CommunityDTO[]> { 
    return this.http.get<CommunityDTO[]>(`${this.baseUrl}/communities`).pipe(
      map(items => items.map(i => ({ ...i, managerId: i.managerId || i.createdBy })))
    ); 
  }
  getCommunity(id: number): Observable<CommunityDTO> { 
    return this.http.get<CommunityDTO>(`${this.baseUrl}/communities/${id}`).pipe(
      map(i => ({ ...i, managerId: i.managerId || i.createdBy }))
    ); 
  }
  deleteCommunity(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/communities/${id}`); }

  // Posts
  getPosts(): Observable<PostDTO[]> { 
    return this.http.get<PostDTO[]>(`${this.baseUrl}/posts`).pipe(
      map(items => items.map(i => ({ ...i, authorId: i.authorId || i.createdBy })))
    ); 
  }
  getPost(id: number): Observable<PostDTO> { 
    return this.http.get<PostDTO>(`${this.baseUrl}/posts/${id}`).pipe(
      map(i => ({ ...i, authorId: i.authorId || i.createdBy }))
    ); 
  }
  getPostsByCommunity(communityId: number): Observable<PostDTO[]> { 
    return this.http.get<PostDTO[]>(`${this.baseUrl}/posts?communityId=${communityId}`).pipe(
      map(items => items.map(i => ({ ...i, authorId: i.authorId || i.createdBy })))
    ); 
  }
  deletePost(id: number, userId: number): Observable<void> { 
    return this.http.delete<void>(`${this.baseUrl}/posts/${id}?userId=${userId}`); 
  }

  // Comments — aggregate across all posts
  getComments(): Observable<CommentDTO[]> {
    return this.getPosts().pipe(
      switchMap(posts => {
        if (!posts || posts.length === 0) return of([]);
        return forkJoin(posts.map(p => this.getCommentsByPost(p.id).pipe(catchError(() => of([])))));
      }),
      map((arrays: CommentDTO[][]) => ([] as CommentDTO[]).concat(...arrays))
    );
  }
  getCommentsByPost(postId: number): Observable<CommentDTO[]> { 
    return this.http.get<CommentDTO[]>(`${this.baseUrl}/comments/post/${postId}`).pipe(
      map(items => items.map(i => ({ ...i, authorId: i.authorId || i.userId })))
    ); 
  }
  deleteComment(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/comments/${id}`); }
  moderateComment(id: number, action: string): Observable<void> { return this.http.post<void>(`${this.baseUrl}/comments/${id}/moderate`, { action }); }

  // Blocks — aggregate across all sections (called from section list)
  getBlock(id: number): Observable<BlockDTO> { return this.http.get<BlockDTO>(`${this.baseUrl}/blocks/${id}`); }
  getBlocks(): Observable<BlockDTO[]> {
    return this.getSections().pipe(
      switchMap(sections => {
        if (!sections || sections.length === 0) return of([]);
        return forkJoin(sections.map(s => this.getBlocksBySection(s.id).pipe(catchError(() => of([])))));
      }),
      map((arrays: BlockDTO[][]) => ([] as BlockDTO[]).concat(...arrays))
    );
  }
  getBlocksBySection(sectionId: number): Observable<BlockDTO[]> { return this.http.get<BlockDTO[]>(`${this.baseUrl}/blocks/section/${sectionId}`); }
  deleteBlock(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/blocks/${id}`); }

  // Reports — aggregate across all posts
  getReports(): Observable<ReportDTO[]> {
    return this.getPosts().pipe(
      switchMap(posts => {
        if (!posts || posts.length === 0) return of([]);
        return forkJoin(posts.map(p => this.getReportsByPost(p.id).pipe(catchError(() => of([])))));
      }),
      map((arrays: ReportDTO[][]) => ([] as ReportDTO[]).concat(...arrays))
    );
  }
  getReportsByPost(postId: number): Observable<ReportDTO[]> { 
    return this.http.get<ReportDTO[]>(`${this.baseUrl}/reports/post/${postId}`).pipe(
      map(items => items.map(i => ({ ...i, reporterId: i.reporterId || i.reportedBy })))
    ); 
  }
  resolveReport(id: number, status: string): Observable<void> { return this.http.put<void>(`${this.baseUrl}/reports/${id}/status?status=${status}`, {}); }
  deleteReport(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/reports/${id}`); }

  // Course Reports — aggregate across all courses
  getCourseReports(): Observable<CourseReportDTO[]> {
    return this.getCourses().pipe(
      switchMap(courses => {
        if (!courses || courses.length === 0) return of([]);
        return forkJoin(courses.map(c => this.getReportsByCourse(c.id).pipe(catchError(() => of([])))));
      }),
      map((arrays: CourseReportDTO[][]) => ([] as CourseReportDTO[]).concat(...arrays))
    );
  }
  getReportsByCourse(courseId: number): Observable<CourseReportDTO[]> { 
    return this.http.get<CourseReportDTO[]>(`${this.baseUrl}/course-reports/course/${courseId}`).pipe(
      map(items => items.map(i => ({ ...i, reporterId: i.reporterId || i.reportedBy })))
    ); 
  }
  resolveCourseReport(id: number, status: string): Observable<void> { return this.http.put<void>(`${this.baseUrl}/course-reports/${id}/status?status=${status}`, {}); }
  deleteCourseReport(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/course-reports/${id}`); }

  // Votes — aggregate across all posts
  getVotes(): Observable<VoteDTO[]> {
    return this.getPosts().pipe(
      switchMap(posts => {
        if (!posts || posts.length === 0) return of([]);
        return forkJoin(posts.map(p => this.http.get<VoteDTO[]>(`${this.baseUrl}/posts/${p.id}?voterId=0`).pipe(
          map((post: any) => (post.votes || []) as VoteDTO[]),
          catchError(() => of([]))
        )));
      }),
      map((arrays: VoteDTO[][]) => ([] as VoteDTO[]).concat(...arrays))
    );
  }

  // Course Votes (no global list endpoint exists on backend)
  getCourseVotes(): Observable<CourseVoteDTO[]> { return of([]); }

  // Contributions — per user only, no global list endpoint
  getContributions(): Observable<ContributionDTO[]> { return of([]); }

  // Courses
  getCourses(): Observable<CourseDTO[]> { return this.http.get<CourseDTO[]>(`${this.baseUrl}/courses`); }
  getCourse(id: number): Observable<CourseDTO> { return this.http.get<CourseDTO>(`${this.baseUrl}/courses/${id}`); }
  getCoursesByCommunity(communityId: number): Observable<CourseDTO[]> { return this.http.get<CourseDTO[]>(`${this.baseUrl}/courses?communityId=${communityId}`); }
  deleteCourse(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/courses/${id}`); }

  getSectionsByCourse(courseId: number): Observable<SectionDTO[]> { return this.http.get<SectionDTO[]>(`${this.baseUrl}/sections/course/${courseId}`); }
  getSection(id: number): Observable<SectionDTO> { return this.http.get<SectionDTO>(`${this.baseUrl}/sections/${id}`); }
  getSections(): Observable<SectionDTO[]> {
    return this.getCourses().pipe(
      switchMap(courses => {
        if (!courses || courses.length === 0) return of([]);
        return forkJoin(courses.map(c => this.getSectionsByCourse(c.id).pipe(catchError(() => of([])))));
      }),
      map((arrays: SectionDTO[][]) => ([] as SectionDTO[]).concat(...arrays))
    );
  }
  deleteSection(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/sections/${id}`); }

  // Course Comments — aggregate across all courses
  getCourseComments(): Observable<CourseCommentDTO[]> {
    return this.getCourses().pipe(
      switchMap(courses => {
        if (!courses || courses.length === 0) return of([]);
        return forkJoin(courses.map(c => this.getCommentsByCourse(c.id).pipe(catchError(() => of([])))));
      }),
      map((arrays: CourseCommentDTO[][]) => ([] as CourseCommentDTO[]).concat(...arrays))
    );
  }
  getCommentsByCourse(courseId: number): Observable<CourseCommentDTO[]> { 
    return this.http.get<CourseCommentDTO[]>(`${this.baseUrl}/course-comments/course/${courseId}`).pipe(
      map(items => items.map(i => ({ ...i, authorId: i.authorId || i.userId })))
    ); 
  }
  deleteCourseComment(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/course-comments/${id}`); }
  moderateCourseComment(id: number, action: string): Observable<void> { return this.http.post<void>(`${this.baseUrl}/course-comments/${id}/moderate`, { action }); }
}
