import type { SiteAdapter } from './SiteAdapter';
import { linkedInAdapter } from './LinkedInAdapter';

export const siteAdapters: readonly SiteAdapter[] = [linkedInAdapter];

export function findAdapterFor(url: string): SiteAdapter | undefined {
  return siteAdapters.find((adapter) => adapter.matches(url));
}
