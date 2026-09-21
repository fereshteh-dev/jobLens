import type { AnalyzeResponseDto, VerdictDto } from './api';

/**
 * Pure formatting of what the backend already decided - no thresholds, no gate logic, no
 * classification happens here. Just turns the response into HTML for display.
 */
export function renderResult(result: AnalyzeResponseDto): string {
  return [
    renderSeniorityCard(result),
    renderRedFlagsCard(result),
    renderSponsorshipCard(result),
    renderGateCard(result),
    result.talkingPoints ? renderTalkingPointsCard(result.talkingPoints) : '',
  ].join('');
}

function renderSeniorityCard(result: AnalyzeResponseDto): string {
  const seniority = result.seniority;
  return `
    <div class="card">
      <h2>Seniority</h2>
      <p>Claimed: <strong>${escapeHtml(formatLabel(seniority.claimed))}</strong> &middot;
         Actual: <strong>${escapeHtml(formatLabel(seniority.actual))}</strong></p>
      <p>Title match: ${verdictBadge(seniority.titleVsReality)}</p>
    </div>`;
}

function renderRedFlagsCard(result: AnalyzeResponseDto): string {
  const items = result.redFlags
    .map((flag) => `<li>${escapeHtml(formatLabel(flag.kind))}: ${verdictBadge(flag.verdict)}</li>`)
    .join('');
  return `
    <div class="card">
      <h2>Red flags</h2>
      <ul>${items}</ul>
    </div>`;
}

function renderSponsorshipCard(result: AnalyzeResponseDto): string {
  return `
    <div class="card">
      <h2>Sponsorship</h2>
      <p>Visa: <strong>${escapeHtml(formatLabel(result.sponsorship.visa))}</strong></p>
      <p>Relocation: <strong>${escapeHtml(formatLabel(result.sponsorship.relocation))}</strong></p>
    </div>`;
}

function renderGateCard(result: AnalyzeResponseDto): string {
  const gate = result.gate;
  const reasons =
    gate.reasons.length > 0
      ? `<ul>${gate.reasons.map((reason) => `<li>${escapeHtml(reason)}</li>`).join('')}</ul>`
      : '';
  return `
    <div class="card">
      <h2>Gate</h2>
      <p><span class="badge ${gate.passed ? 'passed' : 'failed'}">${gate.passed ? 'Passed' : 'Not a match'}</span></p>
      ${reasons}
    </div>`;
}

function renderTalkingPointsCard(talkingPoints: NonNullable<AnalyzeResponseDto['talkingPoints']>): string {
  return `
    <div class="card">
      <h2>Talking points</h2>
      <p><strong>Cover letter angles</strong></p>
      <ul>${listItems(talkingPoints.coverLetterAngles)}</ul>
      <p><strong>Questions to ask</strong></p>
      <ul>${listItems(talkingPoints.questionsToAsk)}</ul>
      <p><strong>Why it fits</strong></p>
      <ul>${listItems(talkingPoints.whyItFits)}</ul>
    </div>`;
}

function listItems(items: string[]): string {
  return items.map((item) => `<li>${escapeHtml(item)}</li>`).join('');
}

function verdictBadge(verdict: VerdictDto): string {
  const cssClass = verdict.kind.toLowerCase();
  const label =
    verdict.probability !== undefined
      ? `${verdict.kind} (${Math.round(verdict.probability * 100)}%)`
      : verdict.kind;
  return `<span class="badge ${cssClass}">${escapeHtml(label)}</span>`;
}

function formatLabel(value: string): string {
  return value.toLowerCase().replace(/_/g, ' ');
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
