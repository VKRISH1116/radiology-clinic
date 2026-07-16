import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReschedulePage } from './ReschedulePage';
import type { Appointment } from '../../types';

// Mock the API shared by ReschedulePage and the SlotPicker it renders.
vi.mock('../../api/api', () => ({
  api: {
    listMyAppointments: vi.fn(),
    listSlots: vi.fn(),
    reschedule: vi.fn(),
  },
}));
import { api } from '../../api/api';

const listMyAppointments = vi.mocked(api.listMyAppointments);
const listSlots = vi.mocked(api.listSlots);
const reschedule = vi.mocked(api.reschedule);

const APPT: Appointment = {
  id: 7,
  slotId: 100,
  slotStartTime: '2030-01-01T09:00:00+05:30',
  status: 'BOOKED',
  billedAmount: 1500,
  studies: [{ serviceId: 4, name: 'Ultrasound Thyroid', priceSnapshot: 1500 }],
};

beforeEach(() => {
  vi.clearAllMocks();
  listMyAppointments.mockResolvedValue([APPT]);
  listSlots.mockResolvedValue([
    { id: 200, startTime: '2030-01-02T10:00:00+05:30', capacity: 1, available: 1 },
    { id: 201, startTime: '2030-01-02T10:15:00+05:30', capacity: 1, available: 0 },
  ]);
  reschedule.mockResolvedValue(APPT);
});

function renderReschedule() {
  render(
    <MemoryRouter initialEntries={['/patient/appointments/7/reschedule']}>
      <Routes>
        <Route path="/patient/appointments/:id/reschedule" element={<ReschedulePage />} />
        <Route path="/patient" element={<div>patient home</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ReschedulePage', () => {
  it('loads the appointment, keeps Confirm disabled until a slot is chosen, then reschedules (AC-F2-3)', async () => {
    renderReschedule();

    // The appointment's current studies confirm the right record loaded.
    expect(await screen.findByText('Ultrasound Thyroid')).toBeInTheDocument();

    const confirm = screen.getByRole('button', { name: /confirm new time/i });
    expect(confirm).toBeDisabled();

    // Fully-booked 10:15 is disabled; pick the free 10:00 slot.
    expect(await screen.findByRole('button', { name: '10:15' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: '10:00' }));
    expect(confirm).toBeEnabled();

    await userEvent.click(confirm);
    expect(reschedule).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ id: 200 }),
    );
    expect(await screen.findByText('patient home')).toBeInTheDocument();
  });

  it('shows a not-found message when the id has no matching appointment', async () => {
    listMyAppointments.mockResolvedValue([]);
    renderReschedule();
    expect(await screen.findByText('Appointment not found.')).toBeInTheDocument();
  });
});
