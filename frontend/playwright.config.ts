import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests drive a real browser against the running SPA, which in turn
 * calls the real Spring backend (so the BACKEND must be up on :8080 with its
 * Postgres). Playwright starts/reuses the Vite dev server on :5173 for us.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  // Each web-step involves a real network round-trip to the backend; give
  // assertions room beyond the 5s default.
  expect: { timeout: 15_000 },
  fullyParallel: false,
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  // Use the system Edge (always present on Windows) so no Chromium download is
  // needed. Switch to devices['Desktop Chrome'] if you run `playwright install`.
  projects: [{ name: 'edge', use: { ...devices['Desktop Edge'], channel: 'msedge' } }],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
