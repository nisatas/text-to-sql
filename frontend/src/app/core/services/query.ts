import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface QueryResponse {
  question: string | null;
  sql: string | null;
  status: 'success' | 'error';
  columns: string[] | null;
  rows: any[][] | null;
  error: string | null;
  summary?: string | null;
  debug: any;
}

export interface FilterQueryRequest {
  outputMode?: 'records' | 'avg_overall' | 'avg_by_subject' | null;
  className?: string | null; // e.g. 9-A
  gradeLevel?: number | null;
  studentNumber?: string | null;
  studentName?: string | null;
  subject?: string | null;
  examNo?: 1 | 2 | null;
  minScore?: number | null;
  maxScore?: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class Query {
  private readonly apiUrl = '/api/query';
  private readonly filterApiUrl = '/api/filter-query';

  constructor(private http: HttpClient) {}

  sendQuery(question: string, includeSummary = false): Observable<QueryResponse> {
    return this.http.post<QueryResponse>(this.apiUrl, { question, includeSummary });
  }

  sendFilterQuery(filters: FilterQueryRequest): Observable<QueryResponse> {
    return this.http.post<QueryResponse>(this.filterApiUrl, filters ?? {});
  }

  getSubjects(): Observable<string[]> {
    return this.http.get<string[]>('/api/meta/subjects');
  }
}
