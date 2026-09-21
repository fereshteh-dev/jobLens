import { JSDOM } from 'jsdom';
import { describe, expect, it } from 'vitest';
import { linkedInAdapter } from './LinkedInAdapter';

describe('linkedInAdapter.matches', () => {
  it('matches a LinkedIn jobs URL', () => {
    expect(linkedInAdapter.matches('https://www.linkedin.com/jobs/view/1234567')).toBe(true);
  });

  it('does not match a non-jobs LinkedIn URL', () => {
    expect(linkedInAdapter.matches('https://www.linkedin.com/in/someone')).toBe(false);
  });

  it('does not match a different site', () => {
    expect(linkedInAdapter.matches('https://www.indeed.com/jobs/view/1234567')).toBe(false);
  });
});

describe('linkedInAdapter.extract', () => {
  function docWithHtml(bodyHtml: string, url = 'https://www.linkedin.com/jobs/view/1234567'): Document {
    return new JSDOM(`<html><body>${bodyHtml}</body></html>`, { url }).window.document;
  }

  it('extracts title, company, location and description when all are present', () => {
    const doc = docWithHtml(`
      <h1 class="job-details-jobs-unified-top-card__job-title">Senior Backend Engineer</h1>
      <a class="job-details-jobs-unified-top-card__company-name">Acme Corp</a>
      <div class="job-details-jobs-unified-top-card__primary-description-container">Remote</div>
      <div id="job-details">Own our payments platform end to end.</div>
    `);

    const posting = linkedInAdapter.extract(doc);

    expect(posting).toEqual({
      url: 'https://www.linkedin.com/jobs/view/1234567',
      title: 'Senior Backend Engineer',
      company: 'Acme Corp',
      location: 'Remote',
      descriptionText: 'Own our payments platform end to end.',
    });
  });

  it('returns null when the title is missing', () => {
    const doc = docWithHtml(`<div id="job-details">Some description.</div>`);
    expect(linkedInAdapter.extract(doc)).toBeNull();
  });

  it('returns null when the description is missing', () => {
    const doc = docWithHtml(`<h1>Senior Backend Engineer</h1>`);
    expect(linkedInAdapter.extract(doc)).toBeNull();
  });

  it('falls back to empty strings for company/location when only those are missing', () => {
    const doc = docWithHtml(`
      <h1>Senior Backend Engineer</h1>
      <div id="job-details">Own our payments platform end to end.</div>
    `);

    const posting = linkedInAdapter.extract(doc);

    expect(posting?.company).toBe('');
    expect(posting?.location).toBe('');
  });
});
