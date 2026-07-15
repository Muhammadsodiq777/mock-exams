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
      <Panel
        title="Approval status"
        description="Teacher profile status controls access to publishing, schedules, earnings, and students."
      >
        <div className="feature-panel-body">
        <p>
          {state?.needsTeacherApproval
            ? 'Your teacher profile must be approved before the dashboard, builder, schedule, earnings, and student pages are available.'
            : 'Use this status summary to confirm whether your workspace is open or waiting for review.'}
        </p>
        <DescriptionList
          items={[
            {
              label: 'Current status',
              value: <Badge tone="info">{teacherStatus.replace('_', ' ')}</Badge>,
            },
          ]}
        />
        </div>
      </Panel>
    </PageContainer>
  )
}
