// A fake API that resolves promises after a small delay, so the UI exercises real
// async patterns (loading spinners, error states) before we wire the real backend.
// Every function returns the same shape the Spring Boot endpoint will.

import type {
  AdminService,
  Appointment,
  AuditEntry,
  AuthSession,
  Referral,
  ReferralRule,
  Role,
  Service,
  SlotAvailability,
  StudyLine,
  UserSummary,
} from '../types';
import {
  mockAppointments,
  mockAudit,
  mockCatalog,
  mockReferrals,
  mockRules,
  mockSchedule,
  mockServices,
  mockUsers,
} from './data';

const delay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms));

// In-memory booking state for the mock, so booking a slot makes it disappear from
// the grid and the new appointment shows up in "My appointments".
const bookedSlotIds = new Set<number>();
let nextAppointmentId = 200;
let nextId = 1000; // for newly created catalogue entries, rules, audit rows

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

  // --- staff / back-office ------------------------------------------------

  async listSchedule(): Promise<Appointment[]> {
    await delay();
    return mockSchedule;
  },

  async completeAppointment(appointmentId: number): Promise<Appointment> {
    await delay();
    const appt = mockSchedule.find((a) => a.id === appointmentId);
    if (!appt) throw new Error('Appointment not found');
    if (appt.status !== 'BOOKED') {
      throw new Error('Only a booked appointment can be completed');
    }
    appt.status = 'COMPLETED';
    return appt;
  },

  async uploadReport(appointmentId: number, fileName: string): Promise<Appointment> {
    await delay();
    const appt = mockSchedule.find((a) => a.id === appointmentId);
    if (!appt) throw new Error('Appointment not found');
    appt.reportFileName = fileName;
    return appt;
  },

  async walkInBook(
    patientName: string,
    slot: SlotAvailability,
    serviceIds: number[],
  ): Promise<Appointment> {
    await delay();
    if (bookedSlotIds.has(slot.id)) {
      throw new Error('That slot was just taken — please pick another.');
    }
    const studies: StudyLine[] = serviceIds.map((serviceId) => {
      const svc = mockServices.find((s) => s.id === serviceId);
      if (!svc) throw new Error('Unknown service');
      return { serviceId, name: svc.name, priceSnapshot: svc.price };
    });
    const appointment: Appointment = {
      id: nextAppointmentId++,
      patientName,
      slotId: slot.id,
      slotStartTime: slot.startTime,
      status: 'BOOKED',
      billedAmount: studies.reduce((sum, s) => sum + s.priceSnapshot, 0),
      studies,
    };
    bookedSlotIds.add(slot.id);
    mockSchedule.unshift(appointment);
    return appointment;
  },

  // --- admin: catalogue ---------------------------------------------------

  async listCatalog(): Promise<AdminService[]> {
    await delay();
    return mockCatalog;
  },

  async createService(category: string, name: string, price: number): Promise<AdminService> {
    await delay();
    const svc: AdminService = { id: nextId++, category, name, price, active: true };
    mockCatalog.push(svc);
    return svc;
  },

  async setServiceActive(id: number, active: boolean): Promise<AdminService> {
    await delay();
    const svc = mockCatalog.find((s) => s.id === id);
    if (!svc) throw new Error('Service not found');
    svc.active = active;
    return svc;
  },

  async updateServicePrice(id: number, price: number): Promise<AdminService> {
    await delay();
    const svc = mockCatalog.find((s) => s.id === id);
    if (!svc) throw new Error('Service not found');
    svc.price = price;
    return svc;
  },

  // --- admin: referrals ---------------------------------------------------

  async listReferrals(): Promise<Referral[]> {
    await delay();
    return mockReferrals;
  },

  async payReferral(id: number): Promise<Referral> {
    await delay();
    const ref = mockReferrals.find((r) => r.id === id);
    if (!ref) throw new Error('Referral not found');
    if (ref.status === 'PAID') throw new Error('Referral already paid');
    ref.status = 'PAID';
    mockAudit.unshift({
      id: nextId++,
      action: 'PAYOUT_UPDATE',
      entity: 'referral',
      entityId: id,
      actor: 'admin@clinic.local',
      createdAt: new Date().toISOString(),
    });
    return ref;
  },

  // --- admin: rules -------------------------------------------------------

  async listRules(): Promise<ReferralRule[]> {
    await delay();
    return mockRules;
  },

  async addRule(rule: Omit<ReferralRule, 'id' | 'active'>): Promise<ReferralRule> {
    await delay();
    const created: ReferralRule = { id: nextId++, active: true, ...rule };
    mockRules.push(created);
    return created;
  },

  // --- admin: users -------------------------------------------------------

  async listUsers(): Promise<UserSummary[]> {
    await delay();
    return mockUsers
      .filter((u) => u.role !== 'PATIENT')
      .map((u) => ({ email: u.email, role: u.role }));
  },

  async adminCreateUser(email: string, role: Role): Promise<void> {
    await delay();
    if (mockUsers.some((u) => u.email === email)) {
      throw new Error('Email already registered');
    }
    mockUsers.push({ email, password: 'password', role });
  },

  // --- admin: audit -------------------------------------------------------

  async listAudit(): Promise<AuditEntry[]> {
    await delay();
    return [...mockAudit].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  },
};
