import { test, expect, MOCK_URL, EDGE_URL } from '../base-test';

test.describe('Retry Mechanism — Both Adapters Fail Then Recover', () => {
  test('both adapters fail → PENDING, mock reset to 200, retry → ARCHIVED', async ({ page, request }) => {
    test.setTimeout(90000);

    // Configure both adapters to fail
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarkb', statusCode: 500 },
    });

    const groupName = `Both Recover ${Date.now()}`;

    // Create group, add entry, close
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Will eventually succeed');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Will eventually succeed' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for PENDING status via Edge API
    await expect(async () => {
      const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
      const edgeGroups = await edgeGroupsResponse.json();
      const edgeGroup = edgeGroups.find((g: any) => g.name === groupName);
      expect(edgeGroup).toBeDefined();
      expect(edgeGroup.status).toBe('PENDING');
    }).toPass({ timeout: 30000 });

    // Reset mock to default (200 OK)
    await request.post(`${MOCK_URL}/api/test/reset`);

    // Trigger retry — should now succeed
    await request.post(`${EDGE_URL}/api/retry`);

    // Wait for ARCHIVED status via Edge API
    await expect(async () => {
      const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
      const edgeGroups = await edgeGroupsResponse.json();
      const edgeGroup = edgeGroups.find((g: any) => g.name === groupName);
      expect(edgeGroup.status).toBe('ARCHIVED');
    }).toPass({ timeout: 30000 });

    // Verify ARCHIVED is reflected in the UI
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
    }).toPass({ timeout: 15000 });
  });
});
