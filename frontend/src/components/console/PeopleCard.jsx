import { useState } from 'react'
import { useWorkspace } from '../../state/context.js'
import { Tabs } from '../ui/Tabs.jsx'
import { ChecksList } from './ChecksList.jsx'
import { PeopleList } from './PeopleList.jsx'

/** Everyone in the rings, one tab at a time, plus the data checks. */
export function PeopleCard({ ctx, onHover, onSelect }) {
  const { network, people, cases, ring } = useWorkspace()
  const [tab, setTab] = useState('ring1')
  const ring1 = ring?.rings[0] ?? []
  const deeper = ring?.rings.slice(1).flat() ?? []
  const cannot = ring?.cannotVaccinate ?? []
  const checks = network.warnings.length + network.qualityWarnings.length
  const waiting = cases.length === 0 ? 'Mark a confirmed case to see the rings.' : 'Building rings…'

  const tabs = [
    { value: 'ring1', label: 'Ring 1', count: ring ? ring1.length : null, dot: 'var(--ring1)' },
    { value: 'ring2', label: 'Ring 2+', count: ring ? deeper.length : null, dot: 'var(--ring2)' },
    { value: 'cannot', label: 'Excluded', count: ring ? cannot.length : null, dot: 'var(--excluded)' },
  ]
  if (checks > 0) tabs.push({ value: 'checks', label: 'Checks', count: checks, dot: 'var(--warning)' })

  return (
    <section className="glass flex min-h-72 flex-1 flex-col gap-2 rounded-[20px] p-4 lg:min-h-0" aria-label="People in the rings">
      <h2 className="text-[15px] font-semibold tracking-tight">People in the rings</h2>
      <Tabs label="Rings" tabs={tabs} value={tab} onChange={setTab} />
      {tab === 'ring1' && (
        <PeopleList ids={ring1} people={people} ctx={ctx} onHover={onHover} onSelect={onSelect} empty={ring ? 'Nobody in ring 1.' : waiting} />
      )}
      {tab === 'ring2' && (
        <PeopleList ids={deeper} people={people} ctx={ctx} onHover={onHover} onSelect={onSelect} empty={ring ? 'Nobody in the outer rings.' : waiting} />
      )}
      {tab === 'cannot' && (
        <>
          <p className="px-1 text-xs leading-relaxed text-muted">
            Ineligible or refused. Their links are covered through the other person, or listed for quarantine.
          </p>
          <PeopleList ids={cannot} people={people} ctx={ctx} onHover={onHover} onSelect={onSelect} empty={ring ? 'Everyone in the rings can be vaccinated.' : waiting} />
        </>
      )}
      {tab === 'checks' && <ChecksList warnings={network.warnings} quality={network.qualityWarnings} />}
    </section>
  )
}
