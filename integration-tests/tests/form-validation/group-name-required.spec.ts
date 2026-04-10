import { test, expect } from '../base-test';

test.describe('Form Validation — Group Name Required', () => {
  test('create button remains disabled until group name is non-empty', async ({ page }) => {

    const input = page.getByTestId('group-name-input');
    const button = page.getByTestId('create-group-button');

    // Initially empty and disabled
    await expect(input).toHaveValue('');
    await expect(button).toBeDisabled();

    // Type a character — enabled
    await input.fill('A');
    await expect(button).toBeEnabled();

    // Clear — disabled again
    await input.fill('');
    await expect(button).toBeDisabled();

    // Type a longer name — enabled
    await input.fill('Valid Group Name 123');
    await expect(button).toBeEnabled();
  });
});
