import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import { AdminLayout } from '../components/layout/AdminLayout'
import { AppLayout } from '../components/layout/AppLayout'
import { PublicLayout } from '../components/layout/PublicLayout'
import { TeacherLayout } from '../components/layout/TeacherLayout'
import {
  RequireApprovedTeacher,
  RequireAuth,
  RequireRole,
} from '../components/routing/RouteGuards'
import { SessionBootstrap } from '../components/routing/SessionBootstrap'
import { NotFoundState } from '../components/routing/ShellState'
import { ROUTES } from '../constants/routes'
import { AdminDashboardPage } from '../pages/admin/AdminDashboardPage'
import { AdminModerationPage } from '../pages/admin/AdminModerationPage'
import { AdminPaymentsPage } from '../pages/admin/AdminPaymentsPage'
import { AdminReportsPage } from '../pages/admin/AdminReportsPage'
import { AdminSettingsPage } from '../pages/admin/AdminSettingsPage'
import { AdminTeachersPage } from '../pages/admin/AdminTeachersPage'
import { AdminUsersPage } from '../pages/admin/AdminUsersPage'
import { AdminWithdrawalsPage } from '../pages/admin/AdminWithdrawalsPage'
import { LoginPage, RegisterPage } from '../pages/auth/AuthPages'
import { PlaceholderPage } from '../pages/PlaceholderPage'
import { MarketplaceDetailPage } from '../pages/public/MarketplaceDetailPage'
import { MarketplacePage } from '../pages/public/MarketplacePage'
import { PricingPage } from '../pages/public/PricingPage'
import { TestDetailPage } from '../pages/public/TestDetailPage'
import { TestsPage } from '../pages/public/TestsPage'
import { DashboardPage } from '../pages/student/DashboardPage'
import { ProfilePage } from '../pages/student/ProfilePage'
import { ResultDetailPage } from '../pages/student/ResultDetailPage'
import { SpeakingPracticePage } from '../pages/student/SpeakingPracticePage'
import { WritingPracticePage } from '../pages/student/WritingPracticePage'
import { TeacherDashboardPage } from '../pages/teacher/TeacherDashboardPage'
import { TeacherEarningsPage } from '../pages/teacher/TeacherEarningsPage'
import { TeacherSchedulePage } from '../pages/teacher/TeacherSchedulePage'
import { TeacherStudentsPage } from '../pages/teacher/TeacherStudentsPage'
import { TeacherTestBuilderPage } from '../pages/teacher/TeacherTestBuilderPage'
import { TeacherTestsPage } from '../pages/teacher/TeacherTestsPage'
import { TeacherProfilePage } from '../pages/teacher/TeacherProfilePage'
import { TeacherNotificationsPage } from '../pages/teacher/TeacherNotificationsPage'

export function AppRouter() {
  return (
    <BrowserRouter>
      <SessionBootstrap>
        <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<Navigate replace to={ROUTES.tests} />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="tests" element={<TestsPage />} />
          <Route path="tests/:id" element={<TestDetailPage />} />
          <Route path="marketplace" element={<MarketplacePage />} />
          <Route path="marketplace/:id" element={<MarketplaceDetailPage />} />
          <Route
            path="teachers/:id"
            element={
              <PlaceholderPage
                eyebrow="Teacher"
                title="Teacher profile"
                summary="Public teacher profile details will mount here."
              />
            }
          />
          <Route path="pricing" element={<PricingPage />} />
        </Route>

        <Route element={<RequireAuth />}>
          <Route element={<AppLayout />}>
            <Route element={<RequireRole allowedRoles={['student']} />}>
              <Route
                path="dashboard"
                element={<DashboardPage />}
              />
              <Route
                path="results/:id"
                element={<ResultDetailPage />}
              />
              <Route
                path="writing"
                element={<WritingPracticePage />}
              />
              <Route
                path="speaking"
                element={<SpeakingPracticePage />}
              />
              <Route path="profile" element={<ProfilePage />} />
            </Route>

            <Route path="teacher" element={<RequireRole allowedRoles={['teacher']} />}>
              <Route element={<TeacherLayout />}>
                <Route index element={<Navigate replace to={ROUTES.teacherDashboard} />} />
                <Route path="profile" element={<TeacherProfilePage />} />
                <Route path="dashboard" element={<TeacherDashboardPage />} />
                <Route element={<RequireApprovedTeacher />}>
                  <Route
                    path="tests"
                    element={<TeacherTestsPage />}
                  />
                  <Route
                    path="tests/create"
                    element={<TeacherTestBuilderPage />}
                  />
                  <Route
                    path="tests/:id/edit"
                    element={<TeacherTestBuilderPage />}
                  />
                  <Route
                    path="schedule"
                    element={<TeacherSchedulePage />}
                  />
                  <Route
                    path="earnings"
                    element={<TeacherEarningsPage />}
                  />
                  <Route
                    path="students"
                    element={<TeacherStudentsPage />}
                  />
                  <Route
                    path="notifications"
                    element={<TeacherNotificationsPage />}
                  />
                </Route>
              </Route>
            </Route>

            <Route path="admin" element={<RequireRole allowedRoles={['admin']} />}>
              <Route element={<AdminLayout />}>
                <Route index element={<Navigate replace to={ROUTES.adminDashboard} />} />
                <Route
                  path="dashboard"
                  element={<AdminDashboardPage />}
                />
                <Route
                  path="users"
                  element={<AdminUsersPage />}
                />
                <Route
                  path="teachers"
                  element={<AdminTeachersPage />}
                />
                <Route
                  path="tests"
                  element={<Navigate replace to={ROUTES.adminModeration} />}
                />
                <Route
                  path="moderation"
                  element={<AdminModerationPage />}
                />
                <Route
                  path="payments"
                  element={<AdminPaymentsPage />}
                />
                <Route
                  path="withdrawals"
                  element={<AdminWithdrawalsPage />}
                />
                <Route
                  path="reports"
                  element={<AdminReportsPage />}
                />
                <Route
                  path="settings"
                  element={<AdminSettingsPage />}
                />
              </Route>
            </Route>
          </Route>
        </Route>

        <Route
          path="*"
          element={
            <div className="site-shell">
              <NotFoundState />
            </div>
          }
        />
        </Routes>
      </SessionBootstrap>
    </BrowserRouter>
  )
}
