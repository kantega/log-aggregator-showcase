import { test, expect } from '../base-test';

test.describe('Docs Page — Navigate to Docs', () => {
  test('navigate to Docs page via the Docs button and verify structure', async ({ page }) => {

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
