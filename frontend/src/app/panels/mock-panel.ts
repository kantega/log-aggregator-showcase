import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { MockPanelService, MockHistoryEntry } from '../services/mock-panel.service';

@Component({
  selector: 'app-mock-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, JsonPipe],
  template: `
    <div class="h-full flex flex-col overflow-hidden">
      <!-- Header -->
      <div class="px-4 py-2.5 border-b border-gray-200 bg-emerald-50 shrink-0 flex items-center gap-2">
        <span
          [class]="
            'inline-block w-2 h-2 rounded-full ' +
            (mockService.error() ? 'bg-red-500' : 'bg-green-500')
          "
          [attr.aria-label]="mockService.error() ? 'Disconnected' : 'Connected'"
          role="status"
        ></span>
        <h2 class="text-sm font-semibold text-emerald-800">External APIs Mock</h2>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-4">
        @if (mockService.error()) {
          <div class="flex items-center gap-2 p-3 bg-amber-50 rounded-lg border border-amber-200">
            <span class="text-amber-600 text-sm">{{ mockService.error() }}</span>
          </div>
        } @else if (mockService.data(); as data) {
          <!-- Setup Controls placeholder -->
          <div class="mb-4 p-3 bg-white rounded-lg border border-gray-200">
            <h3 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Mock Setup</h3>
            <div class="space-y-2">
              <div class="flex items-center gap-3">
                <span class="inline-block w-2 h-2 rounded-full bg-green-500"></span>
                <span class="text-sm text-gray-700 w-16">Noark A</span>
                <span class="text-xs font-mono text-green-700 bg-green-50 px-2 py-0.5 rounded">200 OK</span>
                <span class="text-xs text-gray-400 ml-auto">Controls coming in Step 4</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="inline-block w-2 h-2 rounded-full bg-green-500"></span>
                <span class="text-sm text-gray-700 w-16">Noark B</span>
                <span class="text-xs font-mono text-green-700 bg-green-50 px-2 py-0.5 rounded">200 OK</span>
              </div>
            </div>
          </div>

          <!-- Request History -->
          @if (data.length === 0) {
            <p class="text-sm text-gray-400">No requests recorded yet</p>
          } @else {
            <!-- Noark A requests -->
            <div class="mb-4">
              <h3 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                Noark A
                <span class="text-gray-400 font-normal">({{ noarkARequests().length }})</span>
              </h3>
              @if (noarkARequests().length === 0) {
                <p class="text-xs text-gray-400">No requests</p>
              } @else {
                <div class="space-y-1.5">
                  @for (entry of noarkARequests(); track $index) {
                    <div
                      class="p-2 bg-white rounded border border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors"
                      (click)="toggleRequestExpand('a-' + $index)"
                      (keydown.enter)="toggleRequestExpand('a-' + $index)"
                      tabindex="0"
                      role="button"
                      [attr.aria-expanded]="expandedRequest() === 'a-' + $index"
                    >
                      <div class="flex items-center gap-2">
                        <span class="text-xs font-mono font-medium text-gray-600">{{ entry['method'] }}</span>
                        <span class="text-xs font-mono text-gray-500 truncate flex-1">{{ entry['path'] }}</span>
                        <span class="text-xs text-gray-400">{{ $any(entry['timestamp']) | date: 'HH:mm:ss' }}</span>
                      </div>
                      @if (expandedRequest() === 'a-' + $index) {
                        <pre class="mt-2 text-xs font-mono text-gray-600 whitespace-pre-wrap overflow-hidden bg-gray-50 p-2 rounded">{{ entry['body'] | json }}</pre>
                      }
                    </div>
                  }
                </div>
              }
            </div>

            <!-- Noark B requests -->
            <div>
              <h3 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                Noark B
                <span class="text-gray-400 font-normal">({{ noarkBRequests().length }})</span>
              </h3>
              @if (noarkBRequests().length === 0) {
                <p class="text-xs text-gray-400">No requests</p>
              } @else {
                <div class="space-y-1.5">
                  @for (entry of noarkBRequests(); track $index) {
                    <div
                      class="p-2 bg-white rounded border border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors"
                      (click)="toggleRequestExpand('b-' + $index)"
                      (keydown.enter)="toggleRequestExpand('b-' + $index)"
                      tabindex="0"
                      role="button"
                      [attr.aria-expanded]="expandedRequest() === 'b-' + $index"
                    >
                      <div class="flex items-center gap-2">
                        <span class="text-xs font-mono font-medium text-gray-600">{{ entry['method'] }}</span>
                        <span class="text-xs font-mono text-gray-500 truncate flex-1">{{ entry['path'] }}</span>
                        <span class="text-xs text-gray-400">{{ $any(entry['timestamp']) | date: 'HH:mm:ss' }}</span>
                      </div>
                      @if (expandedRequest() === 'b-' + $index) {
                        <pre class="mt-2 text-xs font-mono text-gray-600 whitespace-pre-wrap overflow-hidden bg-gray-50 p-2 rounded">{{ entry['body'] | json }}</pre>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          }
        } @else {
          <p class="text-sm text-gray-400">Connecting...</p>
        }
      </div>
    </div>
  `,
})
export class MockPanelComponent {
  protected readonly mockService = inject(MockPanelService);
  readonly expandedRequest = signal<string | null>(null);

  readonly noarkARequests = computed(() => {
    const data = this.mockService.data();
    if (!data) return [];
    return data.filter((entry) => {
      const endpoint = entry['endpoint'] as string;
      return endpoint === 'noarka' || (entry['path'] as string)?.includes('noarka');
    });
  });

  readonly noarkBRequests = computed(() => {
    const data = this.mockService.data();
    if (!data) return [];
    return data.filter((entry) => {
      const endpoint = entry['endpoint'] as string;
      return endpoint === 'noarkb' || (entry['path'] as string)?.includes('noarkb');
    });
  });

  toggleRequestExpand(key: string): void {
    this.expandedRequest.update((current) => (current === key ? null : key));
  }
}
