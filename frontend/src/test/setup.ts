// Runs before each test file. Extends Vitest's `expect` with jest-dom matchers
// (toBeInTheDocument, toBeDisabled, ...) and unmounts rendered components after
// each test (auto-cleanup isn't registered when Vitest globals are off).
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
});
