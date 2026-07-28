import { createContext, PropsWithChildren, useContext, useMemo, useState } from 'react';
import { GraphQLClient } from '../api/graphqlClient';
import { readStorageItem, removeStorageItem, writeStorageItem } from './storage';

export type AuthUser = {
  id?: string;
  username: string;
  email: string;
  displayName?: string | null;
  pictureUrl?: string | null;
  emailConfirmed?: boolean;
  role: string;
};

type AuthContextValue = {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  signInWithGoogle: (idToken: string) => Promise<AuthUser>;
  signInWithEmail: (email: string, password: string) => Promise<AuthUser>;
  registerWithEmail: (input: EmailRegistrationInput) => Promise<EmailRegistrationResult>;
  confirmEmail: (token: string) => Promise<AuthUser>;
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

export type EmailRegistrationInput = {
  email: string;
  displayName?: string;
  password: string;
};

export type EmailRegistrationResult = {
  email: string;
  expiresAt: string;
  message: string;
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
                user { id username email displayName pictureUrl emailConfirmed role }
              }
            }
          `,
          { idToken }
        );

        return persistAuth(data.authenticateWithGoogle);
      },
      signInWithEmail: async (email, password) => {
        const data = await authClient.request<{ authenticateWithEmail: AuthPayload }, { email: string; password: string }>(
          `
            mutation AuthenticateWithEmail($email: String!, $password: String!) {
              authenticateWithEmail(email: $email, password: $password) {
                token
                user { id username email displayName pictureUrl emailConfirmed role }
              }
            }
          `,
          { email, password }
        );

        return persistAuth(data.authenticateWithEmail);
      },
      registerWithEmail: async (input) => {
        const data = await authClient.request<{ registerWithEmail: EmailRegistrationResult }, { input: EmailRegistrationInput }>(
          `
            mutation RegisterWithEmail($input: EmailRegistrationInput!) {
              registerWithEmail(input: $input) {
                email
                expiresAt
                message
              }
            }
          `,
          { input }
        );

        return data.registerWithEmail;
      },
      confirmEmail: async (confirmationToken) => {
        const data = await authClient.request<{ confirmEmail: AuthPayload }, { token: string }>(
          `
            mutation ConfirmEmail($token: String!) {
              confirmEmail(token: $token) {
                token
                user { id username email displayName pictureUrl emailConfirmed role }
              }
            }
          `,
          { token: confirmationToken }
        );

        return persistAuth(data.confirmEmail);
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
