import { Outlet } from 'react-router-dom'

import { adminNavigationItems } from '../../constants/navigation'
import { NavList } from './NavList'

export function AdminLayout() {
  return (
    <section className="role-layout" aria-labelledby="admin-layout-title">
      <div className="role-layout-header">
        <div>
          <p className="eyebrow">Admin</p>
          <h1 id="admin-layout-title">Admin workspace</h1>
        </div>
        <NavList ariaLabel="Admin" items={adminNavigationItems} />
      </div>
      <Outlet />
    </section>
  )
}
