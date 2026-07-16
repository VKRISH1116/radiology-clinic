import { expect, test } from '@playwright/test';

// Full-stack happy path: a real browser -> the SPA -> the Spring backend ->
// Postgres. Uses a fresh unique patient each run so it's repeatable.
test('patient can register, log in, book a scan and cancel it', async ({ page }) => {
  const email = `e2e.${Date.now()}@clinic.test`;
  const password = 'password123';

  // --- register ---
  await page.goto('/register');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByLabel('Confirm password').fill(password);
  await page.getByRole('checkbox').check(); // consent notice (AC-F3-1)
  await page.getByRole('button', { name: /create account/i }).click();

  // --- log in ---
  await expect(page).toHaveURL(/\/login/);
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();

  // --- patient dashboard (real data) ---
  await expect(page.getByRole('heading', { name: /welcome back/i })).toBeVisible();
  // The email shows in both the top bar and the greeting — assert at least one.
  await expect(page.getByText(email).first()).toBeVisible();

  // --- book a scan ---
  await page.getByRole('button', { name: /book a scan/i }).click();
  await expect(page.getByRole('heading', { name: /book a scan/i })).toBeVisible();

  // Pick the first study, then the first bookable (enabled) slot.
  await page.getByRole('checkbox').first().check();
  await page
    .locator('button:not([disabled])')
    .filter({ hasText: /^\d{2}:\d{2}$/ })
    .first()
    .click();
  await page.getByRole('button', { name: /confirm booking/i }).click();

  // --- verify it persisted and shows on the dashboard ---
  await expect(page.getByText(/appointment booked/i)).toBeVisible();
  await expect(page.getByText('BOOKED', { exact: true })).toBeVisible();

  // --- cancel it ---
  await page.getByRole('button', { name: /^cancel$/i }).first().click();
  await expect(page.getByText('CANCELLED', { exact: true })).toBeVisible();
});
