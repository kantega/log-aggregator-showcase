import { test, expect, MOCK_URL } from '../base-test';

test.describe('Mock Panel Controls — Request History', () => {
  test('mock panel shows request history for each adapter', async ({ page, request }) => {
    const groupName = `History Group ${Date.now()}`;

    // Create group, add entry, close
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('History entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'History entry' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for archiving to complete
    await page.waitForTimeout(5000);

    // Verify request history via API
    const historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    const history = await historyResponse.json();

    const noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    const noarkbRequests = history.filter((r: any) => r.endpoint === 'noarkb');

    // Noark A: ENTRY_ADDED + GROUP_CLOSED = at least 2
    expect(noarkaRequests.length).toBeGreaterThanOrEqual(2);
    // Noark B: GROUP_CLOSED only = at least 1
    expect(noarkbRequests.length).toBeGreaterThanOrEqual(1);

    // Verify request has expected fields
    const sampleRequest = noarkaRequests[0];
    expect(sampleRequest).toHaveProperty('method');
    expect(sampleRequest).toHaveProperty('path');
    expect(sampleRequest).toHaveProperty('timestamp');
  });
});
