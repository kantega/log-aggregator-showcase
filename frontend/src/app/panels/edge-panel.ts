import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { EdgePanelService, ArchiveGroup } from '../services/edge-panel.service';

@Component({
  selector: 'app-edge-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
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
      <div class="flex-1 overflow-y-auto p-4">
        @if (edgeService.error()) {
          <div class="flex items-center gap-2 p-3 bg-amber-50 rounded-lg border border-amber-200">
            <span class="text-amber-600 text-sm">{{ edgeService.error() }}</span>
          </div>
        } @else if (edgeService.data(); as groups) {
          @if (groups.length === 0) {
            <p class="text-sm text-gray-400">No archive groups yet</p>
          } @else {
            <div class="flex flex-wrap gap-3">
              @for (group of groups; track group.groupId) {
                <div
                  class="w-48 border rounded-lg overflow-hidden cursor-pointer transition-all hover:shadow-md"
                  [class.border-green-200]="group.status === 'ARCHIVED'"
                  [class.border-yellow-200]="group.status === 'PENDING'"
                  [class.border-orange-200]="group.status === 'IN_PROGRESS'"
                  [class.border-red-200]="group.status === 'FAILED'"
                  (click)="toggleExpand(group.groupId)"
                  (keydown.enter)="toggleExpand(group.groupId)"
                  (keydown.space)="toggleExpand(group.groupId)"
                  [attr.data-testid]="'edge-group-' + group.groupId"
                  tabindex="0"
                  [attr.aria-expanded]="expandedGroupId() === group.groupId"
                  role="button"
                >
                  <div class="p-3">
                    <div class="flex items-center justify-between mb-1">
                      <span class="text-sm font-medium text-gray-900 truncate">{{ group.name }}</span>
                    </div>
                    <div class="flex items-center gap-2">
                      <span
                        data-testid="edge-group-status"
                        [class]="'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ' + statusBadgeClass(group.status)"
                      >
                        {{ group.status }}
                      </span>
                      @if (group.retryCount > 0) {
                        <span class="text-xs text-gray-400">retry: {{ group.retryCount }}</span>
                      }
                    </div>
                    @if (group.status === 'FAILED') {
                      <button
                        type="button"
                        data-testid="edge-retry-button"
                        (click)="retry(group, $event)"
                        class="mt-2 w-full px-2 py-1 bg-red-50 text-red-700 text-xs font-medium rounded hover:bg-red-100 transition-colors"
                      >
                        Retry
                      </button>
                    }
                  </div>

                  <!-- Expanded detail -->
                  @if (expandedGroupId() === group.groupId) {
                    <div class="border-t border-gray-100 p-3 bg-gray-50">
                      @if (group.entries.length > 0) {
                        <p class="text-xs font-semibold text-gray-500 mb-1">Entries ({{ group.entries.length }})</p>
                        <div class="space-y-1 mb-2">
                          @for (entry of group.entries; track entry.entryId) {
                            <div class="text-xs font-mono text-gray-600 truncate">{{ entry.content }}</div>
                          }
                        </div>
                      }
                      @if (group.errors.length > 0) {
                        <p class="text-xs font-semibold text-red-500 mb-1">Errors ({{ group.errors.length }})</p>
                        <div class="space-y-1">
                          @for (err of group.errors; track $index) {
                            <div class="text-xs text-red-600">
                              <span class="font-medium">{{ err.adapter }}:</span> {{ err.message }}
                            </div>
                          }
                        </div>
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
export class EdgePanelComponent {
  protected readonly edgeService = inject(EdgePanelService);
  readonly expandedGroupId = signal<number | null>(null);

  toggleExpand(groupId: number): void {
    this.expandedGroupId.update((current) => (current === groupId ? null : groupId));
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
}
