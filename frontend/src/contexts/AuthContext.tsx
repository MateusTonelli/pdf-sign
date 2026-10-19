import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as api from '../api/endpoints';
import { clearTokens, setTokens } from '../api/client';
import type { User } from '../api/types';

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: {
    name: string;
    email: string;
    password: string;
    companyName: string;
  }) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function hasStoredToken(): boolean {
  return !!localStorage.getItem('pdf_sign_access_token');
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [bootstrapped, setBootstrapped] = useState(!hasStoredToken());

  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: api.fetchMe,
    enabled: hasStoredToken(),
    retry: false,
  });

  useEffect(() => {
    if (meQuery.isFetched && !bootstrapped) {
      setBootstrapped(true);
    }
  }, [bootstrapped, meQuery.isFetched]);

  useEffect(() => {
    if (meQuery.isError) {
      clearTokens();
      setBootstrapped(true);
    }
  }, [meQuery.isError]);

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api.login({ email, password });
      setTokens(res.accessToken, res.refreshToken);
      queryClient.setQueryData(['me'], res.user);
      setBootstrapped(true);
      await queryClient.invalidateQueries({ queryKey: ['me'] });
    },
    [queryClient],
  );

  const register = useCallback(
    async (data: {
      name: string;
      email: string;
      password: string;
      companyName: string;
    }) => {
      const res = await api.register(data);
      setTokens(res.accessToken, res.refreshToken);
      queryClient.setQueryData(['me'], res.user);
      setBootstrapped(true);
      await queryClient.invalidateQueries({ queryKey: ['me'] });
    },
    [queryClient],
  );

  const logout = useCallback(() => {
    clearTokens();
    queryClient.clear();
    setBootstrapped(true);
  }, [queryClient]);

  const refreshUser = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: ['me'] });
  }, [queryClient]);

  const value = useMemo<AuthContextValue>(() => {
    const tokenPresent = hasStoredToken();
    const user = meQuery.data ?? null;
    const isLoading =
      tokenPresent && (!bootstrapped || meQuery.isLoading || meQuery.isFetching);

    return {
      user,
      isLoading,
      isAuthenticated: tokenPresent && !!user && meQuery.isSuccess,
      login,
      register,
      logout,
      refreshUser,
    };
  }, [
    bootstrapped,
    login,
    logout,
    meQuery.data,
    meQuery.isFetching,
    meQuery.isLoading,
    meQuery.isSuccess,
    refreshUser,
    register,
  ]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider');
  }
  return ctx;
}
