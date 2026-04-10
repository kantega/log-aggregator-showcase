import { test, expect } from '../base-test';

test.describe('Form Validation — Closed Group Read-Only', () => {
  test('cannot add entries to a closed group', async ({ page }) => {
    const groupName = `Read Only Group ${Date.now()}`;

    // Create group, add entry, close
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Only entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Only entry' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Verify the group is fully read-only
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Only entry' })).toBeVisible();
    await expect(page.getByTestId('entry-content-input')).not.toBeVisible();
    await expect(page.getByTestId('add-entry-button')).not.toBeVisible();
    await expect(page.getByTestId('close-group-button')).not.toBeVisible();

    // Click on the group again to re-verify it stays read-only
    await groupButton.click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');
    await expect(page.getByTestId('entry-content-input')).not.toBeVisible();
  });
});
