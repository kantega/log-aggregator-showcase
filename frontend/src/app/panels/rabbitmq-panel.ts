import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RabbitmqPanelService, RabbitMqMessage } from '../services/rabbitmq-panel.service';

@Component({
  selector: 'app-rabbitmq-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <div class="h-full flex flex-col overflow-hidden">
      <!-- Header -->
      <div class="px-4 py-2.5 border-b border-gray-200 bg-purple-50 shrink-0 flex items-center gap-2">
        <span
          data-testid="rabbitmq-status"
          [class]="
            'inline-block w-2 h-2 rounded-full ' +
            (svc.connected() ? 'bg-green-500' : 'bg-red-500')
          "
          [attr.aria-label]="svc.connected() ? 'Connected' : 'Disconnected'"
          role="status"
        ></span>
        <h2 class="text-sm font-semibold text-purple-800">RabbitMQ Live Feed</h2>
        <span
          data-testid="rabbitmq-message-count"
          class="ml-auto text-xs font-medium text-purple-600 bg-purple-100 px-2 py-0.5 rounded-full"
        >
          {{ svc.messageCount() }} msgs
        </span>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-2" data-testid="rabbitmq-feed">
        @if (svc.error()) {
          <div class="flex items-center gap-2 p-3 bg-amber-50 rounded-lg border border-amber-200">
            <span class="text-amber-600 text-sm">{{ svc.error() }}</span>
          </div>
        } @else if (svc.messages().length === 0) {
          <p class="text-xs text-gray-400 text-center py-2">No messages yet</p>
        } @else {
          <div class="space-y-0.5">
            @for (msg of svc.messages(); track $index) {
              <div class="flex items-center gap-1.5 px-2 py-1 rounded hover:bg-gray-50 text-xs font-mono">
                <span [class]="eventTypeClass(msg.eventType)">{{ msg.eventType }}</span>
                <span class="text-gray-500">Group #{{ msg.groupId }}</span>
                @if (msg.eventType === 'ENTRY_ADDED' && msg.entryContent) {
                  <span class="text-gray-400 truncate flex-1">"{{ msg.entryContent }}"</span>
                } @else {
                  <span class="flex-1"></span>
                }
                <span class="text-gray-400 shrink-0">{{ msg.timestamp | date: 'HH:mm:ss' }}</span>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class RabbitmqPanelComponent {
  protected readonly svc = inject(RabbitmqPanelService);

  eventTypeClass(eventType: string): string {
    switch (eventType) {
      case 'GROUP_CREATED':
        return 'text-blue-600 font-medium';
      case 'ENTRY_ADDED':
        return 'text-gray-600';
      case 'GROUP_CLOSED':
        return 'text-purple-600 font-medium';
      default:
        return 'text-gray-600';
    }
  }
}
