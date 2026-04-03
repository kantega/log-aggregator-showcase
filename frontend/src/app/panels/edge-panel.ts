import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { EdgePanelService, ArchiveGroup, ArchiveEvent } from '../services/edge-panel.service';

@Component({
  selector: 'app-edge-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <div class="h-full flex flex-col overflow-hidden">
      <!-- Header -->
      <div class="px-4 py-2.5 border-b border-gray-200 bg-amber-50 shrink-0 flex items-center gap-2">
        <span
          [class]="
            'inline-block w-2 h-2 rounded-full ' +
            (edgeService.error() ? 'bg-red-500' : 'bg-green-500')
          "
          [attr.aria-label]="edgeService.error() ? 'Disconnected' : 'Connected'"
          role="status"
        ></span>
        <h2 class="text-sm font-semibold text-amber-800">Edge / MongoDB</h2>
      </div>

      <!-- Content -->
      <div class="flex-1 flex flex-col overflow-hidden">
        @if (edgeService.error()) {
          <div class="p-4">
            <div class="flex items-center gap-2 p-3 bg-amber-50 rounded-lg border border-amber-200">
              <span class="text-amber-600 text-sm">{{ edgeService.error() }}</span>
            </div>
          </div>
        } @else if (edgeService.data(); as groups) {
          @if (groups.length === 0) {
            <p class="text-sm text-gray-400 p-4">No archive groups yet</p>
          } @else {
            <!-- Horizontal scrollable group cards -->
            <div class="shrink-0 overflow-x-auto border-b border-gray-100">
              <div class="flex gap-2 p-3 min-w-max">
                @for (group of groups; track group.groupId) {
                  <button
                    type="button"
                    (click)="selectGroup(group)"
                    [attr.data-testid]="'edge-group-' + group.groupId"
                    [class]="
                      'flex-shrink-0 w-36 text-left p-2.5 rounded-lg border transition-all hover:shadow-sm ' +
                      (selectedGroupId() === group.groupId
                        ? 'ring-2 ring-amber-400 border-amber-300 bg-amber-50'
                        : 'border-gray-200 bg-white hover:border-gray-300')
                    "
                  >
                    <div class="text-sm font-medium text-gray-900 truncate">{{ group.name }}</div>
                    <div class="flex items-center gap-1.5 mt-1">
                      <span
                        data-testid="edge-group-status"
                        [class]="'inline-flex items-center px-1.5 py-0.5 rounded-full text-xs font-medium ' + statusBadgeClass(group.status)"
                      >
                        {{ group.status }}
                      </span>
                      @if (group.retryCount > 0) {
                        <span class="text-xs text-gray-400">r:{{ group.retryCount }}</span>
                      }
                    </div>
                  </button>
                }
              </div>
            </div>

            <!-- Selected group detail: archive events list -->
            @if (selectedGroup(); as group) {
              <div class="flex-1 overflow-y-auto p-3">
                @if (group.archiveEvents && group.archiveEvents.length > 0) {
                  <h3 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                    Archive Events ({{ group.archiveEvents.length }})
                  </h3>
                  <div class="space-y-1.5">
                    @for (event of group.archiveEvents; track $index) {
                      <div
                        [class]="
                          'flex items-center gap-2 p-2 rounded border ' +
                          (event.status === 'FAILED'
                            ? 'bg-red-50 border-red-100'
                            : 'bg-green-50 border-green-100')
                        "
                      >
                        <span
                          [class]="
                            'inline-block w-1.5 h-1.5 rounded-full shrink-0 ' +
                            (event.status === 'FAILED' ? 'bg-red-500' : 'bg-green-500')
                          "
                        ></span>
                        <span
                          [class]="
                            'text-xs font-mono font-medium shrink-0 ' +
                            eventTypeColor(event.eventType)
                          "
                        >{{ event.eventType }}</span>
                        <span class="text-xs text-gray-600 shrink-0">{{ event.adapter }}</span>
                        @if (event.message) {
                          <span class="text-xs text-red-600 truncate flex-1">{{ event.message }}</span>
                        } @else {
                          <span class="flex-1"></span>
                        }
                        <span class="text-xs text-gray-400 shrink-0">{{ event.timestamp | date: 'HH:mm:ss' }}</span>
                        @if (event.status === 'FAILED') {
                          <button
                            type="button"
                            data-testid="edge-retry-button"
                            (click)="retry(group, $event)"
                            class="shrink-0 px-2 py-0.5 bg-red-100 text-red-700 text-xs font-medium rounded hover:bg-red-200 transition-colors"
                          >
                            Retry
                          </button>
                        }
                      </div>
                    }
                  </div>
                } @else if (group.errors && group.errors.length > 0) {
                  <!-- Fallback for old data without archiveEvents -->
                  <h3 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                    Errors ({{ group.errors.length }})
                  </h3>
                  <div class="space-y-1.5">
                    @for (err of group.errors; track $index) {
                      <div class="flex items-center gap-2 p-2 bg-red-50 rounded border border-red-100">
                        <span class="inline-block w-1.5 h-1.5 rounded-full bg-red-500 shrink-0"></span>
                        <span class="text-xs font-mono font-medium text-red-700">{{ err.eventType }}</span>
                        <span class="text-xs text-red-600 truncate flex-1">{{ err.adapter }}: {{ err.message }}</span>
                        <span class="text-xs text-red-400 shrink-0">{{ err.timestamp | date: 'HH:mm:ss' }}</span>
                        <button
                          type="button"
                          (click)="retry(group, $event)"
                          class="shrink-0 px-2 py-0.5 bg-red-100 text-red-700 text-xs font-medium rounded hover:bg-red-200 transition-colors"
                        >
                          Retry
                        </button>
                      </div>
                    }
                  </div>
                } @else {
                  <p class="text-sm text-gray-400">No archive events recorded for this group</p>
                }
              </div>
            } @else {
              <div class="flex-1 flex items-center justify-center">
                <p class="text-xs text-gray-400">Select a group to view details</p>
              </div>
            }
          }
        } @else {
          <p class="text-sm text-gray-400 p-4">Connecting...</p>
        }
      </div>
    </div>
  `,
})
export class EdgePanelComponent {
  protected readonly edgeService = inject(EdgePanelService);
  readonly selectedGroupId = signal<number | null>(null);

  readonly selectedGroup = computed(() => {
    const id = this.selectedGroupId();
    const groups = this.edgeService.data();
    if (!id || !groups) return null;
    return groups.find((g) => g.groupId === id) ?? null;
  });

  selectGroup(group: ArchiveGroup): void {
    this.selectedGroupId.set(group.groupId);
  }

  retry(group: ArchiveGroup, event: Event): void {
    event.stopPropagation();
    this.edgeService.retryGroup(group.groupId).subscribe();
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'ARCHIVED':
        return 'bg-green-100 text-green-800';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'IN_PROGRESS':
        return 'bg-orange-100 text-orange-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  eventTypeColor(eventType: string): string {
    switch (eventType) {
      case 'GROUP_CREATED':
        return 'text-blue-700';
      case 'ENTRY_ADDED':
        return 'text-gray-700';
      case 'GROUP_CLOSED':
        return 'text-purple-700';
      default:
        return 'text-gray-700';
    }
  }
}
