import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Error Handling — Noark A 400 Failure', () => {
  test.beforeEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test.afterEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('configure Noark A to return 400 and verify failure tracking', async ({ page }) => {
    const groupName = `Bad Request A ${Date.now()}`;

    await page.goto(BASE_URL);

    // Configure Noark A to return 400
    await page.getByTestId('mock-setup-noarka-status').selectOption('400');
    await page.getByTestId('mock-setup-noarka-apply').click();

    // Create group and add entry
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Bad request entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Bad request entry' })).toBeVisible();

    // Wait for Edge to show FAILED
    await expect(async () => {
      const edgeCards = page.locator('[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      let foundFailed = false;
      for (let i = 0; i < count; i++) {
        const statusEl = edgeCards.nth(i).getByTestId('edge-group-status');
        if (await statusEl.isVisible()) {
          const text = await statusEl.textContent();
          if (text?.includes('FAILED')) {
            foundFailed = true;
            break;
          }
        }
      }
      expect(foundFailed).toBe(true);
    }).toPass({ timeout: 30000 });
  });
});
