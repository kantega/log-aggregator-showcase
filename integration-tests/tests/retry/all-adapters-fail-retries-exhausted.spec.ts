import { test, expect, MOCK_URL, EDGE_URL } from '../base-test';

test.describe('Retry Mechanism — Both Adapters Fail, Retries Exhausted', () => {
  test('both adapters return 500, manual retries step retryCount 1→2→3 then FAILED', async ({ page, request }) => {
    test.setTimeout(90000);

    // Configure BOTH adapters to fail
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarkb', statusCode: 500 },
    });

    const groupName = `Both Fail ${Date.now()}`;

    // Create group, add entry, close
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Will fail');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Will fail' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // The auto-retry scheduler (3s interval) will exhaust retries automatically.
    // Wait for the group to reach FAILED with retryCount=3 (MAX_RETRIES).
    await expect(async () => {
      const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
      const edgeGroups = await edgeGroupsResponse.json();
      const edgeGroup = edgeGroups.find((g: any) => g.name === groupName);
      expect(edgeGroup).toBeDefined();
      expect(edgeGroup.status).toBe('FAILED');
      expect(edgeGroup.retryCount).toBe(3);
    }).toPass({ timeout: 45000 });

    // Verify errors exist for both adapters
    const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
    const edgeGroups = await edgeGroupsResponse.json();
    const edgeGroup = edgeGroups.find((g: any) => g.name === groupName);
    const adapterNames = edgeGroup.errors.map((e: any) => e.adapter);
    expect(adapterNames).toContain('adapter-a');
    expect(adapterNames).toContain('adapter-b');

    // Verify FAILED status is reflected in the UI
    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
      const count = await edgeCards.count();
      let foundFailed = false;
      for (let i = 0; i < count; i++) {
        const statusEl = edgeCards.nth(i).getByTestId('edge-group-status');
        if (await statusEl.isVisible()) {
          const text = await statusEl.textContent();
          if (text?.includes('FAILED')) {
            foundFailed = true;
            break;
          }
        }
      }
      expect(foundFailed).toBe(true);
    }).toPass({ timeout: 15000 });
  });
});
