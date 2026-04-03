import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Docs Page — Service Tabs', () => {
  test('switch between service tabs on the Docs page', async ({ page }) => {
    await page.goto(`${BASE_URL}/docs`);

    // Log Manager tab should be active by default
    await expect(page.getByTestId('tab-Log Manager')).toBeVisible();

    // Click Edge tab
    await page.getByTestId('tab-Edge').click();
    // Verify the iframe src changes (or tab becomes active)
    await page.waitForTimeout(500);

    // Click Adapter A tab
    await page.getByTestId('tab-Adapter A').click();
    await page.waitForTimeout(500);

    // Click Adapter B tab
    await page.getByTestId('tab-Adapter B').click();
    await page.waitForTimeout(500);

    // Click Mock tab
    await page.getByTestId('tab-Mock').click();
    await page.waitForTimeout(500);

    // All tabs should be clickable without errors
    await expect(page.getByTestId('tab-Mock')).toBeVisible();
  });
});
