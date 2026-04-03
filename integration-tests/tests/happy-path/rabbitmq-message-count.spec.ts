import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Happy Path E2E — RabbitMQ Message Count', () => {
  test('verify RabbitMQ message count increments correctly for all mutations', async ({ page }) => {
    const groupName = `MQ Count Group ${Date.now()}`;

    await page.goto(BASE_URL);
    await expect(page.getByTestId('rabbitmq-status')).toHaveAttribute('aria-label', 'Connected', { timeout: 15000 });

    // Record initial count
    const initialCountText = await page.getByTestId('rabbitmq-message-count').textContent();
    const initialCount = parseInt(initialCountText!.replace(/[^0-9]/g, ''), 10) || 0;

    // Create group (+1)
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();

    // Select group
    await groupButton.click();

    // Add entry 1 (+1)
    await page.getByTestId('entry-content-input').fill('Entry One');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Entry One' })).toBeVisible();

    // Add entry 2 (+1)
    await page.getByTestId('entry-content-input').fill('Entry Two');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Entry Two' })).toBeVisible();

    // Close group (+1)
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Total: 4 new messages (1 create + 2 entries + 1 close)
    const expectedCount = initialCount + 4;
    await expect(page.getByTestId('rabbitmq-message-count')).toContainText(expectedCount.toString(), { timeout: 10000 });
  });
});
