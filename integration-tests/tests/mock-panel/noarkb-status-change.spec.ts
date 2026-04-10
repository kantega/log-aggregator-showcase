import { test, expect, MOCK_URL } from '../base-test';

test.describe('Mock Panel Controls — Noark B Status Change', () => {
  test('change Noark B status code and verify the status indicator updates', async ({ page }) => {
    await expect(page.getByTestId('mock-setup-controls')).toBeVisible();

    const statusSelect = page.getByTestId('mock-setup-noarkb-status');
    const applyButton = page.getByTestId('mock-setup-noarkb-apply');

    // Set to 500 — use label matching for Angular [ngValue] numeric binding
    await statusSelect.selectOption({ label: '500' });
    const applyResponse500 = page.waitForResponse(resp => resp.url().includes('/api/test/setup'));
    await applyButton.click();
    await applyResponse500;

    // Verify via API
    let configResponse = await page.request.get(`${MOCK_URL}/api/test/config`);
    let config = await configResponse.json();
    expect(config.noarkb.statusCode).toBe(500);

    // Set back to 200
    await statusSelect.selectOption({ label: '200' });
    const applyResponse200 = page.waitForResponse(resp => resp.url().includes('/api/test/setup'));
    await applyButton.click();
    await applyResponse200;

    configResponse = await page.request.get(`${MOCK_URL}/api/test/config`);
    config = await configResponse.json();
    expect(config.noarkb.statusCode).toBe(200);
  });
});
