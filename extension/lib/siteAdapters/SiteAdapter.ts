import type { JobPostingData } from '../jobPosting';

/**
 * One adapter per supported job site. `extract` reads ONLY the current page's DOM, on user
 * action (called from the content script only in response to an explicit "analyze" click in
 * the side panel) - never on page load, never across multiple postings. No business logic
 * here: this is DOM extraction only, not classification (that's the backend's job).
 */
export interface SiteAdapter {
  readonly name: string;
  matches(url: string): boolean;
  extract(doc: Document): JobPostingData | null;
}
