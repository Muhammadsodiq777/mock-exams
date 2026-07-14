import { useMutation, useQueryClient } from '@tanstack/react-query'
import { type FormEvent, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'

import { SessionLoadingState } from '../../components/routing/ShellState'
import {
  Alert,
  Button,
  PageContainer,
  Panel,
  Tabs,
  TextareaField,
  TextField,
} from '../../components/ui'
import { getDefaultAuthenticatedRoute } from '../../constants/navigation'
import { authKeys } from '../../hooks/useAuthSession'
import { authService } from '../../services/auth.service'
import { useSessionStore } from '../../store/sessionStore'
import type {
  AuthSessionRDO,
  LoginCDO,
  RegisterStudentCDO,
  RegisterTeacherCDO,
} from '../../types/backend'
import { toFormErrorState, type FormErrorState } from '../../utils/apiErrors'

type LoginField = keyof LoginCDO
type StudentRegistrationField = keyof RegisterStudentCDO
type TeacherRegistrationField = keyof RegisterTeacherCDO
type RegistrationMode = 'student' | 'teacher'

const loginFields = ['email', 'password'] satisfies LoginField[]
const studentRegistrationFields = ['email', 'password', 'fullName'] satisfies StudentRegistrationField[]
const teacherRegistrationFields = [
  'email',
  'password',
  'fullName',
  'displayName',
  'bio',
] satisfies TeacherRegistrationField[]

const emptyLoginForm: LoginCDO = {
  email: '',
  password: '',
}

const emptyStudentRegistrationForm: RegisterStudentCDO = {
  email: '',
  password: '',
  fullName: '',
}

const emptyTeacherRegistrationForm: RegisterTeacherCDO = {
  email: '',
  password: '',
  fullName: '',
  displayName: '',
  bio: '',
}

function useApplyAuthenticatedSession() {
  const setAuthenticated = useSessionStore((session) => session.setAuthenticated)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return (session: AuthSessionRDO) => {
    setAuthenticated(session)
    queryClient.setQueryData(authKeys.currentUser, session.user)
    navigate(getDefaultAuthenticatedRoute(useSessionStore.getState()), { replace: true })
  }
}

export function LoginPage() {
  const session = useSessionStore()
  const applyAuthenticatedSession = useApplyAuthenticatedSession()
  const [formData, setFormData] = useState<LoginCDO>(emptyLoginForm)
  const [formError, setFormError] = useState<FormErrorState<LoginField>>({
    formMessage: null,
    fieldErrors: {},
  })
  const loginMutation = useMutation({
    mutationFn: authService.login,
    onSuccess: applyAuthenticatedSession,
    onError: (error) => {
      setFormError(
        toFormErrorState(error, loginFields, 'Unable to log in with those credentials.'),
      )
    },
  })

  if (session.status === 'authenticated') {
    return <Navigate replace to={getDefaultAuthenticatedRoute(session)} />
  }

  if (session.status === 'loading') {
    return <SessionLoadingState />
  }

  const updateField = (field: LoginField, value: string) => {
    setFormData((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFormError({ formMessage: null, fieldErrors: {} })
    loginMutation.mutate({
      email: formData.email.trim(),
      password: formData.password,
    })
  }

  return (
    <PageContainer size="compact">
      <Panel className="auth-panel">
        <p className="ui-eyebrow">Auth</p>
        <h1>Log in</h1>
        <form className="auth-form" onSubmit={handleSubmit}>
          {formError.formMessage && <Alert variant="danger">{formError.formMessage}</Alert>}
          <TextField
            autoComplete="email"
            disabled={loginMutation.isPending}
            error={formError.fieldErrors.email}
            label="Email"
            name="email"
            onChange={(event) => updateField('email', event.target.value)}
            required
            type="email"
            value={formData.email}
          />
          <TextField
            autoComplete="current-password"
            disabled={loginMutation.isPending}
            error={formError.fieldErrors.password}
            label="Password"
            minLength={8}
            name="password"
            onChange={(event) => updateField('password', event.target.value)}
            required
            type="password"
            value={formData.password}
          />
          <Button isLoading={loginMutation.isPending} loadingLabel="Logging in" type="submit">
            Log in
          </Button>
        </form>
      </Panel>
    </PageContainer>
  )
}

export function RegisterPage() {
  const session = useSessionStore()
  const applyAuthenticatedSession = useApplyAuthenticatedSession()
  const [mode, setMode] = useState<RegistrationMode>('student')
  const [studentFormData, setStudentFormData] = useState<RegisterStudentCDO>(
    emptyStudentRegistrationForm,
  )
  const [teacherFormData, setTeacherFormData] = useState<RegisterTeacherCDO>(
    emptyTeacherRegistrationForm,
  )
  const [studentFormError, setStudentFormError] = useState<
    FormErrorState<StudentRegistrationField>
  >({
    formMessage: null,
    fieldErrors: {},
  })
  const [teacherFormError, setTeacherFormError] = useState<
    FormErrorState<TeacherRegistrationField>
  >({
    formMessage: null,
    fieldErrors: {},
  })
  const studentRegistrationMutation = useMutation({
    mutationFn: authService.registerStudent,
    onSuccess: applyAuthenticatedSession,
    onError: (error) => {
      setStudentFormError(
        toFormErrorState(
          error,
          studentRegistrationFields,
          'Unable to create a student account.',
        ),
      )
    },
  })
  const teacherRegistrationMutation = useMutation({
    mutationFn: authService.registerTeacher,
    onSuccess: applyAuthenticatedSession,
    onError: (error) => {
      setTeacherFormError(
        toFormErrorState(
          error,
          teacherRegistrationFields,
          'Unable to create a teacher account.',
        ),
      )
    },
  })

  if (session.status === 'authenticated') {
    return <Navigate replace to={getDefaultAuthenticatedRoute(session)} />
  }

  if (session.status === 'loading') {
    return <SessionLoadingState />
  }

  const updateStudentField = (field: StudentRegistrationField, value: string) => {
    setStudentFormData((current) => ({ ...current, [field]: value }))
  }

  const updateTeacherField = (field: TeacherRegistrationField, value: string) => {
    setTeacherFormData((current) => ({ ...current, [field]: value }))
  }

  const submitStudentRegistration = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setStudentFormError({ formMessage: null, fieldErrors: {} })
    studentRegistrationMutation.mutate({
      email: studentFormData.email.trim(),
      password: studentFormData.password,
      fullName: studentFormData.fullName.trim(),
    })
  }

  const submitTeacherRegistration = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setTeacherFormError({ formMessage: null, fieldErrors: {} })
    teacherRegistrationMutation.mutate({
      email: teacherFormData.email.trim(),
      password: teacherFormData.password,
      fullName: teacherFormData.fullName.trim(),
      displayName: teacherFormData.displayName.trim(),
      bio: teacherFormData.bio.trim(),
    })
  }

  const setRegistrationMode = (nextMode: string) => {
    if (nextMode === 'student' || nextMode === 'teacher') {
      setMode(nextMode)
    }
  }

  return (
    <PageContainer size="compact">
      <Panel className="auth-panel">
        <p className="ui-eyebrow">Auth</p>
        <h1>Create account</h1>
        <Tabs
          value={mode}
          onValueChange={setRegistrationMode}
          items={[
            {
              id: 'student',
              label: 'Student',
              content: (
                <form className="auth-form" onSubmit={submitStudentRegistration}>
                  {studentFormError.formMessage && (
                    <Alert variant="danger">{studentFormError.formMessage}</Alert>
                  )}
                  <TextField
                    autoComplete="name"
                    disabled={studentRegistrationMutation.isPending}
                    error={studentFormError.fieldErrors.fullName}
                    label="Full name"
                    name="fullName"
                    onChange={(event) => updateStudentField('fullName', event.target.value)}
                    required
                    value={studentFormData.fullName}
                  />
                  <TextField
                    autoComplete="email"
                    disabled={studentRegistrationMutation.isPending}
                    error={studentFormError.fieldErrors.email}
                    label="Email"
                    name="email"
                    onChange={(event) => updateStudentField('email', event.target.value)}
                    required
                    type="email"
                    value={studentFormData.email}
                  />
                  <TextField
                    autoComplete="new-password"
                    disabled={studentRegistrationMutation.isPending}
                    error={studentFormError.fieldErrors.password}
                    label="Password"
                    minLength={8}
                    name="password"
                    onChange={(event) => updateStudentField('password', event.target.value)}
                    required
                    type="password"
                    value={studentFormData.password}
                  />
                  <Button
                    isLoading={studentRegistrationMutation.isPending}
                    loadingLabel="Creating account"
                    type="submit"
                  >
                    Create student account
                  </Button>
                </form>
              ),
            },
            {
              id: 'teacher',
              label: 'Teacher',
              content: (
                <form className="auth-form" onSubmit={submitTeacherRegistration}>
                  {teacherFormError.formMessage && (
                    <Alert variant="danger">{teacherFormError.formMessage}</Alert>
                  )}
                  <TextField
                    autoComplete="name"
                    disabled={teacherRegistrationMutation.isPending}
                    error={teacherFormError.fieldErrors.fullName}
                    label="Full name"
                    name="fullName"
                    onChange={(event) => updateTeacherField('fullName', event.target.value)}
                    required
                    value={teacherFormData.fullName}
                  />
                  <TextField
                    autoComplete="organization-title"
                    disabled={teacherRegistrationMutation.isPending}
                    error={teacherFormError.fieldErrors.displayName}
                    label="Display name"
                    name="displayName"
                    onChange={(event) => updateTeacherField('displayName', event.target.value)}
                    required
                    value={teacherFormData.displayName}
                  />
                  <TextField
                    autoComplete="email"
                    disabled={teacherRegistrationMutation.isPending}
                    error={teacherFormError.fieldErrors.email}
                    label="Email"
                    name="email"
                    onChange={(event) => updateTeacherField('email', event.target.value)}
                    required
                    type="email"
                    value={teacherFormData.email}
                  />
                  <TextField
                    autoComplete="new-password"
                    disabled={teacherRegistrationMutation.isPending}
                    error={teacherFormError.fieldErrors.password}
                    label="Password"
                    minLength={8}
                    name="password"
                    onChange={(event) => updateTeacherField('password', event.target.value)}
                    required
                    type="password"
                    value={teacherFormData.password}
                  />
                  <TextareaField
                    disabled={teacherRegistrationMutation.isPending}
                    error={teacherFormError.fieldErrors.bio}
                    label="Bio"
                    name="bio"
                    onChange={(event) => updateTeacherField('bio', event.target.value)}
                    required
                    rows={4}
                    value={teacherFormData.bio}
                  />
                  <Button
                    isLoading={teacherRegistrationMutation.isPending}
                    loadingLabel="Creating account"
                    type="submit"
                  >
                    Create teacher account
                  </Button>
                </form>
              ),
            },
          ]}
        />
      </Panel>
    </PageContainer>
  )
}
