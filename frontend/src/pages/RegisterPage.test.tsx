import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../auth/AuthContext';
import { RegisterPage } from './RegisterPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}));
import { register } from '../api/auth';

const registerMock = vi.mocked(register);

function renderRegister() {
  render(
    <AuthProvider>
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    </AuthProvider>,
  );
}

async function fill(email: string, password: string, confirm: string) {
  await userEvent.type(screen.getByLabelText('Email'), email);
  await userEvent.type(screen.getByLabelText('Password'), password);
  await userEvent.type(screen.getByLabelText('Confirm password'), confirm);
  await userEvent.click(screen.getByRole('button', { name: /create account/i }));
}

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
});

describe('RegisterPage', () => {
  it('rejects a short password without calling the API', async () => {
    renderRegister();
    await fill('a@b.com', 'short', 'short');
    expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('rejects mismatched passwords without calling the API', async () => {
    renderRegister();
    await fill('a@b.com', 'password123', 'different1');
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('calls register with valid input', async () => {
    registerMock.mockResolvedValue(undefined);
    renderRegister();
    await fill('a@b.com', 'password123', 'password123');
    expect(registerMock).toHaveBeenCalledWith('a@b.com', 'password123');
  });
});
