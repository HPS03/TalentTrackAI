import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setSessionExpiredHandler, tokenStore } from '../api/client';
import { authApi } from '../api/services';

const AuthContext = createContext(null);

export const homeFor = (role) =>
  ({ CANDIDATE: '/candidate', RECRUITER: '/recruiter', ADMIN: '/admin' })[role] || '/';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(tokenStore.user);
  const navigate = useNavigate();

  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      navigate('/login', { replace: true });
    });
  }, [navigate]);

  const handleAuth = useCallback((res) => {
    tokenStore.save(res);
    setUser(res.user);
    return res.user;
  }, []);

  const login = useCallback((email, password) => authApi.login({ email, password }).then(handleAuth), [handleAuth]);
  const register = useCallback((body) => authApi.register(body).then(handleAuth), [handleAuth]);

  const logout = useCallback(async () => {
    const refresh = tokenStore.refresh;
    tokenStore.clear();
    setUser(null);
    if (refresh) authApi.logout(refresh).catch(() => {});
    navigate('/login');
  }, [navigate]);

  const value = useMemo(() => ({ user, login, register, logout }), [user, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
