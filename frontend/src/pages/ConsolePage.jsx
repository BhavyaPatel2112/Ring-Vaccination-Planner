import { CircleDot } from 'lucide-react'
import { useCallback, useMemo, useRef, useState } from 'react'
import { Navigate } from 'react-router'
import { CasesCard } from '../components/console/CasesCard.jsx'
import { GuideCard } from '../components/console/GuideCard.jsx'
import { PeopleCard } from '../components/console/PeopleCard.jsx'
import { PersonCard } from '../components/console/PersonCard.jsx'
import { PlanCard } from '../components/console/PlanCard.jsx'
import { RingChart } from '../components/console/RingChart.jsx'
import { Callouts } from '../components/network/Callouts.jsx'
import { CurvedGrid } from '../components/network/CurvedGrid.jsx'
import { Legend } from '../components/network/Legend.jsx'
import { NetworkCanvas } from '../components/network/NetworkCanvas.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { formatNumber } from '../lib/format.js'
import { useWorkspace } from '../state/context.js'

/**
 * The outbreak console. Left: what to do next, the cases and the people in the
 * rings. Middle: the contact network. Right: the plan and the ring breakdown.
 */
export function ConsolePage() {
  const { network, view, cases, ring, plan, restoring } = useWorkspace()
  const canvas = useRef(null)
  const [selected, setSelected] = useState(null)
  const [hovered, setHovered] = useState(null)
  const [expanded, setExpanded] = useState(false)

  const ctx = useMemo(() => {
    const level = new Map()
    ring?.rings.forEach((members, i) => members.forEach((id) => level.set(id, i + 1)))
    return { level, caseSet: new Set(cases), planned: new Set(plan?.people.map((p) => p.personId)) }
  }, [ring, cases, plan])

  const select = useCallback((id) => setSelected(id), [])
  const hover = useCallback((id) => setHovered(id), [])

  // One line at a time: to the person being hovered in a list, or to the selected person.
  const links = useMemo(() => {
    if (hovered && hovered !== selected) return [{ key: `row-${hovered}`, nodeId: hovered, color: 'var(--accent)' }]
    if (selected) return [{ key: 'selected', nodeId: selected, color: 'var(--accent)' }]
    return []
  }, [selected, hovered])

  if (restoring) return null
  if (!network || !view) return <Navigate to="/load" replace />

  return (
    <div
      className={`mx-auto grid w-full max-w-[1600px] flex-1 grid-cols-1 gap-4 px-4 pt-4 lg:h-[calc(100dvh-96px)] lg:flex-none ${
        expanded ? '' : 'lg:grid-cols-[300px_minmax(0,1fr)_320px]'
      }`}
    >
      {!expanded && (
        <aside className="scroll-thin relative z-20 flex flex-col gap-3 lg:min-h-0 lg:overflow-y-auto" aria-label="Cases and rings">
          <GuideCard />
          <CasesCard onHover={hover} onSelect={select} />
          <PeopleCard ctx={ctx} onHover={hover} onSelect={select} />
        </aside>
      )}

      <div className="glass relative order-first min-h-[60vh] overflow-hidden rounded-[24px] lg:order-none lg:min-h-0">
        <CurvedGrid />
        <NetworkCanvas
          ref={canvas}
          view={view}
          ring={ring}
          plan={plan}
          cases={cases}
          selected={selected}
          hovered={hovered}
          onSelect={select}
          onHover={hover}
          expanded={expanded}
          onToggleExpand={() => setExpanded((e) => !e)}
        />
        <div className="pointer-events-none absolute top-3 left-3 flex flex-wrap gap-2">
          <Chip>{formatNumber(network.people)} people</Chip>
          <Chip>{formatNumber(network.contacts)} contacts</Chip>
          <Chip tone="var(--case)">{cases.length} cases</Chip>
          {ring && <Chip tone="var(--ring2)">{ring.targetLinks.length} links to protect</Chip>}
          {view.sampled && <Chip>Showing {formatNumber(view.nodes.length)} nearest people</Chip>}
        </div>
        <PersonCard
          personId={selected}
          ctx={ctx}
          onClose={() => setSelected(null)}
          onFocus={(id) => canvas.current?.focusOn(id)}
        />
        <Legend />
      </div>

      {!expanded && (
        <aside className="scroll-thin relative z-20 flex flex-col gap-3 lg:min-h-0 lg:overflow-y-auto" aria-label="Plan">
          <PlanCard />
          <GlassPanel delay={0.05}>
            <PanelTitle
              icon={CircleDot}
              title="Rings"
              subtitle={ring ? `Depth ${ring.depth} around ${ring.cases.length} case${ring.cases.length === 1 ? '' : 's'}` : 'Waiting for a confirmed case'}
            />
            {ring ? <RingChart ring={ring} /> : <p className="text-sm text-muted">The breakdown appears once a case is marked.</p>}
          </GlassPanel>
        </aside>
      )}

      <Callouts canvas={canvas} links={links} />
    </div>
  )
}

function Chip({ children, tone }) {
  return (
    <span className="glass glass-strong flex h-8 items-center gap-1.5 rounded-full px-3 text-xs font-semibold">
      {tone && <span className="size-2 rounded-full" style={{ background: tone }} />}
      {children}
    </span>
  )
}
