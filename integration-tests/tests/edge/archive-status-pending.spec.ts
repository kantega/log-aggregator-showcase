import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Edge Archive Tracking — PENDING Status', () => {
  test('edge panel shows PENDING status briefly before archiving completes', async ({ page, request }) => {
    // Configure Noark A with 3-second delay
    await request.post(`${MOCK_URL}/api/test/reset`);
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 200, delayMs: 3000 },
    });

    const groupName = `Pending Status Group ${Date.now()}`;

    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Create group and add an entry
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Pending entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Pending entry' })).toBeVisible();

    // Check Edge panel shows PENDING or IN_PROGRESS before the delay resolves
    await expect(async () => {
      const edgeCards = page.locator('[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      expect(count).toBeGreaterThan(0);
      const lastCard = edgeCards.last();
      const status = lastCard.getByTestId('edge-group-status');
      const statusText = await status.textContent();
      expect(['PENDING', 'IN_PROGRESS']).toContain(statusText?.trim());
    }).toPass({ timeout: 5000 });

    // After delay passes, close group and check it eventually becomes ARCHIVED
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    await expect(async () => {
      const edgeCards = page.locator('[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      let foundArchived = false;
      for (let i = 0; i < count; i++) {
        const statusEl = edgeCards.nth(i).getByTestId('edge-group-status');
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

    // Reset delay
    await request.post(`${MOCK_URL}/api/test/reset`);
  });
});
