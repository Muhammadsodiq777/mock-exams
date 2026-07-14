import { NavLink } from 'react-router-dom'

import type { NavigationItem } from '../../constants/navigation'

type NavListProps = {
  ariaLabel: string
  items: NavigationItem[]
}

export function NavList({ ariaLabel, items }: NavListProps) {
  if (items.length === 0) {
    return null
  }

  return (
    <nav aria-label={ariaLabel}>
      <ul className="nav-list">
        {items.map((item) => (
          <li key={item.to}>
            <NavLink
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              to={item.to}
            >
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
