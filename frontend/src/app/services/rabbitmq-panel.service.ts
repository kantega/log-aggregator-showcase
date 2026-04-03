import { Injectable, signal, NgZone, inject } from '@angular/core';
import { Client } from '@stomp/stompjs';

export interface RabbitMqMessage {
  eventType: string;
  groupId: number;
  groupName: string;
  entryContent: string | null;
  entryId: number | null;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class RabbitmqPanelService {
  private readonly zone = inject(NgZone);

  readonly messages = signal<RabbitMqMessage[]>([]);
  readonly messageCount = signal(0);
  readonly connected = signal(false);
  readonly error = signal<string | null>(null);

  private client: Client | null = null;

  connect(): void {
    if (this.client?.active) {
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = `${protocol}//${window.location.host}/ws`;

    this.client = new Client({
      brokerURL,
      connectHeaders: {
        login: 'myuser',
        passcode: 'secret',
      },
      reconnectDelay: 3000,
      onConnect: () => {
        this.zone.run(() => {
          this.connected.set(true);
          this.error.set(null);
        });

        this.client!.subscribe(
          '/exchange/log-manager-exchange/log.event',
          (frame) => {
            const body = JSON.parse(frame.body) as RabbitMqMessage;
            this.zone.run(() => {
              this.messages.update((msgs) => [body, ...msgs]);
              this.messageCount.update((c) => c + 1);
            });
          },
        );
      },
      onStompError: (frame) => {
        this.zone.run(() => {
          this.connected.set(false);
          this.error.set(frame.headers['message'] ?? 'STOMP error');
        });
      },
      onWebSocketClose: () => {
        this.zone.run(() => {
          this.connected.set(false);
        });
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }

  clearMessages(): void {
    this.messages.set([]);
    this.messageCount.set(0);
  }
}
