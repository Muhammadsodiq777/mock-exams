import { useLocation } from 'react-router-dom'

import { Badge, DescriptionList, PageContainer, Panel } from '../../components/ui'
import { useSessionStore } from '../../store/sessionStore'

type TeacherProfileLocationState = {
  needsTeacherApproval?: boolean
}

export function TeacherProfilePage() {
  const session = useSessionStore()
  const location = useLocation()
  const state = location.state as TeacherProfileLocationState | null
  const teacherStatus =
    session.status === 'authenticated'
      ? session.user.teacherProfile?.status ?? 'draft'
      : 'draft'

  return (
    <PageContainer size="compact">
      <Panel className="placeholder-panel">
        <p className="ui-eyebrow">Teacher profile</p>
        <h2>Approval status</h2>
        <p>
          {state?.needsTeacherApproval
            ? 'Your teacher profile must be approved before the dashboard, builder, schedule, earnings, and student pages are available.'
            : 'Teacher profile actions and moderation notes will appear here.'}
        </p>
        <DescriptionList
          items={[
            {
              label: 'Current status',
              value: <Badge tone="info">{teacherStatus.replace('_', ' ')}</Badge>,
            },
          ]}
        />
      </Panel>
    </PageContainer>
  )
}
