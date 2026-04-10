import { test, expect, MOCK_URL } from '../base-test';

test.describe('Retry Mechanism — Max Retries Exceeded', () => {
  test('edge retries failed archive up to max retries then marks as FAILED', async ({ page, request }) => {
    test.setTimeout(90000);

    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });

    const groupName = `Retry Limit ${Date.now()}`;

    // Create group and add entry
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Retry entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Retry entry' })).toBeVisible();

    // Close the group to trigger archiving (Edge only archives on GROUP_CLOSED)
    await page.getByTestId('close-group-button').click();

    // Wait for Edge to exhaust retries and show FAILED
    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      let foundFailed = false;
      for (let i = 0; i < count; i++) {
        const statusEl = edgeCards.nth(i).getByTestId('edge-group-status');
        if (await statusEl.isVisible()) {
          const text = await statusEl.textContent();
          if (text?.includes('FAILED')) {
            foundFailed = true;
            break;
          }
        }
      }
      expect(foundFailed).toBe(true);
    }).toPass({ timeout: 70000 });

    // Verify multiple attempts were made via mock history
    const historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    const history = await historyResponse.json();
    const noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    expect(noarkaRequests.length).toBeGreaterThanOrEqual(2);
  });
});
