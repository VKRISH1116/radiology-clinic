// Domain types shared across the app. These mirror the backend's JSON DTOs, so
// TypeScript can check that our components use the data the API actually returns.
// (In Phase 6 we'll fetch these shapes from the real Spring Boot endpoints.)

export type Role = 'PATIENT' | 'STAFF' | 'ADMIN';

export type AppointmentStatus = 'BOOKED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

/** What /api/auth/login returns: an access token, a refresh token, and identity. */
export interface AuthSession {
  token: string;
  refreshToken: string;
  email: string;
  role: Role;
}

/** A bookable ultrasound study (from /api/services). */
export interface Service {
  id: number;
  category: string;
  name: string;
  price: number;
}

/** One slot in a day's grid with remaining availability (from /api/slots?date=). */
export interface SlotAvailability {
  id: number;
  startTime: string; // ISO-8601 instant
  capacity: number;
  available: number;
}

/** One priced line item on an appointment. */
export interface StudyLine {
  serviceId: number;
  name: string;
  priceSnapshot: number;
}

/** A catalogue entry as admin sees it — includes the active flag. */
export interface AdminService extends Service {
  active: boolean;
}

export type ReferralStatus = 'PENDING' | 'PAID';

/** A computed referral payout in the ledger. */
export interface Referral {
  id: number;
  appointmentId: number;
  doctorName: string;
  amount: number;
  status: ReferralStatus;
  computedAt: string;
}

/** A configurable referral-engine rule (null scope column = "any"). */
export interface ReferralRule {
  id: number;
  referringDoctorId: number | null;
  serviceId: number | null;
  minAmount: number | null;
  maxAmount: number | null;
  percentage: number;
  active: boolean;
}

/** A referring doctor (for tagging bookings / rule scoping). */
export interface ReferringDoctor {
  id: number;
  name: string;
  phone: string | null;
}

/** One audit-trail entry (userId is the acting user, null for anonymous). */
export interface AuditEntry {
  id: number;
  action: string;
  entity: string | null;
  entityId: number | null;
  userId: number | null;
  createdAt: string;
}

/** A staff/admin account row for user management. */
export interface UserSummary {
  email: string;
  role: Role;
}

/** Per-doctor payout totals — the "top referrers" line in the admin overview. */
export interface ReferralSummary {
  referringDoctorId: number | null;
  doctorName: string | null;
  referralCount: number;
  totalAmount: number;
}

/** The admin dashboard's at-a-glance counts (from /api/admin/stats, AC-F7-1). */
export interface DashboardStats {
  todaysAppointments: number;
  totalPatients: number;
  reportsDelivered: number;
  reportsPending: number;
  topReferrers: ReferralSummary[];
}

/** A booked visit (from /api/appointments/mine, or the staff schedule). */
export interface Appointment {
  id: number;
  slotId: number;
  slotStartTime: string; // ISO-8601 instant
  status: AppointmentStatus;
  billedAmount: number;
  studies: StudyLine[];
  patientName?: string; // shown to staff; not needed on the patient's own view
  reportFileName?: string; // set once a report PDF is attached
}
