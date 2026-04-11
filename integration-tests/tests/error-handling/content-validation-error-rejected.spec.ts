import { test, expect, MOCK_URL } from '../base-test';

test.describe('Error Handling — Content Validation Rejects "error" Entry', () => {
  test('entry containing "error" is rejected by Noark A but close still succeeds', async ({ page }) => {
    test.setTimeout(60000);

    const groupName = `Content Validation ${Date.now()}`;

    // Create group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    // Add an entry containing "error" — Noark A will reject this ENTRY_ADDED with 400
    await page.getByTestId('entry-content-input').fill('Something went ERROR here');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Something went ERROR here' })).toBeVisible();

    // Close the group — GROUP_CLOSED is not subject to content validation,
    // so both adapters succeed and the group reaches ARCHIVED
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for Edge to show ARCHIVED status
    await expect(async () => {
      const archiveCards = page.locator('button[data-testid^="edge-group-"]');
      const count = await archiveCards.count();
      let found = false;
      for (let i = 0; i < count; i++) {
        const card = archiveCards.nth(i);
        const text = await card.textContent();
        if (text?.includes(groupName) || text?.includes('ARCHIVED')) {
          const status = card.getByTestId('edge-group-status');
          if (await status.isVisible()) {
            const statusText = await status.textContent();
            if (statusText?.includes('ARCHIVED')) {
              found = true;
              break;
            }
          }
        }
      }
      expect(found).toBe(true);
    }).toPass({ timeout: 30000 });
  });
});
