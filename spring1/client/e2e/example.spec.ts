import { expect, test } from '@playwright/test';

test('has title', async ({ page }) => {
  await page.goto('/');

  // Expect title to contain PokéDex Client
  await expect(page).toHaveTitle(/PokéDex Client/);
});

test('renders header toolbar and branding', async ({ page }) => {
  await page.goto('/');

  await expect(page.locator('.app-title')).toContainText('PokéDex Explorer');
  await expect(page.locator('.sync-btn')).toBeVisible();
});
