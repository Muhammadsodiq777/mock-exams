import { expect, test } from '@playwright/test'

const catalogFixture = {
  data: [
    {
      id: 91001,
      title: 'E2E Reading Smoke Test',
      description: 'Deterministic API fixture rendered by the e2e harness.',
      type: 'reading',
      difficulty: 'intermediate',
      estimatedBandMin: 5.5,
      estimatedBandMax: 7,
      durationMinutes: 60,
      priceAmount: 0,
      currency: 'UZS',
      free: true,
      language: 'en',
      publishedAt: '2026-07-14T09:00:00Z',
      teacher: null,
      categories: [{ id: 1201, name: 'Reading', slug: 'reading' }],
      tags: [
        { id: 2201, name: 'Skimming', slug: 'skimming' },
        { id: 2202, name: 'Band 6+', slug: 'band-6-plus' },
      ],
    },
  ],
  meta: {
    requestId: 'e2e-public-catalog-smoke',
    page: {
      number: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    },
    sort: [{ field: 'publishedAt', direction: 'desc' }],
    filters: {},
  },
}

test.describe('public catalog smoke', () => {
  test.beforeEach(async ({ context, page }) => {
    await context.clearCookies()
    await page.addInitScript(() => {
      window.localStorage.clear()
      window.sessionStorage.clear()
    })
    await page.route('**/api/v1/public/tests**', async (route) => {
      await route.fulfill({
        body: JSON.stringify(catalogFixture),
        contentType: 'application/json',
        status: 200,
      })
    })
  })

  test('boots the Vite app and renders mocked public test data', async ({ page }) => {
    await page.goto('/tests')

    await expect(page.getByRole('heading', { name: 'IELTS practice tests' })).toBeVisible()
    await expect(page.getByLabel('Type')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'E2E Reading Smoke Test' })).toBeVisible()
    await expect(
      page.getByText('Deterministic API fixture rendered by the e2e harness.'),
    ).toBeVisible()
    await expect(page.getByRole('link', { name: 'Open reading test' })).toHaveAttribute(
      'href',
      '/tests/91001',
    )
  })
})
