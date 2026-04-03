import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Retry Mechanism — Recovery', () => {
  test.beforeEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test.afterEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('archive recovers after mock restored to 200 and retry button clicked', async ({ page, request }) => {
    test.setTimeout(90000);

    const groupName = `Recovery Group ${Date.now()}`;

    await page.goto(BASE_URL);

    // Configure Noark A to return 500
    await page.getByTestId('mock-setup-noarka-status').selectOption('500');
    await page.getByTestId('mock-setup-noarka-apply').click();

    // Create group and add entry
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Recovery entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Recovery entry' })).toBeVisible();

    // Wait for FAILED status in Edge
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
    }).toPass({ timeout: 60000 });

    // Restore Noark A to 200
    await page.getByTestId('mock-setup-noarka-status').selectOption('200');
    await page.getByTestId('mock-setup-noarka-apply').click();

    // Click the failed Edge card to select it, then click retry
    const edgeCards = page.locator('[data-testid^="edge-group-"]');
    const count = await edgeCards.count();
    for (let i = 0; i < count; i++) {
      const statusEl = edgeCards.nth(i).getByTestId('edge-group-status');
      if (await statusEl.isVisible()) {
        const text = await statusEl.textContent();
        if (text?.includes('FAILED')) {
          await edgeCards.nth(i).click();
          break;
        }
      }
    }

    // Click retry button
    await page.getByTestId('edge-retry-button').click();

    // Wait for ARCHIVED status
    await expect(async () => {
      const cards = page.locator('[data-testid^="edge-group-"]');
      const cnt = await cards.count();
      let foundArchived = false;
      for (let i = 0; i < cnt; i++) {
        const statusEl = cards.nth(i).getByTestId('edge-group-status');
        if (await statusEl.isVisible()) {
          const text = await statusEl.textContent();
          if (text?.includes('ARCHIVED')) {
            foundArchived = true;
            break;
          }
        }
      }
      expect(foundArchived).toBe(true);
    }).toPass({ timeout: 20000 });
  });
});
