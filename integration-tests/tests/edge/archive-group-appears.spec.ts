import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Edge Archive Tracking — Group Card Appears', () => {
  test.beforeEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('edge panel shows archive group card after group creation', async ({ page }) => {
    const groupName = `Edge Track Group ${Date.now()}`;

    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Create a group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    await expect(page.getByRole('button', { name: new RegExp(groupName) })).toBeVisible();

    // Wait for Edge panel to show a card for this group
    await expect(async () => {
      const edgeCards = page.locator('[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      expect(count).toBeGreaterThan(0);
    }).toPass({ timeout: 10000 });

    // Verify the card has a status badge
    const edgeCards = page.locator('[data-testid^="edge-group-"]');
    const lastCard = edgeCards.last();
    await expect(lastCard.getByTestId('edge-group-status')).toBeVisible();
  });
});
