/* global process */
import { defineConfig, devices } from '@playwright/test'

const frontendBaseUrl = process.env.E2E_FRONTEND_URL ?? 'http://127.0.0.1:5173'
const shouldStartFrontend = process.env.E2E_START_FRONTEND !== 'false'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: 0,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
  ],
  outputDir: 'test-results',
  use: {
    baseURL: frontendBaseUrl,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: shouldStartFrontend
    ? {
        command: 'npm run dev -- --host 127.0.0.1',
        reuseExistingServer: true,
        timeout: 120_000,
        url: frontendBaseUrl,
      }
    : undefined,
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
