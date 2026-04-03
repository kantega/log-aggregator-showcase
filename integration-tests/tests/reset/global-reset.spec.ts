import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Reset Functionality — Global Reset', () => {
  test('RESET button clears all groups, entries, Edge data, and mock history', async ({ page, request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Handle confirmation dialogs
    page.on('dialog', dialog => dialog.accept());

    // Create some data first
    await page.getByTestId('group-name-input').fill(`Reset Group One ${Date.now()}`);
    await page.getByTestId('create-group-button').click();
    await page.waitForTimeout(500);

    await page.getByTestId('group-name-input').fill(`Reset Group Two ${Date.now()}`);
    await page.getByTestId('create-group-button').click();
    await page.waitForTimeout(500);

    // Verify groups exist
    const groupButtons = page.locator('[data-testid^="group-item-"]');
    await expect(async () => {
      const count = await groupButtons.count();
      expect(count).toBeGreaterThanOrEqual(2);
    }).toPass({ timeout: 5000 });

    // Click RESET
    await page.getByTestId('reset-button').click();
    await page.waitForTimeout(3000);

    // Verify groups list is empty
    await expect(async () => {
      const groups = page.locator('[data-testid^="group-item-"]');
      const count = await groups.count();
      expect(count).toBe(0);
    }).toPass({ timeout: 10000 });

    // Verify Edge panel is empty
    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      expect(count).toBe(0);
    }).toPass({ timeout: 5000 });

    // Verify mock history is cleared
    const historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    const history = await historyResponse.json();
    expect(history.length).toBe(0);
  });
});
