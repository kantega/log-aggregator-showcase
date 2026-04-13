import { test, expect, MOCK_URL, EDGE_URL } from '../base-test';

test.describe('Retry Mechanism — Exponential Backoff', () => {
  test('noark-a 500 on GROUP_CLOSED, reset to 200, scheduler retries after backoff → ARCHIVED', async ({
    page,
    request,
  }) => {
    test.setTimeout(60000);

    // Configure noark-a to fail persistently so GROUP_CLOSED is guaranteed to fail.
    // (The "Fail next only" queue is consumed by the earliest event — GROUP_CREATED —
    // which does not increment retryCount, so it can't exercise the backoff path.)
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });

    const groupName = `Backoff Group ${Date.now()}`;

    // Create group, add entry, close
    await page.getByTestId('group-name-input').fill(groupName);
    await page.getByTestId('create-group-button').click();
    const groupButton = page.getByRole('button', { name: new RegExp(groupName) });
    await expect(groupButton).toBeVisible();
    await groupButton.click();

    await page.getByTestId('entry-content-input').fill('Backoff entry');
    await page.getByTestId('add-entry-button').click();
    await expect(
      page.getByTestId('entry-content').filter({ hasText: 'Backoff entry' }),
    ).toBeVisible();

    await page.getByTestId('close-group-button').click();

    // Wait until the initial GROUP_CLOSED attempt has failed and Edge is in PENDING with retryCount ≥ 1
    // (this is the moment the scheduler is armed to retry after the 3s backoff window).
    await expect(async () => {
      const groupsResp = await request.get(`${EDGE_URL}/api/groups`);
      const groups: Array<{ name: string; retryCount: number; status: string }> =
        await groupsResp.json();
      const ourGroup = groups.find((g) => g.name === groupName);
      expect(ourGroup).toBeDefined();
      expect(ourGroup!.status).toBe('PENDING');
      expect(ourGroup!.retryCount).toBeGreaterThanOrEqual(1);
    }).toPass({ timeout: 20000 });

    // Flip the mock to 200 — the scheduler's next retry (after the exponential backoff window) should succeed.
    await request.post(`${MOCK_URL}/api/test/reset`);

    // Eventually the group must reach ARCHIVED via the auto-retry scheduler (no manual /api/retry call).
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

    // Verify Edge state: status ARCHIVED and at least one retry happened
    const groupsResp = await request.get(`${EDGE_URL}/api/groups`);
    const groups: Array<{ name: string; retryCount: number; status: string }> =
      await groupsResp.json();
    const ourGroup = groups.find((g) => g.name === groupName);
    expect(ourGroup).toBeDefined();
    expect(ourGroup!.status).toBe('ARCHIVED');
    expect(ourGroup!.retryCount).toBeGreaterThanOrEqual(1);
  });
});
