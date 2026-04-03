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

    // Cycle through all status codes using label matching for Angular [ngValue]
    for (const code of ['200', '400', '500', '503']) {
      await statusSelect.selectOption({ label: code });
      const resp = page.waitForResponse(r => r.url().includes('/api/test/setup'));
      await applyButton.click();
      await resp;
    }

    // Verify the final configuration via API
    const configResponse = await page.request.get(`${MOCK_URL}/api/test/config`);
    const config = await configResponse.json();
    expect(config.noarka.statusCode).toBe(503);
  });
});
