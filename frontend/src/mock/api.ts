// A fake API that resolves promises after a small delay, so the UI exercises real
// async patterns (loading spinners, error states) before we wire the real backend.
// Every function returns the same shape the Spring Boot endpoint will.

import type { Appointment, AuthSession, Service, SlotAvailability, StudyLine } from '../types';
import { mockAppointments, mockServices, mockUsers } from './data';

const delay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms));

// In-memory booking state for the mock, so booking a slot makes it disappear from
// the grid and the new appointment shows up in "My appointments".
const bookedSlotIds = new Set<number>();
let nextAppointmentId = 200;

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

  async listSlots(date: string): Promise<SlotAvailability[]> {
    await delay();
    // 48 slots, 09:00–20:45, ids derived from the date so they're stable.
    const base = Number(date.replaceAll('-', '')) * 100;
    const slots: SlotAvailability[] = [];
    let idx = 0;
    for (let hour = 9; hour < 21; hour++) {
      for (let minute = 0; minute < 60; minute += 15) {
        const id = base + idx;
        const hh = String(hour).padStart(2, '0');
        const mm = String(minute).padStart(2, '0');
        // Pretend some slots are already taken so availability looks realistic.
        const taken = bookedSlotIds.has(id) || idx % 7 === 3;
        slots.push({
          id,
          startTime: `${date}T${hh}:${mm}:00+05:30`,
          capacity: 1,
          available: taken ? 0 : 1,
        });
        idx++;
      }
    }
    return slots;
  },

  async book(slot: SlotAvailability, serviceIds: number[]): Promise<Appointment> {
    await delay();
    if (bookedSlotIds.has(slot.id)) {
      throw new Error('That slot was just taken — please pick another.');
    }
    const studies: StudyLine[] = serviceIds.map((serviceId) => {
      const svc = mockServices.find((s) => s.id === serviceId);
      if (!svc) throw new Error('Unknown service');
      return { serviceId, name: svc.name, priceSnapshot: svc.price };
    });
    const billedAmount = studies.reduce((sum, s) => sum + s.priceSnapshot, 0);
    const appointment: Appointment = {
      id: nextAppointmentId++,
      slotId: slot.id,
      slotStartTime: slot.startTime,
      status: 'BOOKED',
      billedAmount,
      studies,
    };
    bookedSlotIds.add(slot.id);
    mockAppointments.unshift(appointment); // newest first
    return appointment;
  },

  async cancel(appointmentId: number): Promise<Appointment> {
    await delay();
    const appt = mockAppointments.find((a) => a.id === appointmentId);
    if (!appt) throw new Error('Appointment not found');
    if (appt.status !== 'BOOKED') {
      throw new Error('Only a booked appointment can be cancelled');
    }
    appt.status = 'CANCELLED';
    bookedSlotIds.delete(appt.slotId); // cancelling frees the slot
    return appt;
  },

  async reschedule(appointmentId: number, slot: SlotAvailability): Promise<Appointment> {
    await delay();
    const appt = mockAppointments.find((a) => a.id === appointmentId);
    if (!appt) throw new Error('Appointment not found');
    if (appt.status !== 'BOOKED') {
      throw new Error('Only a booked appointment can be rescheduled');
    }
    if (slot.id !== appt.slotId && bookedSlotIds.has(slot.id)) {
      throw new Error('That slot is already taken');
    }
    bookedSlotIds.delete(appt.slotId); // free the old slot
    appt.slotId = slot.id;
    appt.slotStartTime = slot.startTime;
    bookedSlotIds.add(slot.id);
    return appt;
  },
};
