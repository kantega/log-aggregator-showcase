import { test as base, expect } from '@playwright/test';

export const BASE_URL = 'http://localhost:4200';
export const MOCK_URL = 'http://localhost:8084';
export const EDGE_URL = 'http://localhost:8081';
export const LOG_MANAGER_URL = 'http://localhost:8080';

export { expect };

export const test = base.extend({
  page: async ({ page }, use) => {
    // Accept confirmation dialogs (reset triggers confirm())
    page.on('dialog', dialog => dialog.accept());

    // Navigate and wait for app to load
    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Click reset button
    await page.getByTestId('reset-button').click();

    // Wait for all lists to be empty
    await expect(async () => {
      const groups = page.locator('[data-testid^="group-item-"]');
      expect(await groups.count()).toBe(0);
    }).toPass({ timeout: 10000 });

    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
      expect(await edgeCards.count()).toBe(0);
    }).toPass({ timeout: 10000 });

    await use(page);
  },
});
