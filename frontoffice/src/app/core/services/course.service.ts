import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  Certificate,
  Comment,
  Course,
  Lesson,
  Post,
  Progress,
  Report,
  Section,
  Vote,
  VoteType
} from '../models/community.model';
import { courseDownloadFilename, triggerBlobDownload } from '../utils/file-download.util';

@Injectable({
  providedIn: 'root'
})
export class CourseService {
  private readonly baseUrl = `${environment.msCommunity}/api`;

  constructor(private http: HttpClient) {}

  /** Published courses in a community (for learners). */
  listByCommunity(communityId: number, publishedOnly = true): Observable<Course[]> {
    let params = new HttpParams()
      .set('communityId', String(communityId))
      .set('publishedOnly', publishedOnly ? 'true' : 'false');
    return this.http.get<Course[]>(`${this.baseUrl}/courses`, { params });
  }

  /** All courses authored by a specific user. */
  getMyCourses(userId: number): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.baseUrl}/courses`).pipe(
      map(courses => courses.filter(course => course.authorId === userId))
    );
  }

  createCourse(payload: Partial<Course>): Observable<Course> {
    return this.http.post<Course>(`${this.baseUrl}/courses`, payload);
  }

  updateCourse(id: number, payload: Partial<Course>): Observable<Course> {
    return this.http.put<Course>(`${this.baseUrl}/courses/${id}`, payload);
  }

  createSection(courseId: number, payload: Partial<Section>): Observable<Section> {
    return this.http.post<Section>(`${this.baseUrl}/courses/${courseId}/sections`, payload);
  }

  updateSection(sectionId: number, payload: Partial<Section>): Observable<Section> {
    return this.http.put<Section>(`${this.baseUrl}/sections/${sectionId}`, payload);
  }

  deleteSection(sectionId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sections/${sectionId}`);
  }

  createLesson(sectionId: number, payload: Partial<Lesson>): Observable<Lesson> {
    return this.http.post<Lesson>(`${this.baseUrl}/sections/${sectionId}/lessons`, payload);
  }

  updateLesson(lessonId: number, payload: Partial<Lesson>): Observable<Lesson> {
    return this.http.put<Lesson>(`${this.baseUrl}/lessons/${lessonId}`, payload);
  }

  deleteLesson(lessonId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/lessons/${lessonId}`);
  }

  getCourse(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.baseUrl}/courses/${id}`);
  }

  getSections(courseId: number): Observable<Section[]> {
    return this.http.get<Section[]>(`${this.baseUrl}/courses/${courseId}/sections`);
  }

  getLessons(sectionId: number): Observable<Lesson[]> {
    return this.http.get<Lesson[]>(`${this.baseUrl}/sections/${sectionId}/lessons`);
  }

  getProgress(userId: number, lessonId: number): Observable<Progress> {
    const params = new HttpParams()
      .set('userId', String(userId))
      .set('lessonId', String(lessonId));
    return this.http.get<Progress>(`${this.baseUrl}/progress`, { params });
  }

  markComplete(userId: number, lessonId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/progress`, {
      userId,
      lessonId,
      completed: true
    });
  }

  issueCertificate(userId: number, courseId: number): Observable<Certificate> {
    return this.http.post<Certificate>(`${this.baseUrl}/certificates`, {
      userId,
      courseId
    });
  }

  downloadCourse(postId: number, userId: number): Observable<Blob> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get(`${this.baseUrl}/download/${postId}`, {
      params,
      responseType: 'blob'
    });
  }

  /**
   * Streams a published COURSE post file and triggers a browser download (same as GET /api/download/{postId}).
   */
  downloadCoursePostToDevice(post: Post, userId: number): Observable<void> {
    return this.downloadCourse(post.id, userId).pipe(
      tap((blob) =>
        triggerBlobDownload(blob, courseDownloadFilename(post.id, blob, post.fileUrl))
      ),
      map(() => undefined)
    );
  }

  getQuiz(lessonContent: string): Observable<unknown[]> {
    return this.http.post<unknown[]>(`${this.baseUrl}/ai/quiz`, { lessonContent });
  }

  // Course Comments
  getComments(courseId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.baseUrl}/course-comments/course/${courseId}`);
  }

  addComment(courseId: number, content: string, userId: number): Observable<Comment> {
    return this.http.post<Comment>(`${this.baseUrl}/course-comments/course/${courseId}`, {
      content,
      userId
    });
  }

  // Course Votes
  vote(courseId: number, type: VoteType, userId: number): Observable<Vote> {
    return this.http.post<Vote>(`${this.baseUrl}/course-votes/course/${courseId}`, {
      userId,
      type
    });
  }

  // Course Reports
  report(
    courseId: number,
    reason: string,
    description: string,
    reportedBy: number
  ): Observable<Report> {
    const params = new HttpParams()
      .set('reportedBy', String(reportedBy))
      .set('courseId', String(courseId))
      .set('reason', reason)
      .set('description', description);
    return this.http.post<Report>(`${this.baseUrl}/course-reports/course/${courseId}`, {
      reportedBy,
      reason,
      description
    });
  }
}
