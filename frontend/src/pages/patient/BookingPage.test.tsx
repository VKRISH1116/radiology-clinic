import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BookingPage } from './BookingPage';

// Mock the API module shared by BookingPage and the SlotPicker it renders.
vi.mock('../../api/api', () => ({
  api: {
    listServices: vi.fn(),
    listSlots: vi.fn(),
    book: vi.fn(),
  },
}));
import { api } from '../../api/api';

const listServices = vi.mocked(api.listServices);
const listSlots = vi.mocked(api.listSlots);

beforeEach(() => {
  vi.clearAllMocks();
  listServices.mockResolvedValue([
    { id: 4, category: 'General', name: 'Ultrasound Thyroid', price: 1500 },
  ]);
  listSlots.mockResolvedValue([
    { id: 10, startTime: '2030-01-01T09:00:00+05:30', capacity: 1, available: 1 },
    { id: 11, startTime: '2030-01-01T09:15:00+05:30', capacity: 1, available: 0 },
  ]);
});

describe('BookingPage', () => {
  it('enables Confirm only after a study AND a slot are chosen, and updates the bill', async () => {
    render(
      <MemoryRouter>
        <BookingPage />
      </MemoryRouter>,
    );

    const confirm = await screen.findByRole('button', { name: /confirm booking/i });
    expect(confirm).toBeDisabled();
    // Bill starts at zero.
    expect(screen.getByText('₹0')).toBeInTheDocument();

    // Pick the study -> bill updates, but no slot yet so still disabled.
    await userEvent.click(screen.getByRole('checkbox'));
    expect(screen.queryByText('₹0')).not.toBeInTheDocument();
    expect(confirm).toBeDisabled();

    // The fully-booked 09:15 slot is disabled; pick the free 09:00 one.
    expect(screen.getByRole('button', { name: '09:15' })).toBeDisabled();
    await userEvent.click(await screen.findByRole('button', { name: '09:00' }));

    expect(confirm).toBeEnabled();
  });
});
