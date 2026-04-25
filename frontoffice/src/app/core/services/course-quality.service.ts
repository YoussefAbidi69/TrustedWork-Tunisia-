import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface QualityFeedback {
  type: 'positive' | 'neutral' | 'negative';
  message: string;
}

export interface QualityPrediction {
  score: number;
  label: string;
  label_display: string;
  feedback: QualityFeedback[];
  available?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class CourseQualityService {
  private readonly baseUrl = `${environment.msCommunity}/api/quality`;

  constructor(private readonly http: HttpClient) {}

  predictCourseQuality(title: string, description: string): Observable<QualityPrediction> {
    return this.http.post<QualityPrediction>(`${this.baseUrl}/predict`, { title, description });
  }
}

