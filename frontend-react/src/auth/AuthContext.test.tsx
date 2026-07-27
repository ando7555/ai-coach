import { render, screen } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';

function AuthProbe() {
  const { isAuthenticated, user } = useAuth();

  return (
    <div>
      <span>{isAuthenticated ? 'authenticated' : 'anonymous'}</span>
      <strong>{user?.username ?? 'no-user'}</strong>
    </div>
  );
}

describe('AuthProvider', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('hydrates auth state from the existing JWT local storage keys', () => {
    localStorage.setItem('jwt_token', 'token');
    localStorage.setItem('jwt_user', JSON.stringify({ username: 'coach@example.com', email: 'coach@example.com', role: 'ADMIN' }));

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    );

    expect(screen.getByText('authenticated')).toBeInTheDocument();
    expect(screen.getByText('coach@example.com')).toBeInTheDocument();
  });

  it('clears malformed stored user data', () => {
    localStorage.setItem('jwt_user', '{bad-json');

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    );

    expect(screen.getByText('anonymous')).toBeInTheDocument();
    expect(localStorage.getItem('jwt_user')).toBeNull();
  });
});
