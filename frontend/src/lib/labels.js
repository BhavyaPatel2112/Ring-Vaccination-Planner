/** Plain-language labels and colours for the reasons a person is in a plan. */
export const REASONS = {
  RING_1: { label: 'Ring 1', color: 'var(--ring1)' },
  FORCED: { label: 'Only option on a link', color: 'var(--excluded)' },
  MATCHED: { label: 'Matched link', color: 'var(--planned)' },
  GREEDY: { label: 'Most connected', color: 'var(--accent)' },
  EXACT: { label: 'Minimum cover', color: 'var(--vaccinated)' },
  BUDGET_PRIORITY: { label: 'Top 10% connected', color: 'var(--warning)' },
}

export const METHOD_NAMES = { APPROX: '2-approximation', GREEDY: 'Greedy', EXACT: 'Exact' }
