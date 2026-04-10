import { test, expect } from '../base-test';

test.describe('Reset Functionality — Post-Reset', () => {
  test('application is functional after a reset', async ({ page }) => {
    // Base fixture already performed reset and verified lists are empty
    const groupName = `Post Reset Group ${Date.now()}`;

    // Create a new group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await expect(groupButton.getByTestId('group-item-status')).toHaveText('OPEN');

    // Add entry and close
    await groupButton.click();
    await page.getByTestId('entry-content-input').fill('Post reset entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Post reset entry' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Verify Edge shows ARCHIVED
    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
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
    }).toPass({ timeout: 15000 });
  });
});
