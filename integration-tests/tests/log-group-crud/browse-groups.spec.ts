import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

test.describe('Log Group CRUD — Browse Groups', () => {
  test('browse all groups and switch between them', async ({ page }) => {
    const suffix = Date.now();
    const groupOneName = `Group One ${suffix}`;
    const groupTwoName = `Group Two ${suffix}`;

    await page.goto(BASE_URL);
    await expect(page.getByRole('heading', { name: 'Log Manager' })).toBeVisible();

    // Create two groups
    await page.getByTestId('group-name-input').fill(groupOneName);
    await page.getByTestId('create-group-button').click();
    const groupOneButton = page.getByRole('button', { name: new RegExp(groupOneName) });
    await expect(groupOneButton).toBeVisible();

    await page.getByTestId('group-name-input').fill(groupTwoName);
    await page.getByTestId('create-group-button').click();
    const groupTwoButton = page.getByRole('button', { name: new RegExp(groupTwoName) });
    await expect(groupTwoButton).toBeVisible();

    // Select Group One
    await groupOneButton.click();
    await expect(page.getByTestId('group-detail-name')).toHaveText(groupOneName);
    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');

    // Switch to Group Two
    await groupTwoButton.click();
    await expect(page.getByTestId('group-detail-name')).toHaveText(groupTwoName);
    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');
  });
});
