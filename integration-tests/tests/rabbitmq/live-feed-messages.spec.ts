import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('RabbitMQ Integration — Live Feed Messages', () => {
  test('live feed shows message details on every mutation', async ({ page }) => {
    const groupName = `Feed Test Group ${Date.now()}`;

    await page.goto(BASE_URL);
    await expect(page.getByTestId('rabbitmq-status')).toHaveAttribute('aria-label', 'Connected', { timeout: 15000 });

    const feed = page.getByTestId('rabbitmq-feed');

    // Create a group — should see GROUP_CREATED in feed
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await expect(feed).toContainText('GROUP_CREATED', { timeout: 10000 });

    // Select group and add entry — should see ENTRY_ADDED
    await groupButton.click();
    await page.getByTestId('entry-content-input').fill('Feed entry one');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Feed entry one' })).toBeVisible();
    await expect(feed).toContainText('ENTRY_ADDED', { timeout: 10000 });

    // Close group — should see GROUP_CLOSED
    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');
    await expect(feed).toContainText('GROUP_CLOSED', { timeout: 10000 });
  });
});
