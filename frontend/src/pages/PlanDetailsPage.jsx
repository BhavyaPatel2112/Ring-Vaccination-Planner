import { ArrowDownUp, Search, ShieldCheck, ShieldQuestion, Syringe, TriangleAlert, Wallet } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router'
import { checkRegistry } from '../api/endpoints.js'
import { NeedCases, Page, Pill } from '../components/shared/Page.jsx'
import { Button } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Empty, Notice } from '../components/ui/Notice.jsx'
import { Segmented } from '../components/ui/Segmented.jsx'
import { Slider } from '../components/ui/Slider.jsx'
import { StatTile } from '../components/ui/StatTile.jsx'
import { formatNumber, plural } from '../lib/format.js'
import { METHOD_NAMES, REASONS } from '../lib/labels.js'
import { useWorkspace } from '../state/context.js'

/** Every dose with its reason, the dose budget, the double-dose guard and what is left unprotected. */
export function PlanDetailsPage() {
  const { network, cases, ring, plan, method, setMethod, createPlan, busy, restoring } = useWorkspace()
  const [budgetOn, setBudgetOn] = useState(false)
  const [budget, setBudget] = useState(10)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState('order')
  const [guard, setGuard] = useState(null)

  const full = plan?.dosesForFullPlan ?? 0

  // Double-dose guard: everyone in the plan plus the ring members recorded as vaccinated.
  useEffect(() => {
    if (!network || !plan || !ring) return
    const ids = [...new Set([...plan.people.map((p) => p.personId), ...ring.alreadyVaccinated])]
    if (ids.length === 0) return
    let live = true
    checkRegistry(network.networkId, ids)
      .then((result) => live && setGuard(result))
      .catch(() => live && setGuard(null))
    return () => {
      live = false
    }
  }, [network, plan, ring])

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    const list = (plan?.people ?? []).map((p, i) => ({ ...p, order: i + 1 }))
    const filtered = q
      ? list.filter((p) => [p.personId, p.name, p.explanation, REASONS[p.reason]?.label].some((v) => v?.toLowerCase().includes(q)))
      : list
    const by = {
      order: (a, b) => a.order - b.order,
      name: (a, b) => (a.name || a.personId).localeCompare(b.name || b.personId),
      reason: (a, b) => a.reason.localeCompare(b.reason) || a.order - b.order,
    }
    return [...filtered].sort(by[sort])
  }, [plan, query, sort])

  if (restoring) return null
  if (!network) return <Navigate to="/load" replace />
  if (cases.length === 0) {
    return (
      <Page title="Plan details" icon={ShieldCheck}>
        <NeedCases what="the plan" />
      </Page>
    )
  }

  const generate = () => createPlan(budgetOn ? { budget } : {}).catch(() => {})
  const skipped = guard?.results.filter((r) => r.action === 'SKIP') ?? []

  return (
    <Page
      title="Plan details"
      subtitle={`${network.name} · ${plural(cases.length, 'confirmed case')} · ring depth ${ring?.depth ?? '–'}`}
      icon={ShieldCheck}
    >
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[340px_minmax(0,1fr)]">
        <div className="flex flex-col gap-4">
          <GlassPanel>
            <PanelTitle icon={Syringe} title="Method and budget" subtitle="Change either, then recalculate" />
            <Segmented
              size="sm"
              label="Method"
              className="mb-4 w-full justify-between"
              value={method}
              onChange={setMethod}
              options={[
                { value: 'APPROX', label: 'Approx.', ariaLabel: '2-approximation' },
                { value: 'GREEDY', label: 'Greedy' },
                { value: 'EXACT', label: 'Exact' },
              ]}
            />
            <label className="mb-3 flex items-center gap-2 text-sm font-semibold">
              <input type="checkbox" className="size-4 accent-[var(--accent)]" checked={budgetOn} onChange={(e) => setBudgetOn(e.target.checked)} />
              Limit the number of doses
            </label>
            {budgetOn && (
              <div className="mb-4">
                <Slider
                  label="Doses available"
                  value={budget}
                  onChange={setBudget}
                  min={0}
                  max={Math.max(full || 50, budget, 1)}
                  format={(v) => (full && v >= full ? `${v} (enough for all)` : `${v}`)}
                />
                <p className="mt-2 text-xs text-muted">
                  With too few doses: ring 1 first (most connected first), then the top 10% most connected people, found
                  with randomized Quickselect.
                </p>
              </div>
            )}
            <Button variant="primary" className="w-full" loading={busy === 'plan'} disabled={!ring} onClick={generate}>
              {plan ? 'Recalculate plan' : 'Generate plan'}
            </Button>
          </GlassPanel>

          {plan && (
            <GlassPanel delay={0.04}>
              <PanelTitle icon={ShieldCheck} title={METHOD_NAMES[plan.method]} subtitle={`${plan.runtimeMs.toFixed(2)} ms in Java`} />
              <div className="grid grid-cols-2 gap-3">
                <StatTile label="Doses" value={plan.doses} tone="var(--planned)" />
                <StatTile label="Full plan" value={plan.dosesForFullPlan} />
                <StatTile label="Links to protect" value={plan.targetLinks} tone="var(--ring2)" />
                <StatTile label="Left unprotected" value={plan.unprotectedLinks.length} tone="var(--danger)" />
              </div>
              <p className="mt-3 text-xs leading-relaxed text-muted">{plan.guarantee}</p>
              {plan.contactFilter && (
                <p className="mt-2 text-xs text-faint">
                  Contact settings left out {plan.contactFilter.total - plan.contactFilter.kept} of{' '}
                  {formatNumber(plan.contactFilter.total)} contacts.
                </p>
              )}
            </GlassPanel>
          )}

          {plan && (
            <GlassPanel delay={0.08}>
              <PanelTitle icon={ShieldQuestion} title="Double-dose guard" subtitle="Bloom filter first, then the exact registry" />
              {!guard && <p className="text-sm text-muted">Checking the registry…</p>}
              {guard && (
                <div className="flex flex-col gap-3 text-xs">
                  <div className="grid grid-cols-3 gap-2">
                    <StatTile label="Checked" value={guard.results.length} />
                    <StatTile label="Possibly" value={guard.quickCheckHits} tone="var(--warning)" />
                    <StatTile label="Confirmed" value={guard.alreadyVaccinated} tone="var(--vaccinated)" />
                  </div>
                  {skipped.length > 0 ? (
                    <div>
                      <p className="mb-1.5 font-semibold text-text">Skipped: already vaccinated</p>
                      <div className="flex flex-wrap gap-1.5">
                        {skipped.map((r) => (
                          <Pill key={r.personId} color="var(--vaccinated)">
                            {r.personId}
                          </Pill>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="text-muted">Nobody in or around the plan is already vaccinated.</p>
                  )}
                  <p className="text-muted">
                    {guard.falsePositives === 0
                      ? 'Every "possibly vaccinated" answer was confirmed by the registry.'
                      : `${plural(guard.falsePositives, 'false alarm')} from the filter, cleared by the registry check.`}{' '}
                    Nobody in the plan is already vaccinated, so no one gets a second dose.
                  </p>
                  <p className="text-faint">
                    Filter: {formatNumber(guard.filter.bits)} bits, k = {guard.filter.hashes}, {formatNumber(guard.filter.keys)} ids,
                    expected false positive rate {(guard.filter.expectedFalsePositiveRate * 100).toFixed(2)}%.
                  </p>
                </div>
              )}
            </GlassPanel>
          )}
        </div>

        <div className="flex min-w-0 flex-col gap-4">
          <GlassPanel>
            <PanelTitle
              icon={Syringe}
              title="Doses in priority order"
              subtitle="Every dose has a reason"
              action={
                plan && (
                  <Segmented
                    size="sm"
                    label="Sort"
                    value={sort}
                    onChange={setSort}
                    options={[
                      { value: 'order', label: 'Priority' },
                      { value: 'name', label: 'Name' },
                      { value: 'reason', label: 'Reason' },
                    ]}
                  />
                )
              }
            />
            {!plan ? (
              <Empty icon={ArrowDownUp} title="No plan yet">
                Choose a method and generate the plan.
              </Empty>
            ) : (
              <>
                <label className="inset mb-3 flex h-10 items-center gap-2 rounded-xl px-3">
                  <Search className="size-4 text-faint" />
                  <input
                    className="w-full bg-transparent text-sm outline-none placeholder:text-faint"
                    placeholder="Search by name, id or reason"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </label>
                <div className="scroll-thin max-h-[60vh] overflow-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="sticky top-0 bg-[var(--glass-strong)] text-xs text-muted backdrop-blur">
                      <tr>
                        <th className="py-2 pr-3 font-semibold">#</th>
                        <th className="py-2 pr-3 font-semibold">Person</th>
                        <th className="py-2 pr-3 font-semibold">Reason</th>
                        <th className="py-2 font-semibold">Why</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((p) => (
                        <tr key={p.personId} className="border-t border-[var(--glass-border)] align-top">
                          <td className="py-2 pr-3 text-faint">{p.order}</td>
                          <td className="py-2 pr-3">
                            <p className="font-semibold">{p.name || p.personId}</p>
                            <p className="text-xs text-muted">{p.personId}</p>
                          </td>
                          <td className="py-2 pr-3">
                            <Pill color={REASONS[p.reason]?.color}>{REASONS[p.reason]?.label ?? p.reason}</Pill>
                          </td>
                          <td className="py-2 text-xs text-muted">{p.explanation}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {rows.length === 0 && <p className="py-6 text-center text-sm text-muted">No dose matches the search.</p>}
                </div>
              </>
            )}
          </GlassPanel>

          {plan && (plan.unprotectedLinks.length > 0 || plan.unprotectableLinks.length > 0 || plan.ring1NotVaccinated.length > 0) && (
            <GlassPanel delay={0.05}>
              <PanelTitle icon={TriangleAlert} title="Not protected by this plan" subtitle="Plan quarantine or monitoring for these" />
              <div className="flex flex-col gap-3">
                {plan.ring1NotVaccinated.length > 0 && (
                  <Notice tone="warning" title={`${plan.ring1NotVaccinated.length} ring 1 contacts left out by the budget`}>
                    {plan.ring1NotVaccinated.join(', ')}
                  </Notice>
                )}
                {plan.unprotectedLinks.length > 0 && (
                  <Notice tone="warning" title={`${plan.unprotectedLinks.length} links unprotected because the budget ran out`}>
                    <LinkList links={plan.unprotectedLinks} />
                  </Notice>
                )}
                {plan.unprotectableLinks.length > 0 && (
                  <Notice tone="error" title={`${plan.unprotectableLinks.length} links cannot be protected by vaccination`}>
                    Neither person on these links can be vaccinated: <LinkList links={plan.unprotectableLinks} />
                  </Notice>
                )}
              </div>
            </GlassPanel>
          )}
          {plan && budgetOn && plan.budgetLimited && plan.unprotectedLinks.length === 0 && (
            <Notice tone="info" title="Budget is enough">
              <Wallet className="mr-1 inline size-3" /> Every link is still protected with {plan.doses} doses.
            </Notice>
          )}
        </div>
      </div>
    </Page>
  )
}

function LinkList({ links }) {
  const shown = links.slice(0, 40)
  return (
    <span>
      {shown.map((l) => `${l.a}–${l.b}`).join(', ')}
      {links.length > shown.length && ` and ${links.length - shown.length} more`}
    </span>
  )
}
