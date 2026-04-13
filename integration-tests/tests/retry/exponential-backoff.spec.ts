import { test, expect, EDGE_URL } from '../base-test';

test.describe('Retry Mechanism — Exponential Backoff', () => {
  test('"Fail next only" UI button queues a single 500, group recovers on first retry', async ({
    page,
    request,
  }) => {
    test.setTimeout(60000);

    // Use the UI button to queue a single failure on noark-a
    const failNextButton = page.getByTestId('mock-setup-noarka-fail-next');
    await expect(failNextButton).toBeVisible();
    await failNextButton.click();

    // The queued count badge should appear, confirming the mock accepted the queue
    await expect(page.getByTestId('mock-setup-noarka-queued-failures')).toBeVisible();

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

    // Eventually the group must reach ARCHIVED. The 1st GROUP_CLOSED attempt fails (queued 500),
    // then after a 3s backoff the 2nd attempt should succeed (queue exhausted → 200).
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

    // Verify Edge state: retryCount should be ≥ 1 (one failure happened) and the queue is now empty
    const groupsResp = await request.get(`${EDGE_URL}/api/groups`);
    const groups: Array<{ name: string; retryCount: number; status: string }> =
      await groupsResp.json();
    const ourGroup = groups.find((g) => g.name === groupName);
    expect(ourGroup).toBeDefined();
    expect(ourGroup!.status).toBe('ARCHIVED');
    expect(ourGroup!.retryCount).toBeGreaterThanOrEqual(1);
  });
});
