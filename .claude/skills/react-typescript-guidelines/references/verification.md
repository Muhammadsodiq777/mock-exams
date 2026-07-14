# Verification

## Commands

- `npm run lint`
- `npm run build`
- `npm run e2e` for browser-level flow changes

## Expectations

- Lint and build should pass before finishing frontend work
- If a change touches auth, navigation, or forms, consider whether E2E coverage is warranted
- If E2E is not run, say so explicitly

## UX States

Check these before closing a frontend task:

- Loading state
- Error state
- Empty state where relevant
- Mutation pending/disabled behavior
- Mobile layout sanity for the touched screen
