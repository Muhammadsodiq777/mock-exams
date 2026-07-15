import { lazy, Suspense } from 'react'
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
import { Loader } from '../components/ui'
import { ROUTES } from '../constants/routes'

const LoginPage = lazy(() => import('../pages/auth/AuthPages').then((module) => ({ default: module.LoginPage })))
const RegisterPage = lazy(() => import('../pages/auth/AuthPages').then((module) => ({ default: module.RegisterPage })))
const MarketplaceDetailPage = lazy(() => import('../pages/public/MarketplaceDetailPage').then((module) => ({ default: module.MarketplaceDetailPage })))
const MarketplacePage = lazy(() => import('../pages/public/MarketplacePage').then((module) => ({ default: module.MarketplacePage })))
const PricingPage = lazy(() => import('../pages/public/PricingPage').then((module) => ({ default: module.PricingPage })))
const PublicTeacherProfilePage = lazy(() => import('../pages/public/TeacherProfilePage').then((module) => ({ default: module.PublicTeacherProfilePage })))
const TestDetailPage = lazy(() => import('../pages/public/TestDetailPage').then((module) => ({ default: module.TestDetailPage })))
const TestsPage = lazy(() => import('../pages/public/TestsPage').then((module) => ({ default: module.TestsPage })))
const DashboardPage = lazy(() => import('../pages/student/DashboardPage').then((module) => ({ default: module.DashboardPage })))
const ProfilePage = lazy(() => import('../pages/student/ProfilePage').then((module) => ({ default: module.ProfilePage })))
const ResultDetailPage = lazy(() => import('../pages/student/ResultDetailPage').then((module) => ({ default: module.ResultDetailPage })))
const SpeakingPracticePage = lazy(() => import('../pages/student/SpeakingPracticePage').then((module) => ({ default: module.SpeakingPracticePage })))
const WritingPracticePage = lazy(() => import('../pages/student/WritingPracticePage').then((module) => ({ default: module.WritingPracticePage })))
const TeacherDashboardPage = lazy(() => import('../pages/teacher/TeacherDashboardPage').then((module) => ({ default: module.TeacherDashboardPage })))
const TeacherEarningsPage = lazy(() => import('../pages/teacher/TeacherEarningsPage').then((module) => ({ default: module.TeacherEarningsPage })))
const TeacherNotificationsPage = lazy(() => import('../pages/teacher/TeacherNotificationsPage').then((module) => ({ default: module.TeacherNotificationsPage })))
const TeacherProfilePage = lazy(() => import('../pages/teacher/TeacherProfilePage').then((module) => ({ default: module.TeacherProfilePage })))
const TeacherSchedulePage = lazy(() => import('../pages/teacher/TeacherSchedulePage').then((module) => ({ default: module.TeacherSchedulePage })))
const TeacherStudentsPage = lazy(() => import('../pages/teacher/TeacherStudentsPage').then((module) => ({ default: module.TeacherStudentsPage })))
const TeacherTestBuilderPage = lazy(() => import('../pages/teacher/TeacherTestBuilderPage').then((module) => ({ default: module.TeacherTestBuilderPage })))
const TeacherTestsPage = lazy(() => import('../pages/teacher/TeacherTestsPage').then((module) => ({ default: module.TeacherTestsPage })))
const AdminDashboardPage = lazy(() => import('../pages/admin/AdminDashboardPage').then((module) => ({ default: module.AdminDashboardPage })))
const AdminModerationPage = lazy(() => import('../pages/admin/AdminModerationPage').then((module) => ({ default: module.AdminModerationPage })))
const AdminPaymentsPage = lazy(() => import('../pages/admin/AdminPaymentsPage').then((module) => ({ default: module.AdminPaymentsPage })))
const AdminReportsPage = lazy(() => import('../pages/admin/AdminReportsPage').then((module) => ({ default: module.AdminReportsPage })))
const AdminSettingsPage = lazy(() => import('../pages/admin/AdminSettingsPage').then((module) => ({ default: module.AdminSettingsPage })))
const AdminTeachersPage = lazy(() => import('../pages/admin/AdminTeachersPage').then((module) => ({ default: module.AdminTeachersPage })))
const AdminUsersPage = lazy(() => import('../pages/admin/AdminUsersPage').then((module) => ({ default: module.AdminUsersPage })))
const AdminWithdrawalsPage = lazy(() => import('../pages/admin/AdminWithdrawalsPage').then((module) => ({ default: module.AdminWithdrawalsPage })))

function RouteFallback() {
  return (
    <div className="site-shell">
      <main className="ui-page">
        <Loader label="Loading page" />
      </main>
    </div>
  )
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <SessionBootstrap>
        <Suspense fallback={<RouteFallback />}>
        <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<Navigate replace to={ROUTES.tests} />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="tests" element={<TestsPage />} />
          <Route path="tests/:id" element={<TestDetailPage />} />
          <Route path="marketplace" element={<MarketplacePage />} />
          <Route path="marketplace/:id" element={<MarketplaceDetailPage />} />
          <Route path="teachers/:id" element={<PublicTeacherProfilePage />} />
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
        </Suspense>
      </SessionBootstrap>
    </BrowserRouter>
  )
}
