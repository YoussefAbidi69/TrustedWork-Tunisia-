import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export type CourseBlockType = 'TEXT' | 'CODE' | 'VIDEO' | 'PDF' | 'IMAGE' | 'QUIZ';

export interface CourseCreatePayload {
  title: string;
  description: string;
  communityId: number;
  published: boolean;
  authorId?: number;
}

export interface SectionCreatePayload {
  title: string;
  orderIndex: number;
}

export interface BlockCreatePayload {
  title: string;
  type: CourseBlockType;
  content: string;
  fileUrl?: string;
  orderIndex: number;
}

export interface CourseCreateResponse {
  id: number;
}

export interface SectionCreateResponse {
  id: number;
}

@Injectable({ providedIn: 'root' })
export class CourseBuilderService {
  private readonly baseUrl = `${environment.msCommunity}/api`;

  constructor(private readonly http: HttpClient) {}

  createCourse(payload: CourseCreatePayload): Observable<CourseCreateResponse> {
    return this.http.post<CourseCreateResponse>(`${this.baseUrl}/courses`, payload);
  }

  updateCourse(courseId: number, payload: Partial<CourseCreatePayload>): Observable<any> {
    return this.http.put(`${this.baseUrl}/courses/${courseId}`, payload);
  }

  createSection(courseId: number, payload: SectionCreatePayload): Observable<SectionCreateResponse> {
    return this.http
      .post<SectionCreateResponse>(`${this.baseUrl}/sections/course/${courseId}`, payload)
      .pipe(
        catchError(() =>
          this.http.post<SectionCreateResponse>(`${this.baseUrl}/courses/${courseId}/sections`, payload)
        )
      );
  }

  createBlock(sectionId: number, payload: BlockCreatePayload): Observable<{ id: number }> {
    return this.http
      .post<{ id: number }>(`${this.baseUrl}/blocks/section/${sectionId}`, payload)
      .pipe(
        catchError(() =>
          this.http.post<{ id: number }>(`${this.baseUrl}/sections/${sectionId}/lessons`, {
            title: payload.title,
            content: payload.content,
            type: payload.type,
            pdfUrl: payload.fileUrl || '',
            orderIndex: payload.orderIndex
          })
        )
      );
  }
}
