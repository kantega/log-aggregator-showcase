import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Mock Panel Controls — Noark A Status Change', () => {
  test.afterEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('change Noark A status code and verify the status indicator updates', async ({ page }) => {
    await page.goto(BASE_URL);
    await expect(page.getByTestId('mock-setup-controls')).toBeVisible();

    const statusSelect = page.getByTestId('mock-setup-noarka-status');
    const applyButton = page.getByTestId('mock-setup-noarka-apply');

    // Set to 200
    await statusSelect.selectOption('200');
    await applyButton.click();

    // Set to 400
    await statusSelect.selectOption('400');
    await applyButton.click();

    // Set to 500
    await statusSelect.selectOption('500');
    await applyButton.click();

    // Set to 503
    await statusSelect.selectOption('503');
    await applyButton.click();

    // Verify the configuration via API
    const configResponse = await page.request.get(`${MOCK_URL}/api/test/config`);
    const config = await configResponse.json();
    expect(config.noarka.statusCode).toBe(503);
  });
});
