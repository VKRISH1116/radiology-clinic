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

/** One priced line item on an appointment. */
export interface StudyLine {
  serviceId: number;
  name: string;
  priceSnapshot: number;
}

/** A booked visit (from /api/appointments/mine). */
export interface Appointment {
  id: number;
  slotId: number;
  slotStartTime: string; // ISO-8601 instant
  status: AppointmentStatus;
  billedAmount: number;
  studies: StudyLine[];
}
