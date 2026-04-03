import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Form Validation — Entry Content Required', () => {
  test('add entry button remains disabled until entry content is non-empty', async ({ page }) => {
    const groupName = `Validation Group ${Date.now()}`;

    await page.goto(BASE_URL);

    // Create and select a group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');

    const input = page.getByTestId('entry-content-input');
    const button = page.getByTestId('add-entry-button');

    // Initially disabled
    await expect(button).toBeDisabled();

    // Type content — enabled
    await input.fill('Some content');
    await expect(button).toBeEnabled();

    // Clear — disabled again
    await input.fill('');
    await expect(button).toBeDisabled();
  });
});
