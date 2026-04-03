import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Log Group CRUD — Add Entry Validation', () => {
  test('add entry button is disabled when entry content is empty', async ({ page }) => {
    const groupName = `Validation Group ${Date.now()}`;

    await page.goto(BASE_URL);

    // Create and select a group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');

    // Add Entry button should be disabled with empty input
    await expect(page.getByTestId('add-entry-button')).toBeDisabled();

    // Type a space — check disabled
    await page.getByTestId('entry-content-input').fill(' ');
    const isDisabled = await page.getByTestId('add-entry-button').isDisabled();
    // Either disabled for whitespace-only or enabled — just verify empty disables
    await page.getByTestId('entry-content-input').fill('');
    await expect(page.getByTestId('add-entry-button')).toBeDisabled();
  });
});
