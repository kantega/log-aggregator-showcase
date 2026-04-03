import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Retry Mechanism — Recovery', () => {
  test.beforeEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
    // Configure Noark A to return 500 via API
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });
  });

  test.afterEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('archive recovers after mock restored to 200 and retry button clicked', async ({ page, request }) => {
    test.setTimeout(90000);

    const groupName = `Recovery Group ${Date.now()}`;

    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

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
    }).toPass({ timeout: 70000 });

    // Restore Noark A to 200 via API
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 200 },
    });

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
