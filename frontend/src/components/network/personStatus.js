/**
 * One status per person, in order of importance, with its colour token.
 * `ctx` holds the case set, ring levels and planned people.
 */
export function statusOf(person, ctx) {
  if (ctx.caseSet.has(person.id)) return { key: 'case', label: 'Confirmed case', color: 'var(--case)' }
  if (person.vaccinated) return { key: 'vaccinated', label: 'Already vaccinated', color: 'var(--vaccinated)' }
  if (!person.eligible) return { key: 'ineligible', label: 'Cannot be vaccinated', color: 'var(--excluded)' }
  if (person.refused) return { key: 'refused', label: 'Refused vaccination', color: 'var(--excluded)' }
  if (ctx.planned.has(person.id)) return { key: 'planned', label: 'Dose in the plan', color: 'var(--planned)' }
  const level = ctx.level.get(person.id)
  if (level === 1) return { key: 'ring1', label: 'Ring 1: direct contact', color: 'var(--ring1)' }
  if (level) return { key: 'ring2', label: `Ring ${level}: contact of a contact`, color: 'var(--ring2)' }
  return { key: 'outside', label: 'Outside the rings', color: 'var(--text-faint)' }
}

export function describePerson(person, ctx) {
  return statusOf(person, ctx).label
}
