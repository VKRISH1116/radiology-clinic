// Mock data standing in for the backend during Phase 5. Phase 6 replaces the mock
// API with real HTTP calls; the component code above it won't need to change,
// because both speak the same types from src/types.

import type {
  AdminService,
  Appointment,
  AuditEntry,
  Referral,
  ReferralRule,
  Role,
  Service,
} from '../types';

export interface MockUser {
  email: string;
  password: string;
  role: Role;
}

// Demo logins (password is "password" for all three).
export const mockUsers: MockUser[] = [
  { email: 'patient@clinic.local', password: 'password', role: 'PATIENT' },
  { email: 'staff@clinic.local', password: 'password', role: 'STAFF' },
  { email: 'admin@clinic.local', password: 'password', role: 'ADMIN' },
];

// The 9 seeded ultrasound studies (mirrors Flyway V2).
export const mockServices: Service[] = [
  { id: 1, category: 'General', name: 'Ultrasound Abdomen & Pelvis', price: 1000 },
  { id: 2, category: 'General', name: 'Ultrasound Pelvis', price: 1000 },
  { id: 3, category: 'General', name: 'Ultrasound Small Parts', price: 1000 },
  { id: 6, category: 'General', name: 'Ultrasound Doppler', price: 1000 },
  { id: 5, category: 'General', name: 'Ultrasound Scrotum', price: 1500 },
  { id: 4, category: 'General', name: 'Ultrasound Thyroid', price: 1500 },
  { id: 7, category: 'Obstetrics', name: 'NT Scan', price: 1000 },
  { id: 9, category: 'Obstetrics', name: 'Growth & Doppler Scan', price: 2000 },
  { id: 8, category: 'Obstetrics', name: 'TIFFA Scan', price: 2000 },
];

// Build an ISO instant for today at a given clinic-local time.
function todayAt(hour: number, minute: number): string {
  const d = new Date();
  const date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate(),
  ).padStart(2, '0')}`;
  const hh = String(hour).padStart(2, '0');
  const mm = String(minute).padStart(2, '0');
  return `${date}T${hh}:${mm}:00+05:30`;
}

// The whole clinic's schedule for today, as staff would see it (with patient names).
export const mockSchedule: Appointment[] = [
  {
    id: 501,
    patientName: 'Asha Rao',
    slotId: 9001,
    slotStartTime: todayAt(9, 0),
    status: 'BOOKED',
    billedAmount: 2500,
    studies: [
      { serviceId: 1, name: 'Ultrasound Abdomen & Pelvis', priceSnapshot: 1000 },
      { serviceId: 4, name: 'Ultrasound Thyroid', priceSnapshot: 1500 },
    ],
  },
  {
    id: 502,
    patientName: 'Vikram Singh',
    slotId: 9002,
    slotStartTime: todayAt(9, 45),
    status: 'BOOKED',
    billedAmount: 1000,
    studies: [{ serviceId: 2, name: 'Ultrasound Pelvis', priceSnapshot: 1000 }],
  },
  {
    id: 503,
    patientName: 'Meena Kumari',
    slotId: 9003,
    slotStartTime: todayAt(10, 30),
    status: 'COMPLETED',
    billedAmount: 2000,
    studies: [{ serviceId: 8, name: 'TIFFA Scan', priceSnapshot: 2000 }],
    reportFileName: 'meena-tiffa.pdf',
  },
  {
    id: 504,
    patientName: 'Rahul Nair',
    slotId: 9004,
    slotStartTime: todayAt(11, 15),
    status: 'BOOKED',
    billedAmount: 1500,
    studies: [{ serviceId: 5, name: 'Ultrasound Scrotum', priceSnapshot: 1500 }],
  },
];

// A couple of appointments for the demo patient.
export const mockAppointments: Appointment[] = [
  {
    id: 101,
    slotId: 5,
    slotStartTime: '2026-07-20T09:00:00+05:30',
    status: 'BOOKED',
    billedAmount: 2500,
    studies: [
      { serviceId: 1, name: 'Ultrasound Abdomen & Pelvis', priceSnapshot: 1000 },
      { serviceId: 4, name: 'Ultrasound Thyroid', priceSnapshot: 1500 },
    ],
  },
  {
    id: 98,
    slotId: 2,
    slotStartTime: '2026-07-12T11:15:00+05:30',
    status: 'COMPLETED',
    billedAmount: 1000,
    studies: [{ serviceId: 2, name: 'Ultrasound Pelvis', priceSnapshot: 1000 }],
  },
];

// --- admin console data ---------------------------------------------------

// Full catalogue incl. an inactive entry (admin sees active + inactive).
export const mockCatalog: AdminService[] = [
  ...mockServices.map((s) => ({ ...s, active: true })),
  { id: 99, category: 'General', name: 'Ultrasound Chest (retired)', price: 1200, active: false },
];

export const mockReferrals: Referral[] = [
  { id: 1, appointmentId: 503, doctorName: 'Dr. Meera Sharma', amount: 625, status: 'PENDING', computedAt: todayAt(10, 35) },
  { id: 2, appointmentId: 480, doctorName: 'Dr. Arjun Iyer', amount: 225, status: 'PAID', computedAt: todayAt(9, 20) },
  { id: 3, appointmentId: 495, doctorName: 'Dr. Meera Sharma', amount: 400, status: 'PENDING', computedAt: todayAt(11, 5) },
];

export const mockRules: ReferralRule[] = [
  { id: 1, doctorName: null, serviceName: null, minAmount: null, percentage: 20, active: true },
  { id: 2, doctorName: 'Dr. Meera Sharma', serviceName: null, minAmount: null, percentage: 25, active: true },
  { id: 3, doctorName: null, serviceName: 'Ultrasound Thyroid', minAmount: null, percentage: 15, active: true },
  { id: 4, doctorName: null, serviceName: null, minAmount: 3000, percentage: 30, active: true },
];

export const mockAudit: AuditEntry[] = [
  { id: 1, action: 'REPORT_UPLOAD', entity: 'appointment', entityId: 503, actor: 'staff@clinic.local', createdAt: todayAt(10, 40) },
  { id: 2, action: 'PAYOUT_UPDATE', entity: 'referral', entityId: 2, actor: 'admin@clinic.local', createdAt: todayAt(9, 20) },
  { id: 3, action: 'REPORT_DOWNLOAD', entity: 'appointment', entityId: 503, actor: 'patient@clinic.local', createdAt: todayAt(11, 5) },
];
