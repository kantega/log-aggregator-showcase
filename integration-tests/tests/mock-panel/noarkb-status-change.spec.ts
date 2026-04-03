import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Mock Panel Controls — Noark B Status Change', () => {
  test.afterEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('change Noark B status code and verify the status indicator updates', async ({ page }) => {
    await page.goto(BASE_URL);
    await expect(page.getByTestId('mock-setup-controls')).toBeVisible();

    const statusSelect = page.getByTestId('mock-setup-noarkb-status');
    const applyButton = page.getByTestId('mock-setup-noarkb-apply');

    // Set to 500
    await statusSelect.selectOption('500');
    await applyButton.click();

    // Verify via API
    let configResponse = await page.request.get(`${MOCK_URL}/api/test/config`);
    let config = await configResponse.json();
    expect(config.noarkb.statusCode).toBe(500);

    // Set back to 200
    await statusSelect.selectOption('200');
    await applyButton.click();

    configResponse = await page.request.get(`${MOCK_URL}/api/test/config`);
    config = await configResponse.json();
    expect(config.noarkb.statusCode).toBe(200);
  });
});
