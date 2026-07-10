import { defineStore } from 'pinia'

import http from '../api/http'

export interface MenuItem {
  name: string
  path: string
  icon?: string
}

export interface UserInfo {
  id: number
  username: string
  displayName: string
  roles: string[]
}

interface LoginPayload {
  username: string
  password: string
}

interface LoginResponse {
  token: string
  userInfo: UserInfo
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

const TOKEN_KEY = 'hq-training-token'
const USER_KEY = 'hq-training-user'
const MENUS_KEY = 'hq-training-menus'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '' as string,
    user: null as UserInfo | null,
    menus: [] as MenuItem[],
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    primaryRole: (state) => state.user?.roles[0] ?? '',
    accessiblePaths: (state) => state.menus.map((item) => item.path),
  },
  actions: {
    restoreSession() {
      if (!this.token) {
        this.token = window.localStorage.getItem(TOKEN_KEY) ?? ''
      }
      if (!this.user) {
        const rawUser = window.localStorage.getItem(USER_KEY)
        this.user = rawUser ? (JSON.parse(rawUser) as UserInfo) : null
      }
      if (!this.menus.length) {
        const rawMenus = window.localStorage.getItem(MENUS_KEY)
        this.menus = rawMenus ? (JSON.parse(rawMenus) as MenuItem[]) : []
      }
    },
    async login(payload: LoginPayload) {
      const response = (await http.post('/auth/login', payload)) as ApiResponse<LoginResponse>
      this.token = response.data.token
      this.user = response.data.userInfo
      window.localStorage.setItem(TOKEN_KEY, this.token)
      window.localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      await this.fetchMenus()
    },
    async bootstrap() {
      await Promise.all([this.fetchCurrentUser(), this.fetchMenus()])
    },
    async fetchCurrentUser() {
      const response = (await http.get('/auth/me')) as ApiResponse<UserInfo>
      this.user = response.data
      window.localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    async fetchMenus() {
      const response = (await http.get('/auth/menus')) as ApiResponse<MenuItem[]>
      this.menus = response.data
      window.localStorage.setItem(MENUS_KEY, JSON.stringify(this.menus))
    },
    logout() {
      this.token = ''
      this.user = null
      this.menus = []
      window.localStorage.removeItem(TOKEN_KEY)
      window.localStorage.removeItem(USER_KEY)
      window.localStorage.removeItem(MENUS_KEY)
    },
    hasRole(role: string) {
      return this.user?.roles.includes(role) ?? false
    },
  },
})
