import { Link, Outlet } from 'react-router-dom'

import {
  getDefaultAuthenticatedRoute,
  getVisibleNavigationItems,
  publicNavigationItems,
} from '../../constants/navigation'
import { ROUTES } from '../../constants/routes'
import { useSessionStore } from '../../store/sessionStore'
import { NavList } from './NavList'

export function PublicLayout() {
  const session = useSessionStore()
  const accountHref =
    session.status === 'authenticated' ? getDefaultAuthenticatedRoute(session) : ROUTES.login
  const accountLabel =
    session.status === 'loading'
      ? 'Account'
      : session.status === 'authenticated'
        ? 'Dashboard'
        : 'Log in'

  return (
    <div className="site-shell">
      <header className="shell-header">
        <Link className="brand-link" to={ROUTES.tests}>
          Mock Exams
        </Link>
        <NavList
          ariaLabel="Public"
          items={getVisibleNavigationItems(publicNavigationItems, session)}
        />
        <Link className="account-link" to={accountHref}>
          {accountLabel}
        </Link>
      </header>
      <Outlet />
    </div>
  )
}
