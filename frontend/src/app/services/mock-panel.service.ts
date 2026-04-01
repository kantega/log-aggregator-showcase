import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, interval, switchMap, catchError, of } from 'rxjs';

export interface MockHistoryEntry {
  [key: string]: unknown;
}

@Injectable({ providedIn: 'root' })
export class MockPanelService {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = signal<MockHistoryEntry[] | null>(null);
  readonly error = signal<string | null>(null);

  private subscription: Subscription | null = null;

  startPolling(): void {
    if (this.subscription) {
      return;
    }

    this.subscription = interval(3000)
      .pipe(
        switchMap(() =>
          this.http.get<MockHistoryEntry[]>('/mock-api/api/test/history').pipe(catchError(() => of(null))),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => {
        if (response) {
          this.data.set(response);
          this.error.set(null);
        } else {
          this.data.set(null);
          this.error.set('Service unavailable');
        }
      });
  }

  stopPolling(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
  }
}
