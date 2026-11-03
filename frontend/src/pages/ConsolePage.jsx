import { AnimatePresence, motion } from 'motion/react'
import { Activity, CircleDot, Network as NetworkIcon } from 'lucide-react'
import { useCallback, useMemo, useRef, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router'
import { CasesContent } from '../components/console/CasesSection.jsx'
import { ChecksList } from '../components/console/ChecksList.jsx'
import { PeopleList } from '../components/console/PeopleList.jsx'
import { PersonCard } from '../components/console/PersonCard.jsx'
import { PlanCard } from '../components/console/PlanCard.jsx'
import { RingChart } from '../components/console/RingChart.jsx'
import { Callouts } from '../components/network/Callouts.jsx'
import { CurvedGrid } from '../components/network/CurvedGrid.jsx'
import { Legend } from '../components/network/Legend.jsx'
import { NetworkCanvas } from '../components/network/NetworkCanvas.jsx'
import { GlassPanel, Grabber, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Notice } from '../components/ui/Notice.jsx'
import { Section, SectionGroup } from '../components/ui/Section.jsx'
import { StatTile } from '../components/ui/StatTile.jsx'
import { formatNumber } from '../lib/format.js'
import { useWorkspace } from '../state/context.js'

/**
 * The outbreak console: confirmed cases and rings on the left, the contact
 * network in the middle, numbers and the plan on the right.
 */
export function ConsolePage() {
  const { network, view, people, cases, ring, plan, restoring } = useWorkspace()
  const [params] = useSearchParams()
  const focus = params.get('focus') === '1'
  const canvas = useRef(null)
  const [selected, setSelected] = useState(null)
  const [hovered, setHovered] = useState(null)

  const ctx = useMemo(() => {
    const level = new Map()
    ring?.rings.forEach((members, i) => members.forEach((id) => level.set(id, i + 1)))
    return { level, caseSet: new Set(cases), planned: new Set(plan?.people.map((p) => p.personId)) }
  }, [ring, cases, plan])

  const select = useCallback((id) => setSelected(id), [])
  const hover = useCallback((id) => setHovered(id), [])

  const links = useMemo(() => {
    const result = cases.map((id) => ({ key: `case-${id}`, nodeId: id, color: 'var(--case)' }))
    if (selected) result.push({ key: 'selected', nodeId: selected, color: 'var(--accent)' })
    if (hovered && !cases.includes(hovered)) result.push({ key: `row-${hovered}`, nodeId: hovered, color: 'var(--accent)' })
    return result
  }, [cases, selected, hovered])

  if (restoring) return null
  if (!network || !view) return <Navigate to="/load" replace />

  const ring1 = ring?.rings[0] ?? []
  const deeper = ring?.rings.slice(1).flat() ?? []
  const checks = network.warnings.length + network.qualityWarnings.length

  return (
    <div className="grid flex-1 grid-cols-1 gap-4 px-4 pt-4 lg:h-[calc(100dvh-232px)] lg:flex-none lg:grid-cols-[300px_minmax(0,1fr)_300px] xl:grid-cols-[330px_minmax(0,1fr)_330px]">
      <AnimatePresence>
        {!focus && (
          <motion.aside
            key="left"
            initial={{ opacity: 0, x: -24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -24 }}
            className="scroll-thin relative z-20 flex flex-col gap-2 overflow-y-auto lg:pr-1"
            aria-label="Cases and rings"
          >
            <SectionGroup defaultOpen={['cases', 'ring1', 'checks']}>
              <Section value="cases" title="Confirmed cases" count={cases.length} dot="var(--case)">
                <CasesContent onHover={hover} onSelect={select} />
              </Section>
              <Section value="ring1" title="Ring 1 · direct contacts" count={ring ? ring1.length : '–'} dot="var(--ring1)">
                <PeopleList
                  ids={ring1}
                  people={people}
                  ctx={ctx}
                  onHover={hover}
                  onSelect={select}
                  empty={cases.length ? 'Building rings…' : 'Mark a confirmed case to see its contacts.'}
                />
              </Section>
              <Section value="ring2" title="Ring 2 and beyond" count={ring ? deeper.length : '–'} dot="var(--ring2)">
                <PeopleList ids={deeper} people={people} ctx={ctx} onHover={hover} onSelect={select} empty="Nobody in the outer rings yet." />
              </Section>
              <Section
                value="cannot"
                title="Cannot be vaccinated"
                count={ring ? ring.cannotVaccinate.length : '–'}
                dot="var(--excluded)"
              >
                <p className="px-1 pb-2 text-[11px] text-muted">
                  Ineligible or refused. Their links are protected through the other person, or listed for quarantine.
                </p>
                <PeopleList
                  ids={ring?.cannotVaccinate ?? []}
                  people={people}
                  ctx={ctx}
                  onHover={hover}
                  onSelect={select}
                  empty="Everyone in the rings can be vaccinated."
                />
              </Section>
              <Section value="checks" title="Data checks" count={checks} dot="var(--warning)">
                <ChecksList warnings={network.warnings} quality={network.qualityWarnings} />
              </Section>
            </SectionGroup>
            <Grabber />
          </motion.aside>
        )}
      </AnimatePresence>

      <div
        className={`relative order-first min-h-[60vh] overflow-hidden rounded-[28px] lg:order-none lg:min-h-0 ${focus ? 'lg:col-span-3' : ''}`}
      >
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
        />
        <Legend />
        {view.sampled && (
          <div className="absolute top-3 left-1/2 w-max max-w-[90%] -translate-x-1/2">
            <Notice tone="info">
              Showing {formatNumber(view.nodes.length)} of {formatNumber(view.totalPeople)} people around the cases. The
              plan always uses the full network.
            </Notice>
          </div>
        )}
      </div>

      <AnimatePresence>
        {!focus && (
          <motion.aside
            key="right"
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 24 }}
            className="scroll-thin relative z-20 flex flex-col gap-1 overflow-y-auto lg:pl-1"
            aria-label="Outbreak numbers and plan"
          >
            <PersonCard
              personId={selected}
              ctx={ctx}
              onClose={() => setSelected(null)}
              onFocus={(id) => canvas.current?.focusOn(id)}
            />
            <GlassPanel delay={0.05} grabber>
              <PanelTitle icon={Activity} title="Outbreak" subtitle={network.name} />
              <div className="grid grid-cols-2 gap-x-4 gap-y-3">
                <StatTile label="People" value={network.people} />
                <StatTile label="Contacts" value={network.contacts} />
                <StatTile label="Confirmed cases" value={cases.length} tone="var(--case)" />
                <StatTile label="Links to protect" value={ring ? ring.targetLinks.length : null} tone="var(--ring2)" />
              </div>
            </GlassPanel>
            <PlanCard delay={0.1} />
            <GlassPanel delay={0.15} grabber>
              <PanelTitle
                icon={ring ? CircleDot : NetworkIcon}
                title="Rings"
                subtitle={ring ? `Depth ${ring.depth} around ${ring.cases.length} case${ring.cases.length === 1 ? '' : 's'}` : 'No cases marked yet'}
              />
              {ring ? (
                <RingChart ring={ring} />
              ) : (
                <p className="text-xs text-muted">Rings appear here as soon as a confirmed case is marked.</p>
              )}
            </GlassPanel>
          </motion.aside>
        )}
      </AnimatePresence>

      <Callouts canvas={canvas} links={focus ? [] : links} />
    </div>
  )
}
