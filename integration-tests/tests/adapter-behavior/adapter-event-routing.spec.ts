import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Adapter Behavior — Event Routing', () => {
  test.beforeEach(async ({ page, request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
    await page.goto(BASE_URL);
    page.on('dialog', dialog => dialog.accept());
    await page.getByTestId('reset-button').click();
    await page.waitForTimeout(2000);
  });

  test('Adapter A archives on every event, Adapter B only on GROUP_CLOSED', async ({ page, request }) => {
    const groupName = `Routing Test ${Date.now()}`;

    // Create group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    // Add 3 entries
    for (const entry of ['Route entry one', 'Route entry two', 'Route entry three']) {
      await page.getByTestId('entry-content-input').fill(entry);
      await page.getByTestId('add-entry-button').click();
      await expect(page.getByTestId('entry-content').filter({ hasText: entry })).toBeVisible();
    }

    // Wait for ENTRY_ADDED archiving to process
    await page.waitForTimeout(5000);

    // Check: Noark A has 3 requests (ENTRY_ADDED), Noark B has 0
    let historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    let history = await historyResponse.json();
    let noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    let noarkbRequests = history.filter((r: any) => r.endpoint === 'noarkb');

    expect(noarkaRequests.length).toBe(3);
    expect(noarkbRequests.length).toBe(0);

    // Close the group
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for GROUP_CLOSED archiving
    await page.waitForTimeout(5000);

    // Check: Noark A has 4 (3 ENTRY_ADDED + 1 GROUP_CLOSED), Noark B has 1 (GROUP_CLOSED)
    historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    history = await historyResponse.json();
    noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    noarkbRequests = history.filter((r: any) => r.endpoint === 'noarkb');

    expect(noarkaRequests.length).toBe(4);
    expect(noarkbRequests.length).toBe(1);
  });
});
