import { test, expect } from '../base-test';

test.describe('Edge Archive Tracking — Group Card Appears', () => {
  test('edge panel shows archive group card after group creation', async ({ page }) => {
    const groupName = `Edge Track Group ${Date.now()}`;

    // Create a group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    await expect(page.getByRole('button', { name: new RegExp(groupName) })).toBeVisible();

    // Wait for Edge panel to show a card with a status badge
    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      expect(count).toBeGreaterThan(0);
      // Verify at least one card has a visible status badge
      const lastCard = edgeCards.last();
      const status = lastCard.getByTestId('edge-group-status');
      await expect(status).toBeVisible();
    }).toPass({ timeout: 15000 });
  });
});
