import { defineConfig } from 'wxt';

// See docs/adr/0007-extension-tooling.md for why WXT over CRXJS.
export default defineConfig({
  srcDir: '.',
  manifest: {
    name: 'JobLens',
    description:
      'Decodes job postings: real vs. claimed seniority, red flags, and visa/relocation likelihood.',
    permissions: ['storage', 'sidePanel', 'tabs'],
    action: {},
  },
});
