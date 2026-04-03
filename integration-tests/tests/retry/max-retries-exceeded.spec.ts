import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Retry Mechanism — Max Retries Exceeded', () => {
  test.beforeEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test.afterEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('edge retries failed archive up to max retries then marks as FAILED', async ({ page, request }) => {
    // Set a longer test timeout since retries take time (3 retries × 3s intervals)
    test.setTimeout(90000);

    const groupName = `Retry Limit ${Date.now()}`;

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

    await page.getByTestId('entry-content-input').fill('Retry entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Retry entry' })).toBeVisible();

    // Wait for Edge to exhaust retries and show FAILED
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

    // Verify multiple attempts were made via mock history
    const historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    const history = await historyResponse.json();
    const noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    // Initial attempt + retries = at least 2 requests
    expect(noarkaRequests.length).toBeGreaterThanOrEqual(2);
  });
});
