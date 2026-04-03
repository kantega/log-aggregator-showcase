import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Docs Page — Navigate to Docs', () => {
  test('navigate to Docs page via the Docs button and verify structure', async ({ page }) => {
    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Click Docs button
    await page.getByTestId('docs-button').click();

    // Verify URL
    await expect(page).toHaveURL(/\/docs/);

    // Verify page structure
    await expect(page.getByTestId('back-button')).toBeVisible();

    // Verify service tabs are present
    await expect(page.getByTestId('tab-Log Manager')).toBeVisible();
    await expect(page.getByTestId('tab-Edge')).toBeVisible();
    await expect(page.getByTestId('tab-Adapter A')).toBeVisible();
    await expect(page.getByTestId('tab-Adapter B')).toBeVisible();
    await expect(page.getByTestId('tab-Mock')).toBeVisible();
  });
});
