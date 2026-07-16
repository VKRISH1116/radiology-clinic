import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { StatsBar } from './StatsBar';

vi.mock('../../api/api', () => ({
  api: { getDashboardStats: vi.fn() },
}));
import { api } from '../../api/api';

const getDashboardStats = vi.mocked(api.getDashboardStats);

beforeEach(() => vi.clearAllMocks());

describe('StatsBar (AC-F7-1)', () => {
  it('renders the KPI counts and top referrers once loaded', async () => {
    getDashboardStats.mockResolvedValue({
      todaysAppointments: 3,
      totalPatients: 12,
      reportsDelivered: 7,
      reportsPending: 2,
      topReferrers: [
        { referringDoctorId: 1, doctorName: 'Dr. Meera Sharma', referralCount: 4, totalAmount: 2500 },
      ],
    });

    render(<StatsBar />);

    // Each labelled tile shows its count.
    expect(await screen.findByText("Today's appointments")).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('Reports pending')).toBeInTheDocument();
    // Top referrer name is listed.
    expect(screen.getByText('Dr. Meera Sharma')).toBeInTheDocument();
  });

  it('shows a fallback message when the stats call fails', async () => {
    getDashboardStats.mockRejectedValue(new Error('boom'));
    render(<StatsBar />);
    expect(await screen.findByText(/dashboard stats unavailable/i)).toBeInTheDocument();
  });
});
