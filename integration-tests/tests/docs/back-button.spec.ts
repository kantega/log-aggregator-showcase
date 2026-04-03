import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Docs Page — Back Button', () => {
  test('back to App link navigates back from Docs page', async ({ page }) => {
    await page.goto(`${BASE_URL}/docs`);
    await expect(page.getByTestId('back-button')).toBeVisible();

    // Click back
    await page.getByTestId('back-button').click();

    // Verify we're back at the main app
    await expect(page).toHaveURL(BASE_URL + '/');
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();
  });
});
