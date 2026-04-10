import { test, expect, MOCK_URL } from '../base-test';

test.describe('Mock Panel Controls — Noark A Delay Config', () => {
  test('configure Noark A delay and verify it takes effect', async ({ page, request }) => {
    const groupName = `Delay Test Group ${Date.now()}`;

    await expect(page.getByTestId('mock-setup-controls')).toBeVisible();

    // Set 3-second delay on Noark A via UI
    await page.getByTestId('mock-setup-noarka-delay').fill('3');
    const applyResponse = page.waitForResponse(resp => resp.url().includes('/api/test/setup'));
    await page.getByTestId('mock-setup-noarka-apply').click();
    await applyResponse;

    // Verify delay was configured via API
    const configResponse = await request.get(`${MOCK_URL}/api/test/config`);
    const config = await configResponse.json();
    expect(config.noarka.delayMs).toBe(3000);

    // Create group and add entry
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Delay entry');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Delay entry' })).toBeVisible();

    // Immediately check Edge — should NOT be ARCHIVED yet (delay is 3s)
    await page.waitForTimeout(500);
    const edgeCards = page.locator('button[data-testid^="edge-group-"]');
    const count = await edgeCards.count();
    if (count > 0) {
      const lastCard = edgeCards.last();
      const status = lastCard.getByTestId('edge-group-status');
      if (await status.isVisible()) {
        const statusText = await status.textContent();
        expect(statusText?.trim()).not.toBe('ARCHIVED');
      }
    }

    // Wait for delay + processing, then close and verify it eventually archives
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    await expect(async () => {
      const cards = page.locator('button[data-testid^="edge-group-"]');
      const cnt = await cards.count();
      let foundArchived = false;
      for (let i = 0; i < cnt; i++) {
        const statusEl = cards.nth(i).getByTestId('edge-group-status');
        if (await statusEl.isVisible()) {
          const text = await statusEl.textContent();
          if (text?.includes('ARCHIVED')) {
            foundArchived = true;
            break;
          }
        }
      }
      expect(foundArchived).toBe(true);
    }).toPass({ timeout: 20000 });
  });
});
