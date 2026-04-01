import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, interval, switchMap, catchError, of } from 'rxjs';

export interface RabbitMqQueueInfo {
  messages: number;
  messagesReady: number;
  messagesUnacknowledged: number;
  consumers: number;
}

@Injectable({ providedIn: 'root' })
export class RabbitmqPanelService {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = signal<RabbitMqQueueInfo | null>(null);
  readonly error = signal<string | null>(null);

  private subscription: Subscription | null = null;

  startPolling(): void {
    if (this.subscription) {
      return;
    }

    const headers = new HttpHeaders({
      Authorization: `Basic ${btoa('myuser:secret')}`,
    });

    this.subscription = interval(3000)
      .pipe(
        switchMap(() =>
          this.http.get<Record<string, unknown>>('/rabbitmq-api/api/queues/%2F/log-events-queue', { headers }).pipe(
            catchError(() => of(null)),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => {
        if (response) {
          this.data.set({
            messages: (response['messages'] as number) ?? 0,
            messagesReady: (response['messages_ready'] as number) ?? 0,
            messagesUnacknowledged: (response['messages_unacknowledged'] as number) ?? 0,
            consumers: (response['consumers'] as number) ?? 0,
          });
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
