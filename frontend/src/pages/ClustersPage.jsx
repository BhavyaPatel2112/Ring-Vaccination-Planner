import { useAutoAnimate } from '@formkit/auto-animate/react'
import { Link2, Network, UsersRound, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router'
import { toast } from 'sonner'
import { addContact, getClusters } from '../api/endpoints.js'
import { CurvedGrid } from '../components/network/CurvedGrid.jsx'
import { NetworkCanvas } from '../components/network/NetworkCanvas.jsx'
import { Field, Page, Pill } from '../components/shared/Page.jsx'
import { Button } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Notice } from '../components/ui/Notice.jsx'
import { StatTile } from '../components/ui/StatTile.jsx'
import { cn } from '../lib/cn.js'
import { formatNumber, plural } from '../lib/format.js'
import { useWorkspace } from '../state/context.js'

/**
 * Outbreak clusters from the disjoint-set union: who is connected to whom by a
 * chain of contacts. A new contact report merges clusters straight away.
 */
export function ClustersPage() {
  const { network, view, cases, ring, plan, refresh, restoring } = useWorkspace()
  const [data, setData] = useState(null)
  const [active, setActive] = useState(null)
  const [form, setForm] = useState({ personA: '', personB: '', date: '', durationMinutes: '' })
  const [saving, setSaving] = useState(false)
  const [last, setLast] = useState(null)
  const [list] = useAutoAnimate()

  const load = useCallback(() => {
    if (!network) return Promise.resolve()
    return getClusters(network.networkId, cases)
      .then(setData)
      .catch((e) => toast.error('Could not read the clusters', { description: e.message }))
  }, [network, cases])

  useEffect(() => {
    load()
  }, [load])

  const highlight = useMemo(() => {
    const cluster = data?.clusters.find((c) => c.clusterId === active)
    return cluster && !cluster.membersOmitted ? new Set(cluster.members) : null
  }, [data, active])

  if (restoring) return null
  if (!network || !view) return <Navigate to="/load" replace />

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      const body = {
        personA: form.personA.trim(),
        personB: form.personB.trim(),
        ...(form.date && { date: form.date }),
        ...(form.durationMinutes !== '' && { durationMinutes: Number(form.durationMinutes) }),
      }
      const result = await addContact(network.networkId, body)
      setLast(result)
      toast.success(result.merged ? 'Clusters merged' : 'Contact added', {
        description: `${result.message}. Cluster update took ${result.runtimeMicros.toFixed(1)} µs.`,
      })
      setForm({ personA: '', personB: '', date: '', durationMinutes: '' })
      await Promise.all([load(), refresh()])
    } catch (e) {
      toast.error('Contact not added', { description: e.message })
    } finally {
      setSaving(false)
    }
  }

  const largest = data?.clusters.reduce((m, c) => Math.max(m, c.size), 0) ?? 0
  const withCases = data?.clusters.filter((c) => c.cases > 0).length ?? 0

  return (
    <Page
      title="Outbreak clusters"
      subtitle="Groups of people linked by a chain of contacts, kept in a disjoint-set union"
      icon={UsersRound}
      width="max-w-[1600px]"
    >
      <div className="grid grid-cols-1 gap-4 lg:h-[calc(100dvh-180px)] lg:grid-cols-[360px_minmax(0,1fr)]">
        <div className="scroll-thin flex flex-col gap-4 lg:min-h-0 lg:overflow-y-auto">
          <GlassPanel>
            <div className="grid grid-cols-3 gap-3">
              <StatTile label="Clusters" value={data?.clusterCount} />
              <StatTile label="Largest" value={largest} tone="var(--accent)" />
              <StatTile label="With cases" value={withCases} tone="var(--case)" />
            </div>
            {data?.isolatedPeople > 0 && (
              <p className="mt-2 text-xs text-muted">{plural(data.isolatedPeople, 'person', 'people')} with no contacts are not listed.</p>
            )}
          </GlassPanel>

          <GlassPanel delay={0.04}>
            <PanelTitle icon={Link2} title="Report a new contact" subtitle="Joins two clusters in near-constant time" />
            <form onSubmit={submit} className="grid grid-cols-2 gap-3">
              <Field label="Person A" placeholder="P001" value={form.personA} onChange={(e) => setForm({ ...form, personA: e.target.value })} required />
              <Field label="Person B" placeholder="P040" value={form.personB} onChange={(e) => setForm({ ...form, personB: e.target.value })} required />
              <Field label="Date" type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
              <Field
                label="Minutes"
                type="number"
                min="0"
                placeholder="30"
                value={form.durationMinutes}
                onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })}
              />
              <Button type="submit" variant="primary" className="col-span-2" loading={saving}>
                Add contact
              </Button>
            </form>
            {last && (
              <Notice tone="info" className="mt-3" title={last.merged ? 'Two clusters merged' : 'Same cluster already'}>
                {last.message}. Now {plural(last.clusterCount, 'cluster')} and {formatNumber(last.contacts)} contacts. The
                rings and plans use the new contact straight away.
              </Notice>
            )}
          </GlassPanel>

          <GlassPanel delay={0.08}>
            <PanelTitle
              icon={Network}
              title="Clusters"
              subtitle="Cases first, then largest. Click one to highlight it."
              action={active && <Button size="sm" variant="ghost" icon={X} onClick={() => setActive(null)}>Clear</Button>}
            />
            <ul ref={list} className="flex flex-col gap-2">
              {data?.clusters.map((c, i) => (
                <li key={c.clusterId}>
                  <button
                    type="button"
                    onClick={() => setActive(active === c.clusterId ? null : c.clusterId)}
                    className={cn(
                      'inset flex w-full items-center gap-3 rounded-xl p-3 text-left transition hover:border-accent',
                      active === c.clusterId && 'border-accent bg-accent-soft',
                    )}
                  >
                    <span className="grid size-8 shrink-0 place-items-center rounded-full bg-inset text-xs font-semibold text-muted">{i + 1}</span>
                    <span className="min-w-0 flex-1">
                      <span className="block text-sm font-semibold">{plural(c.size, 'person', 'people')}</span>
                      <span className="block truncate text-xs text-muted">
                        {c.cases > 0 ? `Cases: ${c.caseIds.join(', ')}` : 'No confirmed case'}
                      </span>
                    </span>
                    <span className="flex flex-col items-end gap-1">
                      {c.cases > 0 && <Pill color="var(--case)">{c.cases} case{c.cases === 1 ? '' : 's'}</Pill>}
                      {c.vaccinated > 0 && <Pill color="var(--vaccinated)">{c.vaccinated} vaccinated</Pill>}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {data?.clustersOmitted > 0 && <p className="mt-2 text-xs text-muted">and {plural(data.clustersOmitted, 'smaller cluster')}.</p>}
            {data && data.clusters.length === 0 && <p className="text-sm text-muted">Nobody has a contact yet.</p>}
          </GlassPanel>
        </div>

        <div className="glass relative min-h-[60vh] overflow-hidden rounded-[24px] lg:min-h-0">
          <CurvedGrid />
          <NetworkCanvas view={view} ring={ring} plan={plan} cases={cases} highlight={highlight} />
          {active && !highlight && (
            <div className="absolute top-3 left-3 max-w-sm">
              <Notice tone="info">This cluster is too large to highlight; the map shows a sample around the cases.</Notice>
            </div>
          )}
          {view.sampled && (
            <span className="glass glass-strong absolute top-3 right-3 rounded-full px-3 py-1.5 text-xs font-semibold">
              Showing {formatNumber(view.nodes.length)} nearest people
            </span>
          )}
        </div>
      </div>
    </Page>
  )
}
