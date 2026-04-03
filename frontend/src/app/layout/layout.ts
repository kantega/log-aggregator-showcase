import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  OnDestroy,
  inject,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { forkJoin, catchError, of } from 'rxjs';

import { RouterLink } from '@angular/router';
import { LogGroup, LogEntry } from '../models/log-group.model';
import { LogManagerApiService } from '../services/log-manager-api.service';
import { RabbitmqPanelService } from '../services/rabbitmq-panel.service';
import { EdgePanelService } from '../services/edge-panel.service';
import { MockPanelService } from '../services/mock-panel.service';

import { RabbitmqPanelComponent } from '../panels/rabbitmq-panel';
import { EdgePanelComponent } from '../panels/edge-panel';
import { MockPanelComponent } from '../panels/mock-panel';

@Component({
  selector: 'app-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DatePipe, RouterLink, RabbitmqPanelComponent, EdgePanelComponent, MockPanelComponent],
  template: `
    <div class="flex h-screen overflow-hidden">
      <!-- Left Side: Log Manager + Edge -->
      <div class="w-[55%] flex flex-col border-r border-gray-200 bg-white overflow-hidden">
        <!-- Header -->
        <header class="px-6 py-4 border-b border-gray-200 bg-blue-50 shrink-0 flex items-center justify-between">
          <h1 class="text-xl font-semibold text-blue-800">Log Manager</h1>
          <div class="flex items-center gap-2">
            <a
              routerLink="/docs"
              data-testid="docs-button"
              class="px-3 py-1.5 bg-gray-600 text-white text-xs font-medium rounded-lg hover:bg-gray-700 transition-colors"
            >
              Docs
            </a>
            <button
              type="button"
              data-testid="reset-button"
              (click)="resetAll()"
              class="px-3 py-1.5 bg-red-600 text-white text-xs font-medium rounded-lg hover:bg-red-700 transition-colors"
            >
              RESET
            </button>
          </div>
        </header>

        <!-- Create Group Form -->
        <div class="px-6 py-4 border-b border-gray-200 shrink-0">
          <form (submit)="createGroup($event)" class="flex gap-3">
            <label for="group-name-input" class="sr-only">Group name</label>
            <input
              id="group-name-input"
              data-testid="group-name-input"
              type="text"
              [formControl]="groupNameControl"
              placeholder="New group name..."
              class="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            <button
              type="submit"
              data-testid="create-group-button"
              [disabled]="groupNameControl.invalid"
              class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Create
            </button>
          </form>
        </div>

        <!-- Content area: groups list + detail -->
        <div class="flex-[1] flex overflow-hidden min-h-0">
          <!-- Groups List -->
          <div class="w-1/3 border-r border-gray-200 overflow-y-auto">
            <div class="p-3">
              <h2 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2 px-2">
                Groups
              </h2>
              @if (loadingGroups()) {
                <p class="text-sm text-gray-400 px-2">Loading...</p>
              }
              @for (group of groups(); track group.id) {
                <button
                  type="button"
                  (click)="selectGroup(group)"
                  [attr.data-testid]="'group-item-' + group.id"
                  [class]="
                    'w-full text-left px-3 py-2.5 rounded-lg mb-1 transition-colors ' +
                    (selectedGroup()?.id === group.id
                      ? 'bg-blue-50 border border-blue-200'
                      : 'hover:bg-gray-50')
                  "
                >
                  <div class="flex items-center justify-between">
                    <span data-testid="group-item-name" class="text-sm font-medium text-gray-900 truncate">{{ group.name }}</span>
                    <span
                      data-testid="group-item-status"
                      [class]="
                        'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ' +
                        (group.status === 'OPEN'
                          ? 'bg-green-100 text-green-800'
                          : 'bg-gray-100 text-gray-600')
                      "
                    >
                      {{ group.status }}
                    </span>
                  </div>
                  <p class="text-xs text-gray-400 mt-1">{{ group.createdAt | date: 'short' }}</p>
                </button>
              } @empty {
                @if (!loadingGroups()) {
                  <p class="text-sm text-gray-400 px-2">No groups yet</p>
                }
              }
            </div>
          </div>

          <!-- Selected Group Detail -->
          <div class="flex-1 flex flex-col overflow-hidden">
            @if (selectedGroup(); as group) {
              <div class="px-6 py-4 border-b border-gray-200 shrink-0">
                <div class="flex items-center justify-between">
                  <div>
                    <h2 data-testid="group-detail-name" class="text-lg font-semibold text-gray-900">{{ group.name }}</h2>
                    <p class="text-xs text-gray-500 mt-0.5">
                      Updated: {{ group.updatedAt | date: 'medium' }}
                    </p>
                  </div>
                  <div class="flex items-center gap-3">
                    <span
                      data-testid="group-detail-status"
                      [class]="
                        'inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ' +
                        (group.status === 'OPEN'
                          ? 'bg-green-100 text-green-800'
                          : 'bg-gray-100 text-gray-600')
                      "
                    >
                      {{ group.status }}
                    </span>
                    @if (group.status === 'OPEN') {
                      <button
                        type="button"
                        data-testid="close-group-button"
                        (click)="closeGroup()"
                        class="px-3 py-1.5 bg-red-50 text-red-700 text-xs font-medium rounded-lg hover:bg-red-100 transition-colors"
                      >
                        Close Group
                      </button>
                    }
                  </div>
                </div>
              </div>

              <!-- Add Entry Form (only for OPEN groups) -->
              @if (group.status === 'OPEN') {
                <div class="px-6 py-3 border-b border-gray-200 shrink-0">
                  <form (submit)="addEntry($event)" class="flex gap-3">
                    <label for="entry-content-input" class="sr-only">Entry content</label>
                    <input
                      id="entry-content-input"
                      data-testid="entry-content-input"
                      type="text"
                      [formControl]="entryContentControl"
                      placeholder="Add log entry..."
                      class="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                    <button
                      type="submit"
                      data-testid="add-entry-button"
                      [disabled]="entryContentControl.invalid"
                      class="px-4 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      Add Entry
                    </button>
                  </form>
                </div>
              }

              <!-- Entries List -->
              <div class="flex-1 overflow-y-auto p-6">
                @if (loadingEntries()) {
                  <p class="text-sm text-gray-400">Loading entries...</p>
                }
                @for (entry of entries(); track entry.id) {
                  <div data-testid="entry-item" class="mb-3 p-3 bg-gray-50 rounded-lg border border-gray-100">
                    <p data-testid="entry-content" class="text-sm font-mono text-gray-800 whitespace-pre-wrap">{{ entry.content }}</p>
                    <p class="text-xs text-gray-400 mt-1.5">{{ entry.createdAt | date: 'medium' }}</p>
                  </div>
                } @empty {
                  @if (!loadingEntries()) {
                    <p class="text-sm text-gray-400">No entries yet</p>
                  }
                }
              </div>
            } @else {
              <div class="flex-1 flex items-center justify-center">
                <p class="text-sm text-gray-400">Select a group to view details</p>
              </div>
            }
          </div>
        </div>

        <!-- Edge Panel (lower-left, ~35% of remaining height) -->
        <div class="flex-[1] border-t border-gray-200 min-h-0">
          <app-edge-panel />
        </div>
      </div>

      <!-- Right Side: RabbitMQ (20%) + Mock (80%) -->
      <div class="w-[45%] flex flex-col overflow-hidden bg-gray-50">
        <!-- RabbitMQ Panel (~30%) -->
        <div class="flex-[3] border-b border-gray-200 min-h-0">
          <app-rabbitmq-panel />
        </div>

        <!-- Mock Panel (~70%) -->
        <div class="flex-[7] min-h-0">
          <app-mock-panel />
        </div>
      </div>
    </div>

    <!-- Toast notification -->
    @if (toastMessage()) {
      <div
        data-testid="toast"
        [class]="
          'fixed bottom-4 right-4 px-4 py-2 rounded-lg shadow-lg text-sm font-medium transition-opacity ' +
          (toastType() === 'success' ? 'bg-green-600 text-white' : 'bg-red-600 text-white')
        "
        role="alert"
      >
        {{ toastMessage() }}
      </div>
    }
  `,
})
export class LayoutComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly api = inject(LogManagerApiService);
  private readonly rabbitmqService = inject(RabbitmqPanelService);
  private readonly edgeService = inject(EdgePanelService);
  private readonly mockService = inject(MockPanelService);

  readonly groups = signal<LogGroup[]>([]);
  readonly selectedGroup = signal<LogGroup | null>(null);
  readonly entries = signal<LogEntry[]>([]);
  readonly loadingGroups = signal(false);
  readonly loadingEntries = signal(false);
  readonly toastMessage = signal<string | null>(null);
  readonly toastType = signal<'success' | 'error'>('success');

  readonly groupNameControl = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly entryContentControl = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  ngOnInit(): void {
    this.loadGroups();
    this.rabbitmqService.connect();
    this.edgeService.startPolling();
    this.mockService.startPolling();
  }

  ngOnDestroy(): void {
    this.rabbitmqService.disconnect();
    this.edgeService.stopPolling();
    this.mockService.stopPolling();
  }

  createGroup(event: Event): void {
    event.preventDefault();
    const name = this.groupNameControl.value.trim();
    if (!name) return;

    this.api.createGroup(name).subscribe({
      next: () => {
        this.groupNameControl.reset();
        this.loadGroups();
      },
    });
  }

  selectGroup(group: LogGroup): void {
    this.selectedGroup.set(group);
    this.loadEntries(group.id);
  }

  addEntry(event: Event): void {
    event.preventDefault();
    const group = this.selectedGroup();
    if (!group) return;

    const content = this.entryContentControl.value.trim();
    if (!content) return;

    this.api.addEntry(group.id, content).subscribe({
      next: () => {
        this.entryContentControl.reset();
        this.loadEntries(group.id);
      },
    });
  }

  closeGroup(): void {
    const group = this.selectedGroup();
    if (!group) return;

    this.api.closeGroup(group.id).subscribe({
      next: (updated) => {
        this.selectedGroup.set(updated);
        this.loadGroups();
      },
    });
  }

  resetAll(): void {
    if (!confirm('Reset all services? This clears all data.')) return;

    const rabbitHeaders = new HttpHeaders({
      Authorization: `Basic ${btoa('myuser:secret')}`,
    });

    forkJoin({
      logManager: this.api.deleteAll().pipe(catchError(() => of(null))),
      edge: this.edgeService.deleteAll().pipe(catchError(() => of(null))),
      mock: this.http.post('/mock-api/api/test/reset', {}).pipe(catchError(() => of(null))),
      rabbit: this.http.delete('/rabbitmq-api/api/queues/%2F/log-events-queue/contents', { headers: rabbitHeaders }).pipe(catchError(() => of(null))),
    }).subscribe(() => {
      this.groups.set([]);
      this.selectedGroup.set(null);
      this.entries.set([]);
      this.rabbitmqService.clearMessages();
      this.showToast('Reset complete', 'success');
    });
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage.set(message);
    this.toastType.set(type);
    setTimeout(() => this.toastMessage.set(null), 3000);
  }

  private loadGroups(): void {
    this.loadingGroups.set(true);
    this.api.getGroups().subscribe({
      next: (groups) => {
        this.groups.set(groups);
        this.loadingGroups.set(false);
      },
      error: () => {
        this.loadingGroups.set(false);
      },
    });
  }

  private loadEntries(groupId: number): void {
    this.loadingEntries.set(true);
    this.api.getEntries(groupId).subscribe({
      next: (entries) => {
        this.entries.set(entries);
        this.loadingEntries.set(false);
      },
      error: () => {
        this.entries.set([]);
        this.loadingEntries.set(false);
      },
    });
  }
}
