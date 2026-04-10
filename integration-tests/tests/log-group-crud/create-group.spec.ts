import { test, expect } from '../base-test';

test.describe('Log Group CRUD — Create Group', () => {
  test('create a new log group with a valid name', async ({ page }) => {
    const groupName = `My First Group ${Date.now()}`;

    // Input should be empty, Create button disabled
    await expect(page.getByTestId('group-name-input')).toHaveValue('');
    await expect(page.getByTestId('create-group-button')).toBeDisabled();

    // Type a group name — button should enable
    await page.getByTestId('group-name-input').fill(groupName);
    await expect(page.getByTestId('create-group-button')).toBeEnabled();

    // Create the group
    await page.getByTestId('create-group-button').click();

    // Group appears in list with OPEN status
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await expect(groupButton.getByTestId('group-item-status')).toHaveText('OPEN');

    // Input is cleared, button disabled again
    await expect(page.getByTestId('group-name-input')).toHaveValue('');
    await expect(page.getByTestId('create-group-button')).toBeDisabled();

    // Select the group and verify detail panel
    await groupButton.click();
    await expect(page.getByTestId('group-detail-name')).toHaveText(groupName);
    await expect(page.getByTestId('group-detail-status')).toHaveText('OPEN');
    await expect(page.getByTestId('entry-content-input')).toBeVisible();
    await expect(page.getByTestId('close-group-button')).toBeVisible();
    await expect(page.getByTestId('entry-item')).toHaveCount(0);
  });
});
