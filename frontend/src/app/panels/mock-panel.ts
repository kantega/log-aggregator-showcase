import { Component, ChangeDetectionStrategy, inject, signal, computed, effect } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MockPanelService, MockHistoryEntry } from '../services/mock-panel.service';

@Component({
  selector: 'app-mock-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, JsonPipe, FormsModule],
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
          <!-- Setup Controls -->
          <div class="mb-4 p-3 bg-white rounded-lg border border-gray-200" data-testid="mock-setup-controls">
            <h3 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Mock Setup</h3>
            <div class="space-y-2">
              <!-- Noark A -->
              <div class="flex items-center gap-2" data-testid="mock-setup-noarka">
                <span
                  [class]="'inline-block w-2 h-2 rounded-full ' + statusDotClass('noarka')"
                  role="status"
                  [attr.aria-label]="'Noark A status: ' + noarkAStatus()"
                ></span>
                <span class="text-sm text-gray-700 w-14 shrink-0">Noark A</span>
                <select
                  class="text-xs font-mono border border-gray-200 rounded px-1.5 py-1 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-400"
                  [ngModel]="noarkAStatus()"
                  (ngModelChange)="noarkAStatus.set($event)"
                  data-testid="mock-setup-noarka-status"
                  aria-label="Noark A status code"
                >
                  @for (code of statusCodes; track code) {
                    <option [ngValue]="code">{{ code }}</option>
                  }
                </select>
                <input
                  type="number"
                  class="text-xs font-mono border border-gray-200 rounded px-1.5 py-1 w-16 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-400"
                  [ngModel]="noarkADelay()"
                  (ngModelChange)="noarkADelay.set(+$event)"
                  data-testid="mock-setup-noarka-delay"
                  aria-label="Noark A delay in seconds"
                  min="0"
                  step="0.5"
                  placeholder="s"
                />
                <span class="text-xs text-gray-400">s</span>
                <button
                  class="text-xs font-medium px-2 py-1 rounded bg-emerald-100 text-emerald-700 hover:bg-emerald-200 transition-colors"
                  (click)="applySetup('noarka', noarkAStatus(), noarkADelay())"
                  data-testid="mock-setup-noarka-apply"
                >Apply</button>
                <button
                  class="text-xs font-medium px-2 py-1 rounded bg-amber-100 text-amber-700 hover:bg-amber-200 transition-colors"
                  (click)="failNextOnly('noarka')"
                  data-testid="mock-setup-noarka-fail-next"
                  title="Queue a single 500 response on the next request only"
                >Fail next only</button>
                <span [class]="'text-xs font-mono px-2 py-0.5 rounded ' + statusBadgeClass('noarka')">
                  {{ currentStatusLabel('noarka') }}
                </span>
                @if (currentDelay('noarka') > 0) {
                  <span class="text-xs text-gray-400">+{{ currentDelay('noarka') }}s</span>
                }
                @if (queuedFailures('noarka') > 0) {
                  <span
                    class="text-xs font-mono px-2 py-0.5 rounded bg-amber-50 text-amber-700"
                    data-testid="mock-setup-noarka-queued-failures"
                  >queued: {{ queuedFailures('noarka') }}</span>
                }
              </div>

              <!-- Noark B -->
              <div class="flex items-center gap-2" data-testid="mock-setup-noarkb">
                <span
                  [class]="'inline-block w-2 h-2 rounded-full ' + statusDotClass('noarkb')"
                  role="status"
                  [attr.aria-label]="'Noark B status: ' + noarkBStatus()"
                ></span>
                <span class="text-sm text-gray-700 w-14 shrink-0">Noark B</span>
                <select
                  class="text-xs font-mono border border-gray-200 rounded px-1.5 py-1 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-400"
                  [ngModel]="noarkBStatus()"
                  (ngModelChange)="noarkBStatus.set($event)"
                  data-testid="mock-setup-noarkb-status"
                  aria-label="Noark B status code"
                >
                  @for (code of statusCodes; track code) {
                    <option [ngValue]="code">{{ code }}</option>
                  }
                </select>
                <input
                  type="number"
                  class="text-xs font-mono border border-gray-200 rounded px-1.5 py-1 w-16 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-400"
                  [ngModel]="noarkBDelay()"
                  (ngModelChange)="noarkBDelay.set(+$event)"
                  data-testid="mock-setup-noarkb-delay"
                  aria-label="Noark B delay in seconds"
                  min="0"
                  step="0.5"
                  placeholder="s"
                />
                <span class="text-xs text-gray-400">s</span>
                <button
                  class="text-xs font-medium px-2 py-1 rounded bg-emerald-100 text-emerald-700 hover:bg-emerald-200 transition-colors"
                  (click)="applySetup('noarkb', noarkBStatus(), noarkBDelay())"
                  data-testid="mock-setup-noarkb-apply"
                >Apply</button>
                <button
                  class="text-xs font-medium px-2 py-1 rounded bg-amber-100 text-amber-700 hover:bg-amber-200 transition-colors"
                  (click)="failNextOnly('noarkb')"
                  data-testid="mock-setup-noarkb-fail-next"
                  title="Queue a single 500 response on the next request only"
                >Fail next only</button>
                <span [class]="'text-xs font-mono px-2 py-0.5 rounded ' + statusBadgeClass('noarkb')">
                  {{ currentStatusLabel('noarkb') }}
                </span>
                @if (currentDelay('noarkb') > 0) {
                  <span class="text-xs text-gray-400">+{{ currentDelay('noarkb') }}s</span>
                }
                @if (queuedFailures('noarkb') > 0) {
                  <span
                    class="text-xs font-mono px-2 py-0.5 rounded bg-amber-50 text-amber-700"
                    data-testid="mock-setup-noarkb-queued-failures"
                  >queued: {{ queuedFailures('noarkb') }}</span>
                }
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
  readonly statusCodes = [200, 400, 500, 503];

  // Form state signals
  readonly noarkAStatus = signal(200);
  readonly noarkADelay = signal(0);
  readonly noarkBStatus = signal(200);
  readonly noarkBDelay = signal(0);

  private configSynced = false;

  constructor() {
    // Sync form state from server config when it first arrives
    effect(() => {
      const config = this.mockService.config();
      if (config && !this.configSynced) {
        this.configSynced = true;
        if (config['noarka']) {
          this.noarkAStatus.set(config['noarka'].statusCode);
          this.noarkADelay.set(config['noarka'].delayMs / 1000);
        }
        if (config['noarkb']) {
          this.noarkBStatus.set(config['noarkb'].statusCode);
          this.noarkBDelay.set(config['noarkb'].delayMs / 1000);
        }
      }
    });
  }

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

  applySetup(endpoint: string, statusCode: number, delaySec: number): void {
    this.mockService.setup(endpoint, Number(statusCode), Math.round(Number(delaySec) * 1000));
  }

  failNextOnly(endpoint: string): void {
    this.mockService.failNextOnly(endpoint, 500);
  }

  queuedFailures(endpoint: string): number {
    const config = this.mockService.config();
    return config?.[endpoint]?.failResponses?.length ?? 0;
  }

  statusDotClass(endpoint: string): string {
    const config = this.mockService.config();
    const code = config?.[endpoint]?.statusCode ?? 200;
    return code >= 200 && code < 300 ? 'bg-green-500' : 'bg-red-500';
  }

  statusBadgeClass(endpoint: string): string {
    const config = this.mockService.config();
    const code = config?.[endpoint]?.statusCode ?? 200;
    return code >= 200 && code < 300
      ? 'text-green-700 bg-green-50'
      : 'text-red-700 bg-red-50';
  }

  currentDelay(endpoint: string): number {
    const config = this.mockService.config();
    const delayMs = config?.[endpoint]?.delayMs ?? 0;
    return delayMs / 1000;
  }

  currentStatusLabel(endpoint: string): string {
    const config = this.mockService.config();
    const code = config?.[endpoint]?.statusCode ?? 200;
    if (code === 200) return '200 OK';
    if (code === 400) return '400 Bad Request';
    if (code === 500) return '500 Error';
    if (code === 503) return '503 Unavailable';
    return `${code}`;
  }
}
