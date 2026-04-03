import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Form Validation — Group Name Error State', () => {
  test('group name persists in input after typing without submission', async ({ page }) => {
    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    const input = page.getByTestId('group-name-input');
    const button = page.getByTestId('create-group-button');

    // Type a name
    await input.fill('Error Test Group');
    await expect(input).toHaveValue('Error Test Group');
    await expect(button).toBeEnabled();

    // The input should retain its value (no unexpected clearing)
    await page.waitForTimeout(1000);
    await expect(input).toHaveValue('Error Test Group');

    // Successfully create the group to verify the button works
    await button.click();
    const groupButton = page.getByRole('button', { name: /Error Test Group/ });
    await expect(groupButton).toBeVisible();

    // After successful creation, input should be cleared
    await expect(input).toHaveValue('');
  });
});
