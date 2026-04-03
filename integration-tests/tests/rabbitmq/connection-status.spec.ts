import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('RabbitMQ Integration — Connection Status', () => {
  test('RabbitMQ panel shows Connected status on page load', async ({ page }) => {
    await page.goto(BASE_URL);

    // RabbitMQ panel should be visible
    await expect(page.getByTestId('rabbitmq-feed')).toBeVisible();

    // Wait for connection status to show Connected
    await expect(page.getByTestId('rabbitmq-status')).toHaveAttribute('aria-label', 'Connected', { timeout: 15000 });
  });
});
