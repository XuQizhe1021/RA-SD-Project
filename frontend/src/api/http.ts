import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('hq-training-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      window.localStorage.removeItem('hq-training-token')
      window.localStorage.removeItem('hq-training-user')
      window.localStorage.removeItem('hq-training-menus')
    }
    return Promise.reject(error)
  },
)

export function getErrorMessage(error: unknown, fallback = '操作失败，请稍后重试') {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message || fallback
  }
  if (error instanceof Error) {
    return error.message || fallback
  }
  return fallback
}

export default http
