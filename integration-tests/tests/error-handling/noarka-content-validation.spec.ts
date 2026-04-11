import { test, expect, MOCK_URL, EDGE_URL } from '../base-test';

test.describe('Error Handling — Noark A Content Validation', () => {
  test('entry with forbidden "error" text is rejected but GROUP_CLOSED still succeeds', async ({ page, request }) => {
    test.setTimeout(60000);

    const groupName = `Validation ${Date.now()}`;

    // Create group
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    // Add entry with forbidden content ("error" in text)
    await page.getByTestId('entry-content-input').fill('This has an error in it');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'This has an error in it' })).toBeVisible();

    // Add a clean entry
    await page.getByTestId('entry-content-input').fill('This is perfectly fine');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'This is perfectly fine' })).toBeVisible();

    // Wait for Edge to record the validation error from the forbidden ENTRY_ADDED
    await expect(async () => {
      const response = await request.get(`${EDGE_URL}/api/groups`);
      const groups = await response.json();
      const group = groups.find((g: any) => g.name === groupName);
      expect(group).toBeTruthy();
      expect(group.errors.length).toBeGreaterThan(0);
    }).toPass({ timeout: 30000 });

    // Close the group — GROUP_CLOSED must not be affected by content validation
    await page.getByTestId('close-group-button').click();

    // Group should reach ARCHIVED because GROUP_CLOSED succeeds
    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      let foundArchived = false;
      for (let i = 0; i < count; i++) {
        const statusEl = edgeCards.nth(i).getByTestId('edge-group-status');
        if (await statusEl.isVisible()) {
          const text = await statusEl.textContent();
          if (text?.includes('ARCHIVED')) {
            foundArchived = true;
            break;
          }
        }
      }
      expect(foundArchived).toBe(true);
    }).toPass({ timeout: 30000 });

    // Verify via API that errors were recorded despite ARCHIVED status
    const response = await request.get(`${EDGE_URL}/api/groups`);
    const groups = await response.json();
    const group = groups.find((g: any) => g.name === groupName);
    expect(group.status).toBe('ARCHIVED');
    expect(group.errors.length).toBeGreaterThan(0);

    // Verify mock history: forbidden ENTRY_ADDED was still sent (and rejected)
    const historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    const history = await historyResponse.json();
    const noarkaRequests = history.filter((h: any) => h.endpoint === 'noarka');
    // 2 ENTRY_ADDED + 1 GROUP_CLOSED = 3 requests
    expect(noarkaRequests.length).toBe(3);
  });
});
