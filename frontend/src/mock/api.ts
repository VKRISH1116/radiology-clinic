// A fake API that resolves promises after a small delay, so the UI exercises real
// async patterns (loading spinners, error states) before we wire the real backend.
// Every function returns the same shape the Spring Boot endpoint will.

import type { Appointment, AuthSession, Service } from '../types';
import { mockAppointments, mockServices, mockUsers } from './data';

const delay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms));

export const mockApi = {
  async login(email: string, password: string): Promise<AuthSession> {
    await delay();
    const user = mockUsers.find((u) => u.email === email && u.password === password);
    if (!user) {
      // Same message whether the email is unknown or the password is wrong.
      throw new Error('Invalid email or password');
    }
    return {
      token: `mock.access.${user.role}`,
      refreshToken: 'mock.refresh',
      email: user.email,
      role: user.role,
    };
  },

  async register(email: string, password: string): Promise<void> {
    await delay();
    if (mockUsers.some((u) => u.email === email)) {
      throw new Error('Email already registered');
    }
    // Public registration only ever creates a PATIENT (same rule as the backend).
    mockUsers.push({ email, password, role: 'PATIENT' });
  },

  async listServices(): Promise<Service[]> {
    await delay();
    return mockServices;
  },

  async listMyAppointments(): Promise<Appointment[]> {
    await delay();
    return mockAppointments;
  },
};
