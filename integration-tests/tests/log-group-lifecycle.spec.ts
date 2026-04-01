import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Log Group Lifecycle', () => {
  test('create group, add 3 entries, close group, verify RabbitMQ messages and UI state', async ({ page }) => {
    const groupName = `test-group-${Date.now()}`;

    // Navigate to app
    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Wait for RabbitMQ panel to connect
    await expect(page.getByTestId('rabbitmq-status')).toHaveAttribute('aria-label', 'Connected', { timeout: 15000 });

    // Read initial message count from the UI
    const initialCountText = await page.getByTestId('rabbitmq-messages').textContent();
    const initialMessageCount = parseInt(initialCountText!.trim(), 10);

    // --- Create a new group ---
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();

    // Wait for group to appear in the list and click it
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();

    // Verify it shows OPEN in the groups list
    await expect(groupButton.getByTestId('group-item-status')).toHaveText('OPEN');

    // Select the group
    await groupButton.click();

    // Verify group detail shows the name and OPEN status
    await expect(page.getByTestId('group-detail-name')).toHaveText(groupName);
    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');

    // --- Add 3 log entries ---
    const entries = ['First log entry', 'Second log entry', 'Third log entry'];
    for (const entry of entries) {
      await page.getByTestId('entry-content-input').fill(entry);
      await page.getByTestId('add-entry-button').click();
      await expect(page.getByTestId('entry-content').filter({ hasText: entry })).toBeVisible();
    }

    // Verify all 3 entries are visible
    await expect(page.getByTestId('entry-item')).toHaveCount(3);

    // --- Close the group ---
    await page.getByTestId('close-group-button').click();

    // Verify group status changed to CLOSED in detail view
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Verify group shows CLOSED in the groups list
    await expect(groupButton.getByTestId('group-item-status')).toHaveText('CLOSED');

    // Verify "Add Entry" input and "Close Group" button are gone
    await expect(page.getByTestId('entry-content-input')).not.toBeVisible();
    await expect(page.getByTestId('close-group-button')).not.toBeVisible();

    // --- Verify RabbitMQ message count in the UI ---
    // Creating group = 1 msg, adding 3 entries = 3 msgs, closing group = 1 msg => 5 new messages
    const expectedCount = (initialMessageCount + 5).toString();
    await expect(page.getByTestId('rabbitmq-messages')).toHaveText(expectedCount, { timeout: 10000 });
  });
});
