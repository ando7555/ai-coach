import { createContext, PropsWithChildren, useContext, useMemo, useState } from 'react';
import { GraphQLClient } from '../api/graphqlClient';
import { readStorageItem, removeStorageItem, writeStorageItem } from './storage';

export type AuthUser = {
  id?: string;
  username: string;
  role: string;
};

type AuthContextValue = {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<AuthUser>;
  register: (username: string, password: string, role: string) => Promise<AuthUser>;
  logout: () => void;
};

const TOKEN_KEY = 'jwt_token';
const USER_KEY = 'jwt_user';

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const authClient = new GraphQLClient({ getToken: () => readStorageItem(TOKEN_KEY) });

type AuthPayload = {
  token: string;
  user: AuthUser;
};

function readStoredUser(): AuthUser | null {
  const rawUser = readStorageItem(USER_KEY);

  if (!rawUser) {
    return null;
  }

  try {
    return JSON.parse(rawUser) as AuthUser;
  } catch {
    removeStorageItem(USER_KEY);
    return null;
  }
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [token, setToken] = useState(() => readStorageItem(TOKEN_KEY));
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser());

  function persistAuth(payload: AuthPayload) {
    writeStorageItem(TOKEN_KEY, payload.token);
    writeStorageItem(USER_KEY, JSON.stringify(payload.user));
    setToken(payload.token);
    setUser(payload.user);
    return payload.user;
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      login: async (username, password) => {
        const data = await authClient.request<{ login: AuthPayload }, { username: string; password: string }>(
          `
            mutation Login($username: String!, $password: String!) {
              login(username: $username, password: $password) {
                token
                user { id username role }
              }
            }
          `,
          { username, password }
        );

        return persistAuth(data.login);
      },
      register: async (username, password, role) => {
        const data = await authClient.request<
          { register: AuthPayload },
          { username: string; password: string; role: string }
        >(
          `
            mutation Register($username: String!, $password: String!, $role: String) {
              register(username: $username, password: $password, role: $role) {
                token
                user { id username role }
              }
            }
          `,
          { username, password, role }
        );

        return persistAuth(data.register);
      },
      logout: () => {
        removeStorageItem(TOKEN_KEY);
        removeStorageItem(USER_KEY);
        setToken(null);
        setUser(null);
      }
    }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);

  if (!value) {
    throw new Error('useAuth must be used within AuthProvider');
  }

  return value;
}
