import { describe, expect, it } from 'vitest';
import { formatINR } from './format';

describe('formatINR', () => {
  it('formats whole rupees with the ₹ symbol and no decimals', () => {
    expect(formatINR(2500)).toBe('₹2,500');
  });

  it('uses Indian digit grouping (lakhs)', () => {
    expect(formatINR(1000000)).toBe('₹10,00,000');
  });
});
