import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface CourseOutlineResponse {
  topic?: string;
  level?: string;
  sections?: { title?: string; lessons?: string[] }[];
}

export interface QuizQuestionDto {
  question?: string;
  text?: string;
  label?: string;
  options?: string[];
  correctIndex?: number;
}

@Injectable({ providedIn: 'root' })
export class CommunityAiService {
  private readonly baseUrl = `${environment.msCommunity}/api/ai`;

  constructor(private readonly http: HttpClient) {}

  generateCourseOutline(topic: string, level: string): Observable<CourseOutlineResponse> {
    return this.http.post<CourseOutlineResponse>(`${this.baseUrl}/course-outline`, { topic, level });
  }

  generateQuiz(lessonContent: string): Observable<QuizQuestionDto[]> {
    return this.http.post<QuizQuestionDto[]>(`${this.baseUrl}/quiz`, { lessonContent });
  }

  summarizeLesson(lessonContent: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/summarize`, { lessonContent }, { responseType: 'text' });
  }

  tutorAnswer(courseContent: string, question: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/tutor-answer`, { courseContent, question }, { responseType: 'text' });
  }
}
