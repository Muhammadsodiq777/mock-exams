import { api, unwrapResource } from './api'
import { apiRoutes } from './apiRoutes'
import type {
  ApiSuccessEnvelope,
  AuthSessionRDO,
  CurrentUserRDO,
  LoginCDO,
  RegisterStudentCDO,
  RegisterTeacherCDO,
} from '../types/backend'

export const authApiRoutes = apiRoutes.auth

export const authService = {
  async registerStudent(payload: RegisterStudentCDO) {
    const response = await api.post<ApiSuccessEnvelope<AuthSessionRDO>>(
      apiRoutes.auth.registerStudent,
      payload,
    )
    return unwrapResource(response)
  },

  async registerTeacher(payload: RegisterTeacherCDO) {
    const response = await api.post<ApiSuccessEnvelope<AuthSessionRDO>>(
      apiRoutes.auth.registerTeacher,
      payload,
    )
    return unwrapResource(response)
  },

  async login(payload: LoginCDO) {
    const response = await api.post<ApiSuccessEnvelope<AuthSessionRDO>>(apiRoutes.auth.login, payload)
    return unwrapResource(response)
  },

  async getCurrentUser() {
    const response = await api.get<ApiSuccessEnvelope<CurrentUserRDO>>(apiRoutes.auth.me)
    return unwrapResource(response)
  },
}
