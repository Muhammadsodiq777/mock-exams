import { Outlet } from 'react-router-dom'

import {
  getVisibleNavigationItems,
  teacherNavigationItems,
} from '../../constants/navigation'
import { useSessionStore } from '../../store/sessionStore'
import { NavList } from './NavList'

export function TeacherLayout() {
  const session = useSessionStore()

  return (
    <section className="role-layout" aria-labelledby="teacher-layout-title">
      <div className="role-layout-header">
        <div>
          <p className="eyebrow">Teacher</p>
          <h1 id="teacher-layout-title">Teacher workspace</h1>
        </div>
        <NavList
          ariaLabel="Teacher"
          items={getVisibleNavigationItems(teacherNavigationItems, session)}
        />
      </div>
      <Outlet />
    </section>
  )
}
