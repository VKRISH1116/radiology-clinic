// Real API calls against the Spring backend. Every function keeps the SAME
// signature the mock had, so screens only swap their import — the payoff of
// mirroring the backend DTOs in src/types.

import type {
  AdminService,
  Appointment,
  AuditEntry,
  DashboardStats,
  Referral,
  ReferralRule,
  ReferringDoctor,
  Role,
  Service,
  SlotAvailability,
  UserSummary,
} from '../types';
import { request, upload } from './client';
import { getSession } from './session';

export const api = {
  // --- patient ------------------------------------------------------------
  listServices: () => request<Service[]>('/api/services'),

  listMyAppointments: () => request<Appointment[]>('/api/appointments/mine'),

  listSlots: (date: string) =>
    request<SlotAvailability[]>(`/api/slots?date=${encodeURIComponent(date)}`),

  book: (slot: SlotAvailability, serviceIds: number[]) =>
    request<Appointment>('/api/appointments', {
      method: 'POST',
      body: { slotId: slot.id, serviceIds, patient: { fullName: getSession()?.email ?? 'Patient' } },
    }),

  cancel: (appointmentId: number) =>
    request<Appointment>(`/api/appointments/${appointmentId}/cancel`, { method: 'POST' }),

  reschedule: (appointmentId: number, slot: SlotAvailability) =>
    request<Appointment>(`/api/appointments/${appointmentId}/reschedule`, {
      method: 'POST',
      body: { slotId: slot.id },
    }),

  // --- staff --------------------------------------------------------------
  listSchedule: () => request<Appointment[]>('/api/appointments'),

  completeAppointment: (appointmentId: number) =>
    request<Appointment>(`/api/appointments/${appointmentId}/complete`, { method: 'POST' }),

  uploadReport: (appointmentId: number, file: File) =>
    upload(`/api/appointments/${appointmentId}/report`, file),

  walkInBook: (
    patientName: string,
    slot: SlotAvailability,
    serviceIds: number[],
    referringDoctorId?: number,
  ) =>
    request<Appointment>('/api/appointments/walk-in', {
      method: 'POST',
      body: { slotId: slot.id, serviceIds, referringDoctorId, patient: { fullName: patientName } },
    }),

  // Add a referring doctor on the fly (staff walk-in "pick-or-type" field).
  createReferringDoctor: (name: string, phone?: string) =>
    request<ReferringDoctor>('/api/referring-doctors', {
      method: 'POST',
      body: { name, phone },
    }),

  // --- admin: dashboard ---------------------------------------------------
  getDashboardStats: () => request<DashboardStats>('/api/admin/stats'),

  // --- admin: catalogue ---------------------------------------------------
  listCatalog: () => request<AdminService[]>('/api/services/all'),

  createService: (category: string, name: string, price: number) =>
    request<AdminService>('/api/services', {
      method: 'POST',
      body: { category, name, price },
    }),

  // Full PUT — the backend replaces the row, so send every field.
  updateService: (svc: AdminService) =>
    request<AdminService>(`/api/services/${svc.id}`, {
      method: 'PUT',
      body: { category: svc.category, name: svc.name, price: svc.price, active: svc.active },
    }),

  // --- admin: referrals ---------------------------------------------------
  listReferrals: () => request<Referral[]>('/api/referrals'),

  payReferral: (id: number) =>
    request<Referral>(`/api/referrals/${id}/pay`, { method: 'POST' }),

  // --- admin: rules -------------------------------------------------------
  listRules: () => request<ReferralRule[]>('/api/referral-rules'),

  listReferringDoctors: () => request<ReferringDoctor[]>('/api/referring-doctors'),

  addRule: (rule: Omit<ReferralRule, 'id' | 'active'>) =>
    request<ReferralRule>('/api/referral-rules', { method: 'POST', body: rule }),

  // --- admin: users -------------------------------------------------------
  listUsers: () => request<UserSummary[]>('/api/admin/users'),

  adminCreateUser: (email: string, password: string, role: Role) =>
    request<void>('/api/admin/users', { method: 'POST', body: { email, password, role } }),

  // --- admin: audit -------------------------------------------------------
  listAudit: () => request<AuditEntry[]>('/api/audit-logs'),
};
