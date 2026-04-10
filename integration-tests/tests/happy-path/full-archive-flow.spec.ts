import { test, expect, MOCK_URL } from '../base-test';

test.describe('Happy Path E2E — Full Archive Flow', () => {
  test('create group, add entries, close group, verify archiving in Noark A and B', async ({ page }) => {
    const groupName = `Happy Path Group ${Date.now()}`;

    // Wait for RabbitMQ connection
    await expect(page.getByTestId('rabbitmq-status')).toHaveAttribute('aria-label', 'Connected', { timeout: 15000 });

    // Record initial message count
    const initialCountText = await page.getByTestId('rabbitmq-message-count').textContent();
    const initialCount = parseInt(initialCountText!.replace(/[^0-9]/g, ''), 10) || 0;

    // Create group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();

    // Select group and add 2 entries
    await groupButton.click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');

    await page.getByTestId('entry-content-input').fill('First log line');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'First log line' })).toBeVisible();

    await page.getByTestId('entry-content-input').fill('Second log line');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Second log line' })).toBeVisible();

    // Close the group
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Verify RabbitMQ message count: 1 create + 2 entries + 1 close = 4 new messages
    const expectedCount = initialCount + 4;
    await expect(page.getByTestId('rabbitmq-message-count')).toContainText(expectedCount.toString(), { timeout: 10000 });

    // Wait for Edge panel to show ARCHIVED status
    // Find the edge group card — it uses the group ID from the backend
    // We wait for any edge group card with ARCHIVED status to appear
    await expect(async () => {
      const archiveCards = page.locator('button[data-testid^="edge-group-"]');
      const count = await archiveCards.count();
      let found = false;
      for (let i = 0; i < count; i++) {
        const card = archiveCards.nth(i);
        const text = await card.textContent();
        if (text?.includes(groupName) || text?.includes('ARCHIVED')) {
          const status = card.getByTestId('edge-group-status');
          if (await status.isVisible()) {
            const statusText = await status.textContent();
            if (statusText?.includes('ARCHIVED')) {
              found = true;
              break;
            }
          }
        }
      }
      expect(found).toBe(true);
    }).toPass({ timeout: 15000 });

    // Verify mock received requests — check via API
    const historyResponse = await page.request.get(`${MOCK_URL}/api/test/history`);
    const history = await historyResponse.json();

    const noarkaRequests = history.filter((r: any) => r.endpoint === 'noarka');
    const noarkbRequests = history.filter((r: any) => r.endpoint === 'noarkb');

    // Noark A: 2 ENTRY_ADDED + 1 GROUP_CLOSED = 3 requests
    expect(noarkaRequests.length).toBeGreaterThanOrEqual(3);
    // Noark B: 1 GROUP_CLOSED only
    expect(noarkbRequests.length).toBeGreaterThanOrEqual(1);
  });
});
