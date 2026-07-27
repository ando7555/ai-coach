import { createContext, PropsWithChildren, useContext, useMemo, useState } from 'react';
import { GraphQLClient } from '../api/graphqlClient';
import { readStorageItem, removeStorageItem, writeStorageItem } from './storage';

export type AuthUser = {
  id?: string;
  username: string;
  email: string;
  displayName?: string | null;
  pictureUrl?: string | null;
  role: string;
};

type AuthContextValue = {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  signInWithGoogle: (idToken: string) => Promise<AuthUser>;
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
      signInWithGoogle: async (idToken) => {
        const data = await authClient.request<{ authenticateWithGoogle: AuthPayload }, { idToken: string }>(
          `
            mutation AuthenticateWithGoogle($idToken: String!) {
              authenticateWithGoogle(idToken: $idToken) {
                token
                user { id username email displayName pictureUrl role }
              }
            }
          `,
          { idToken }
        );

        return persistAuth(data.authenticateWithGoogle);
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
