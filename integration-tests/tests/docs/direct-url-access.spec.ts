import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Docs Page — Direct URL Access', () => {
  test('docs page is accessible directly via URL', async ({ page }) => {
    await page.goto(`${BASE_URL}/docs`);

    // Verify page loads correctly
    await expect(page.getByTestId('back-button')).toBeVisible();

    // All service tabs present
    await expect(page.getByTestId('tab-Log Manager')).toBeVisible();
    await expect(page.getByTestId('tab-Edge')).toBeVisible();
    await expect(page.getByTestId('tab-Adapter A')).toBeVisible();
    await expect(page.getByTestId('tab-Adapter B')).toBeVisible();
    await expect(page.getByTestId('tab-Mock')).toBeVisible();
  });
});
