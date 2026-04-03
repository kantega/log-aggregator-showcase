import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const MOCK_URL = 'http://localhost:8084';

test.describe('Error Handling — Noark B Skips ENTRY_ADDED', () => {
  test.beforeEach(async ({ request }) => {
    await request.post(`${MOCK_URL}/api/test/reset`);
  });

  test('Noark B only archives on GROUP_CLOSED, skips ENTRY_ADDED events', async ({ page, request }) => {
    const groupName = `B Skip Test ${Date.now()}`;

    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Create group, add 2 entries
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('First skip entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'First skip entry' })).toBeVisible();

    await page.getByTestId('entry-content-input').fill('Second skip entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Second skip entry' })).toBeVisible();

    // Wait a moment for archiving to process
    await page.waitForTimeout(3000);

    // Check history: Noark A should have requests, Noark B should have 0
    let historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    let history = await historyResponse.json();
    let noarkbRequests = history.filter((r: any) => r.endpoint === 'noarkb');
    expect(noarkbRequests.length).toBe(0);

    let noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    expect(noarkaRequests.length).toBeGreaterThanOrEqual(2);

    // Now close the group
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for archiving to complete
    await page.waitForTimeout(3000);

    // Noark B should now have exactly 1 request (GROUP_CLOSED only)
    historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    history = await historyResponse.json();
    noarkbRequests = history.filter((r: any) => r.endpoint === 'noarkb');
    expect(noarkbRequests.length).toBe(1);

    // Noark A should have 3 total (2 ENTRY_ADDED + 1 GROUP_CLOSED)
    noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    expect(noarkaRequests.length).toBeGreaterThanOrEqual(3);
  });
});
