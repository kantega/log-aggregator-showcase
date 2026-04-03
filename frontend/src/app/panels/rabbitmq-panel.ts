import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RabbitmqPanelService } from '../services/rabbitmq-panel.service';

@Component({
  selector: 'app-rabbitmq-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="h-full flex flex-col overflow-hidden">
      <!-- Header -->
      <div class="px-4 py-2.5 border-b border-gray-200 bg-purple-50 shrink-0 flex items-center gap-2">
        <span
          data-testid="rabbitmq-status"
          [class]="
            'inline-block w-2 h-2 rounded-full ' +
            (rabbitmqService.error() ? 'bg-red-500' : 'bg-green-500')
          "
          [attr.aria-label]="rabbitmqService.error() ? 'Disconnected' : 'Connected'"
          role="status"
        ></span>
        <h2 class="text-sm font-semibold text-purple-800">RabbitMQ Live Feed</h2>
        @if (rabbitmqService.data(); as data) {
          <span class="ml-auto text-xs font-medium text-purple-600 bg-purple-100 px-2 py-0.5 rounded-full">
            {{ data.messages }} msgs
          </span>
        }
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-4">
        @if (rabbitmqService.error()) {
          <div class="flex items-center gap-2 p-3 bg-amber-50 rounded-lg border border-amber-200">
            <span class="text-amber-600 text-sm">{{ rabbitmqService.error() }}</span>
          </div>
        } @else if (rabbitmqService.data(); as data) {
          <div class="grid grid-cols-2 gap-2">
            <div class="p-2.5 bg-white rounded-lg border border-gray-100 shadow-sm">
              <p class="text-xs text-gray-500 uppercase tracking-wider">Messages</p>
              <p data-testid="rabbitmq-messages" class="text-xl font-semibold text-gray-900 mt-0.5">{{ data.messages }}</p>
            </div>
            <div class="p-2.5 bg-white rounded-lg border border-gray-100 shadow-sm">
              <p class="text-xs text-gray-500 uppercase tracking-wider">Ready</p>
              <p class="text-xl font-semibold text-gray-900 mt-0.5">{{ data.messagesReady }}</p>
            </div>
            <div class="p-2.5 bg-white rounded-lg border border-gray-100 shadow-sm">
              <p class="text-xs text-gray-500 uppercase tracking-wider">Unacked</p>
              <p class="text-xl font-semibold text-gray-900 mt-0.5">{{ data.messagesUnacknowledged }}</p>
            </div>
            <div class="p-2.5 bg-white rounded-lg border border-gray-100 shadow-sm">
              <p class="text-xs text-gray-500 uppercase tracking-wider">Consumers</p>
              <p class="text-xl font-semibold text-gray-900 mt-0.5">{{ data.consumers }}</p>
            </div>
          </div>
          <p class="text-xs text-gray-400 mt-3 text-center">
            Live STOMP feed coming in Step 5
          </p>
        } @else {
          <p class="text-sm text-gray-400">Connecting...</p>
        }
      </div>
    </div>
  `,
})
export class RabbitmqPanelComponent {
  protected readonly rabbitmqService = inject(RabbitmqPanelService);
}
