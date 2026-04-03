import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { LogGroup, LogEntry } from '../models/log-group.model';

@Injectable({ providedIn: 'root' })
export class LogManagerApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/groups';

  getGroups(): Observable<LogGroup[]> {
    return this.http.get<LogGroup[]>(this.baseUrl);
  }

  getGroup(id: number): Observable<LogGroup> {
    return this.http.get<LogGroup>(`${this.baseUrl}/${id}`);
  }

  createGroup(name: string): Observable<LogGroup> {
    return this.http.post<LogGroup>(this.baseUrl, { name });
  }

  getEntries(groupId: number): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.baseUrl}/${groupId}/entries`);
  }

  addEntry(groupId: number, content: string): Observable<LogEntry> {
    return this.http.post<LogEntry>(`${this.baseUrl}/${groupId}/entries`, { content });
  }

  closeGroup(id: number): Observable<LogGroup> {
    return this.http.post<LogGroup>(`${this.baseUrl}/${id}/close`, {});
  }

  deleteAll(): Observable<unknown> {
    return this.http.delete(this.baseUrl);
  }
}
