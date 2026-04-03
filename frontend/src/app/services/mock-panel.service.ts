import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, interval, switchMap, catchError, of, forkJoin } from 'rxjs';

export interface MockHistoryEntry {
  [key: string]: unknown;
}

export interface MockConfig {
  statusCode: number;
  body: string;
  delayMs: number;
}

@Injectable({ providedIn: 'root' })
export class MockPanelService {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = signal<MockHistoryEntry[] | null>(null);
  readonly config = signal<Record<string, MockConfig> | null>(null);
  readonly error = signal<string | null>(null);

  private subscription: Subscription | null = null;

  startPolling(): void {
    if (this.subscription) {
      return;
    }

    // Initial fetch
    this.fetchAll();

    this.subscription = interval(3000)
      .pipe(
        switchMap(() =>
          forkJoin({
            history: this.http.get<MockHistoryEntry[]>('/mock-api/api/test/history').pipe(catchError(() => of(null))),
            config: this.http.get<Record<string, MockConfig>>('/mock-api/api/test/config').pipe(catchError(() => of(null))),
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => {
        if (response.history) {
          this.data.set(response.history);
          this.error.set(null);
        } else {
          this.data.set(null);
          this.error.set('Service unavailable');
        }
        if (response.config) {
          this.config.set(response.config);
        }
      });
  }

  stopPolling(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
  }

  setup(endpoint: string, statusCode: number, delayMs: number): void {
    this.http
      .post('/mock-api/api/test/setup', { endpoint, statusCode, delayMs })
      .subscribe(() => this.fetchConfig());
  }

  reset(): void {
    this.http.post('/mock-api/api/test/reset', {}).subscribe(() => {
      this.fetchAll();
    });
  }

  private fetchAll(): void {
    forkJoin({
      history: this.http.get<MockHistoryEntry[]>('/mock-api/api/test/history').pipe(catchError(() => of(null))),
      config: this.http.get<Record<string, MockConfig>>('/mock-api/api/test/config').pipe(catchError(() => of(null))),
    }).subscribe((response) => {
      if (response.history) {
        this.data.set(response.history);
        this.error.set(null);
      }
      if (response.config) {
        this.config.set(response.config);
      }
    });
  }

  private fetchConfig(): void {
    this.http
      .get<Record<string, MockConfig>>('/mock-api/api/test/config')
      .pipe(catchError(() => of(null)))
      .subscribe((config) => {
        if (config) {
          this.config.set(config);
        }
      });
  }
}
