import { Outlet } from 'react-router-dom'

import {
  appNavigationItems,
  getVisibleNavigationItems,
} from '../../constants/navigation'
import { useLogout } from '../../hooks/useAuthSession'
import { useSessionStore } from '../../store/sessionStore'
import { NavList } from './NavList'

export function AppLayout() {
  const session = useSessionStore()
  const navItems = getVisibleNavigationItems(appNavigationItems, session)
  const logout = useLogout()

  return (
    <div className="site-shell">
      <header className="shell-header app-header">
        <div>
          <p className="eyebrow">Mock Exams</p>
          <p className="user-label">
            {session.status === 'authenticated' ? session.user.fullName : 'Signed out'}
          </p>
        </div>
        <NavList ariaLabel="App" items={navItems} />
        {session.status === 'authenticated' && (
          <button className="logout-button" onClick={logout} type="button">
            Log out
          </button>
        )}
      </header>
      <Outlet />
    </div>
  )
}
