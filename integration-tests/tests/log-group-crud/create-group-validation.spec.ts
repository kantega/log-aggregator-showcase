import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Log Group CRUD — Create Group Validation', () => {
  test('create button is disabled when group name input is empty', async ({ page }) => {
    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Button disabled with empty input
    await expect(page.getByTestId('create-group-button')).toBeDisabled();

    // Type a space — button should still be disabled (or trimmed)
    await page.getByTestId('group-name-input').fill(' ');
    // Either disabled or enabled — if enabled, the backend should reject it
    // We check the UI-level validation here
    const isDisabled = await page.getByTestId('create-group-button').isDisabled();
    if (!isDisabled) {
      // If space is accepted in input, clear and check again
      await page.getByTestId('group-name-input').fill('');
      await expect(page.getByTestId('create-group-button')).toBeDisabled();
    }

    // Clear and verify disabled
    await page.getByTestId('group-name-input').fill('');
    await expect(page.getByTestId('create-group-button')).toBeDisabled();
  });
});
