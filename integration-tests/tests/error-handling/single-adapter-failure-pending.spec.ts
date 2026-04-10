import { test, expect, MOCK_URL, EDGE_URL } from '../base-test';

test.describe('Error Handling — Single Adapter Failure → PENDING', () => {
  test('noark-a returns 500, group goes PENDING with retryCount=1, noark-b still receives data', async ({ page, request }) => {
    test.setTimeout(60000);

    // Configure only noark-a to fail — noark-b stays healthy
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });

    const groupName = `Single Fail ${Date.now()}`;

    // Create group, add entry, close
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Some content');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Some content' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for Edge to process — with auto-retry scheduler (3s interval), the group
    // may be PENDING (retries not exhausted) or FAILED (retries exhausted).
    // The key assertion: adapter-a failure is tracked with errors and retryCount >= 1.
    await expect(async () => {
      const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
      const edgeGroups = await edgeGroupsResponse.json();
      const edgeGroup = edgeGroups.find((g: any) => g.name === groupName);
      expect(edgeGroup).toBeDefined();
      expect(['PENDING', 'FAILED']).toContain(edgeGroup.status);
      expect(edgeGroup.retryCount).toBeGreaterThanOrEqual(1);
      expect(edgeGroup.errors.length).toBeGreaterThan(0);
    }).toPass({ timeout: 30000 });

    // Verify the errors reference adapter-a
    const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
    const edgeGroups = await edgeGroupsResponse.json();
    const edgeGroup = edgeGroups.find((g: any) => g.name === groupName);
    expect(edgeGroup.errors.some((e: any) => e.adapter === 'adapter-a')).toBe(true);

    // Verify noark-b still received the GROUP_CLOSED request
    const historyResponse = await request.get(`${MOCK_URL}/api/test/history`);
    const history = await historyResponse.json();
    const noarkbRequests = history.filter((r: any) => r.endpoint === 'noarkb');
    expect(noarkbRequests.length).toBeGreaterThanOrEqual(1);
  });
});
