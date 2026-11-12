import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { configureApi } from '../services/api.js'
import * as authService from '../services/authService.js'

/*
 * Token storage: the backend returns the JWT in the response body (it does not
 * set cookies), so the token is kept in sessionStorage. It survives a page
 * refresh but is cleared when the tab closes. Any script running on the page
 * (i.e. an XSS bug) can read it, so keep the app free of dangerouslySetInnerHTML
 * and untrusted scripts. Passwords are never stored. A stricter alternative
 * (HttpOnly cookie) would need a backend change and is out of scope here.
 */
const STORAGE_KEY = 'shopsphere.token'

function readToken() {
  try {
    return sessionStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

function writeToken(token) {
  try {
    if (token) sessionStorage.setItem(STORAGE_KEY, token)
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    /* storage unavailable: session lasts until refresh */
  }
}

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const tokenRef = useRef(readToken())
  const [user, setUser] = useState(null)
  // "loading" while we validate a stored token against /auth/me on startup.
  const [loading, setLoading] = useState(Boolean(tokenRef.current))

  const clear = useCallback(() => {
    tokenRef.current = null
    writeToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    configureApi({ tokenGetter: () => tokenRef.current, unauthorizedHandler: clear })
    if (!tokenRef.current) return
    authService
      .fetchCurrentUser()
      .then(setUser)
      .catch(clear)
      .finally(() => setLoading(false))
  }, [clear])

  const login = useCallback(async (credentials) => {
    const result = await authService.login(credentials)
    tokenRef.current = result.accessToken
    writeToken(result.accessToken)
    setUser(result.user)
    return result.user
  }, [])

  const value = useMemo(
    () => ({ user, loading, isAuthenticated: Boolean(user), login, logout: clear }),
    [user, loading, login, clear],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
