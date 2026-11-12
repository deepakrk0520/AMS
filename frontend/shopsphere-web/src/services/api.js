import axios from 'axios'

const baseURL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')

// Single Axios instance for the whole app. All backend routes are versioned (ADR-004).
const api = axios.create({
  baseURL: `${baseURL}/api/v1`,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// The auth layer registers these so this module has no dependency on React.
let getToken = () => null
let onUnauthorized = () => {}

export function configureApi({ tokenGetter, unauthorizedHandler }) {
  getToken = tokenGetter
  onUnauthorized = unauthorizedHandler
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 on a request that carried a token means it expired or is invalid.
    // (A 401 from /auth/login is just bad credentials and has no token.)
    if (error.response?.status === 401 && error.config?.headers?.Authorization) {
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)

/** Turns any Axios error into a short, user-safe message. */
export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error.response) {
    return 'Cannot reach the server. Check your connection and try again.'
  }
  const { status, data } = error.response
  if (status === 403) return 'You do not have permission to do that.'
  if (status >= 500) return 'The server had a problem. Please try again later.'
  return data?.message || fallback
}

export default api
