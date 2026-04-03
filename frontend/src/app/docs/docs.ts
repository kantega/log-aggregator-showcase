import { Component, ChangeDetectionStrategy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

interface ServiceDoc {
  name: string;
  url: string;
  port: number;
  color: string;
}

@Component({
  selector: 'app-docs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <div class="flex flex-col h-screen bg-gray-50">
      <!-- Header -->
      <header class="px-6 py-4 border-b border-gray-200 bg-white shrink-0 flex items-center justify-between">
        <h1 class="text-xl font-semibold text-gray-800">API Documentation</h1>
        <a
          routerLink="/"
          data-testid="back-button"
          class="px-3 py-1.5 bg-blue-600 text-white text-xs font-medium rounded-lg hover:bg-blue-700 transition-colors"
        >
          Back to App
        </a>
      </header>

      <!-- Tabs -->
      <nav class="px-6 pt-3 bg-white border-b border-gray-200 shrink-0" aria-label="Service documentation tabs">
        <div class="flex gap-1" role="tablist">
          @for (service of services; track service.name) {
            <button
              type="button"
              role="tab"
              [attr.aria-selected]="activeTab() === service.name"
              [attr.data-testid]="'tab-' + service.name"
              (click)="selectTab(service)"
              [class]="
                'px-4 py-2 text-sm font-medium rounded-t-lg transition-colors border-b-2 ' +
                (activeTab() === service.name
                  ? service.color + ' border-current'
                  : 'text-gray-500 border-transparent hover:text-gray-700 hover:border-gray-300')
              "
            >
              {{ service.name }}
              <span class="text-xs text-gray-400 ml-1">:{{ service.port }}</span>
            </button>
          }
        </div>
      </nav>

      <!-- Content -->
      <div class="flex-1 min-h-0">
        <iframe
          [src]="activeUrl()"
          [title]="activeTab() + ' API documentation'"
          class="w-full h-full border-0"
        ></iframe>
      </div>
    </div>
  `,
})
export class DocsComponent {
  private readonly sanitizer: DomSanitizer;
  readonly activeTab = signal('Log Manager');
  readonly activeUrl = signal<SafeResourceUrl>('');

  readonly services: ServiceDoc[] = [
    { name: 'Log Manager', url: 'http://localhost:8080/swagger-ui.html', port: 8080, color: 'text-blue-600' },
    { name: 'Edge', url: 'http://localhost:8081/swagger-ui.html', port: 8081, color: 'text-amber-600' },
    { name: 'Adapter A', url: 'http://localhost:8082/swagger-ui.html', port: 8082, color: 'text-teal-600' },
    { name: 'Adapter B', url: 'http://localhost:8083/swagger-ui.html', port: 8083, color: 'text-cyan-600' },
    { name: 'Mock', url: 'http://localhost:8084/swagger-ui.html', port: 8084, color: 'text-emerald-600' },
  ];

  constructor(sanitizer: DomSanitizer) {
    this.sanitizer = sanitizer;
    this.activeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.services[0].url));
  }

  selectTab(service: ServiceDoc): void {
    this.activeTab.set(service.name);
    this.activeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(service.url));
  }
}
