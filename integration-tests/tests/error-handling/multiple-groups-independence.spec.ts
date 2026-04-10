import { test, expect, MOCK_URL, EDGE_URL } from '../base-test';

test.describe('Error Handling — Multiple Groups Independence', () => {
  test('failure in group A does not affect group B — independent archiving', async ({ page, request }) => {
    test.setTimeout(90000);

    const ts = Date.now();
    const groupAName = `Group A Fail ${ts}`;
    const groupBName = `Group B Pass ${ts}`;

    // Configure noark-a to fail
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });

    // --- Create group A (will fail on archiving) ---
    await page.getByTestId('group-name-input').fill(groupAName);
    await page.getByTestId('create-group-button').click();
    const groupAButton = page.getByRole('button', { name: new RegExp(groupAName) });
    await expect(groupAButton).toBeVisible();
    await groupAButton.click();

    await page.getByTestId('entry-content-input').fill('Entry for A');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Entry for A' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for group A to reach PENDING in Edge
    await expect(async () => {
      const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
      const edgeGroups = await edgeGroupsResponse.json();
      const edgeGroupA = edgeGroups.find((g: any) => g.name === groupAName);
      expect(edgeGroupA).toBeDefined();
      expect(edgeGroupA.status).toBe('PENDING');
    }).toPass({ timeout: 30000 });

    // Reset mock — now everything works
    await request.post(`${MOCK_URL}/api/test/reset`);

    // --- Create group B (should succeed) ---
    await page.getByTestId('group-name-input').fill(groupBName);
    await page.getByTestId('create-group-button').click();
    const groupBButton = page.getByRole('button', { name: new RegExp(groupBName) });
    await expect(groupBButton).toBeVisible();
    await groupBButton.click();

    await page.getByTestId('entry-content-input').fill('Entry for B');
    await page.getByTestId('add-entry-button').click();
    await expect(page.getByTestId('entry-content').filter({ hasText: 'Entry for B' })).toBeVisible();

    await page.getByTestId('close-group-button').click();
    await expect(page.getByTestId('group-detail-status')).toHaveText('CLOSED');

    // Wait for group B to reach ARCHIVED
    await expect(async () => {
      const edgeGroupsResponse = await request.get(`${EDGE_URL}/api/groups`);
      const edgeGroups = await edgeGroupsResponse.json();
      const edgeGroupB = edgeGroups.find((g: any) => g.name === groupBName);
      expect(edgeGroupB).toBeDefined();
      expect(edgeGroupB.status).toBe('ARCHIVED');
    }).toPass({ timeout: 30000 });

    // Confirm group A is STILL PENDING — not affected by group B's success
    const finalResponse = await request.get(`${EDGE_URL}/api/groups`);
    const finalGroups = await finalResponse.json();
    const finalGroupA = finalGroups.find((g: any) => g.name === groupAName);
    expect(finalGroupA.status).toBe('PENDING');
  });
});
