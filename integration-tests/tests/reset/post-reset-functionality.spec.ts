import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Reset Functionality — Post-Reset', () => {
  test('application is functional after a reset', async ({ page, request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
    await page.goto(BASE_URL);
    page.on('dialog', dialog => dialog.accept());

    // Reset
    await page.getByTestId('reset-button').click();
    await page.waitForTimeout(3000);

    // Verify groups list is empty
    await expect(async () => {
      const groups = page.locator('[data-testid^="group-item-"]');
      const count = await groups.count();
      expect(count).toBe(0);
    }).toPass({ timeout: 10000 });

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
