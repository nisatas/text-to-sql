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
  debug: any;
}

@Injectable({
  providedIn: 'root',
})
export class Query {
  private apiUrl = 'http://localhost:8080/api/query';

  constructor(private http: HttpClient) {}

  sendQuery(question: string): Observable<QueryResponse> {
    return this.http.post<QueryResponse>(this.apiUrl, { question });
  }
}
