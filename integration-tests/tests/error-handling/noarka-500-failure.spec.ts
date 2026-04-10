import { test, expect, MOCK_URL } from '../base-test';

test.describe('Error Handling — Noark A 500 Failure', () => {
  test('configure Noark A to return 500 and verify failure tracked in Edge', async ({ page, request }) => {
    test.setTimeout(60000);

    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });

    const groupName = `Failure Test A ${Date.now()}`;

    // Create group and add entry
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Failure entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Failure entry' })).toBeVisible();

    // Close the group to trigger archiving (Edge only archives on GROUP_CLOSED)
    await page.getByTestId('close-group-button').click();

    // Wait for Edge to show FAILED status (retries may take time)
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
    }).toPass({ timeout: 45000 });
  });
});
