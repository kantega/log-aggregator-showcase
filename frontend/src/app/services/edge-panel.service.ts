import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, interval, switchMap, catchError, of } from 'rxjs';

export interface ArchiveEntry {
  entryId: number;
  content: string;
  timestamp: string;
}

export interface ArchiveError {
  adapter: string;
  message: string;
  timestamp: string;
}

export interface ArchiveGroup {
  id: string;
  groupId: number;
  name: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'ARCHIVED' | 'FAILED';
  entries: ArchiveEntry[];
  errors: ArchiveError[];
  retryCount: number;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class EdgePanelService {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = signal<ArchiveGroup[] | null>(null);
  readonly error = signal<string | null>(null);

  private subscription: Subscription | null = null;

  startPolling(): void {
    if (this.subscription) {
      return;
    }

    this.subscription = interval(3000)
      .pipe(
        switchMap(() =>
          this.http.get<ArchiveGroup[]>('/edge-api/api/groups').pipe(catchError(() => of(null))),
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

  retryGroup(groupId: number) {
    return this.http.post('/edge-api/api/retry', {});
  }
}
