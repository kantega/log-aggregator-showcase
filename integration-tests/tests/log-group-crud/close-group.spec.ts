import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Log Group CRUD — Close Group', () => {
  test('close a log group and verify it becomes read-only', async ({ page }) => {
    const groupName = `Close Test Group ${Date.now()}`;

    await page.goto(BASE_URL);

    // Create group, select it, add one entry
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Test entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-item')).toHaveCount(1);
    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');

    // Close the group
    await page.getByTestId('close-group-button').click();

    // Verify CLOSED status in both detail and list
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');
    await expect(groupButton.getByTestId('group-item-status')).toHaveText('CLOSED');

    // Verify read-only: no input, no add button, no close button
    await expect(page.getByTestId('entry-content-input')).not.toBeVisible();
    await expect(page.getByTestId('add-entry-button')).not.toBeVisible();
    await expect(page.getByTestId('close-group-button')).not.toBeVisible();

    // Entry still visible
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Test entry' })).toBeVisible();
  });
});
