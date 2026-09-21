import type { JobPostingData } from '../jobPosting';
import type { SiteAdapter } from './SiteAdapter';

/**
 * First-cut LinkedIn adapter. Selectors below are best-effort from LinkedIn's public,
 * commonly-documented class names as of this writing - LinkedIn changes its markup often and
 * these were NOT verified against a live page in this environment. If extraction stops
 * working, this is the one file to update; the SiteAdapter interface exists specifically so
 * that fix never touches the rest of the extension.
 */
export const linkedInAdapter: SiteAdapter = {
  name: 'linkedin',

  matches(url) {
    return /(^|\.)linkedin\.com$/.test(new URL(url).hostname) && /\/jobs\//.test(url);
  },

  extract(doc) {
    const title = firstText(doc, [
      'h1.job-details-jobs-unified-top-card__job-title',
      'h1.t-24',
      '.jobs-unified-top-card__job-title',
      'h1',
    ]);
    const company = firstText(doc, [
      '.job-details-jobs-unified-top-card__company-name a',
      '.job-details-jobs-unified-top-card__company-name',
      '.jobs-unified-top-card__company-name',
    ]);
    const location = firstText(doc, [
      '.job-details-jobs-unified-top-card__primary-description-container',
      '.jobs-unified-top-card__bullet',
      '.jobs-unified-top-card__subtitle-primary-grouping',
    ]);
    const descriptionText = firstText(doc, [
      '#job-details',
      '.jobs-description__content',
      '.jobs-box__html-content',
    ]);

    if (!title || !descriptionText) {
      return null;
    }

    const posting: JobPostingData = {
      url: doc.location.href,
      title,
      company: company ?? '',
      location: location ?? '',
      descriptionText,
    };
    return posting;
  },
};

function firstText(doc: Document, selectors: string[]): string | null {
  for (const selector of selectors) {
    const text = doc.querySelector(selector)?.textContent?.trim();
    if (text) {
      return text;
    }
  }
  return null;
}
