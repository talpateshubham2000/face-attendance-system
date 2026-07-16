import { createContext, useContext, useEffect, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Load whatever we last knew about the user from localStorage on first mount
  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');
    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch {
        localStorage.removeItem('user');
      }
    }
    setIsLoading(false);
  }, []);

  // Called after signup/login - persists both the token and user snapshot
  function saveSession(authResponse) {
    const { token, ...userFields } = authResponse;
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userFields));
    setUser(userFields);
  }

  // Called after completing profile / registering face - updates the cached snapshot
  function updateUser(partialUser) {
    setUser((prev) => {
      const next = { ...prev, ...partialUser };
      localStorage.setItem('user', JSON.stringify(next));
      return next;
    });
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  }

  const isAuthenticated = Boolean(user && localStorage.getItem('token'));

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, saveSession, updateUser, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
