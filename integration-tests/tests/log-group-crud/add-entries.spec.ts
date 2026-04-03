import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Log Group CRUD — Add Entries', () => {
  test('add log entries to an open group', async ({ page }) => {
    const groupName = `Entry Test Group ${Date.now()}`;

    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Create group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();

    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await expect(groupButton.getByTestId('group-item-status')).toHaveText('OPEN');

    // Select the group
    await groupButton.click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');
    await expect(page.getByTestId('entry-item')).toHaveCount(0);

    // Add 3 entries
    const entries = ['Alpha entry', 'Beta entry', 'Gamma entry'];
    for (let i = 0; i < entries.length; i++) {
      await page.getByTestId('entry-content-input').fill(entries[i]);
      await page.getByTestId('add-entry-button').click();
      await expect(page.getByTestId('entry-content').filter({ hasText: entries[i] })).toBeVisible();
      await expect(page.getByTestId('entry-content-input')).toHaveValue('');
      await expect(page.getByTestId('entry-item')).toHaveCount(i + 1);
    }
  });
});
