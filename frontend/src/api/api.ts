// Real patient-facing API calls against the Spring backend. Each function keeps
// the SAME signature the mock had, so the screens don't change when we swap the
// import — that's the payoff of mirroring the backend DTOs in src/types.

import type { Appointment, Service, SlotAvailability } from '../types';
import { request } from './client';
import { getSession } from './session';

export const api = {
  listServices: () => request<Service[]>('/api/services'),

  listMyAppointments: () => request<Appointment[]>('/api/appointments/mine'),

  listSlots: (date: string) =>
    request<SlotAvailability[]>(`/api/slots?date=${encodeURIComponent(date)}`),

  book: (slot: SlotAvailability, serviceIds: number[]) =>
    request<Appointment>('/api/appointments', {
      method: 'POST',
      // The backend requires a patient name to create the profile on first
      // booking; registration only captured an email, so we use that for now.
      body: { slotId: slot.id, serviceIds, patient: { fullName: getSession()?.email ?? 'Patient' } },
    }),

  cancel: (appointmentId: number) =>
    request<Appointment>(`/api/appointments/${appointmentId}/cancel`, { method: 'POST' }),

  reschedule: (appointmentId: number, slot: SlotAvailability) =>
    request<Appointment>(`/api/appointments/${appointmentId}/reschedule`, {
      method: 'POST',
      body: { slotId: slot.id },
    }),
};
