import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CatalogueSection } from './CatalogueSection';
import type { AdminService } from '../../../types';

vi.mock('../../../api/api', () => ({
  api: {
    listCatalog: vi.fn(),
    updateService: vi.fn(),
    createService: vi.fn(),
  },
}));
import { api } from '../../../api/api';

const listCatalog = vi.mocked(api.listCatalog);
const updateService = vi.mocked(api.updateService);
const createService = vi.mocked(api.createService);

const THYROID: AdminService = {
  id: 4,
  category: 'General',
  name: 'Ultrasound Thyroid',
  price: 1500,
  active: true,
};

beforeEach(() => {
  vi.clearAllMocks();
  listCatalog.mockResolvedValue([THYROID]);
});

describe('CatalogueSection', () => {
  it('shows Save only after the price changes, then persists the new price (AC-F8-1)', async () => {
    updateService.mockResolvedValue({ ...THYROID, price: 1800 });
    render(<CatalogueSection />);

    const priceInput = (await screen.findByDisplayValue('1500')) as HTMLInputElement;

    // No pending change yet -> no Save button in the row.
    const row = priceInput.closest('tr')!;
    expect(within(row).queryByRole('button', { name: 'Save' })).toBeNull();

    // Change the price -> Save appears (row is "dirty").
    await userEvent.clear(priceInput);
    await userEvent.type(priceInput, '1800');
    const save = within(row).getByRole('button', { name: 'Save' });

    await userEvent.click(save);
    expect(updateService).toHaveBeenCalledWith(
      expect.objectContaining({ id: 4, price: 1800 }),
    );
  });

  it('blocks adding a study with no name and does not call the API', async () => {
    render(<CatalogueSection />);
    await screen.findByDisplayValue('1500'); // wait for load

    await userEvent.click(screen.getByRole('button', { name: /add study/i }));
    expect(screen.getByText('Enter a name and a valid price')).toBeInTheDocument();
    expect(createService).not.toHaveBeenCalled();
  });
});
