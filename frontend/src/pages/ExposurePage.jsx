import { AnimatePresence, motion } from 'motion/react'
import { CircleCheck, CircleHelp, Lock, MapPin, Plus, ShieldAlert, Sparkles, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Navigate } from 'react-router'
import { toast } from 'sonner'
import { checkExposure } from '../api/endpoints.js'
import { Field, Page, Pill } from '../components/shared/Page.jsx'
import { Button, IconButton } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Segmented } from '../components/ui/Segmented.jsx'
import { formatNumber } from '../lib/format.js'
import { useWorkspace } from '../state/context.js'

const blankVisit = () => ({ venueId: '', date: '', start: '', end: '' })
const dayFormat = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })

/**
 * Public exposure self-check. The Bloom filter gives the quick answer; every
 * "possibly exposed" is checked against the exact records before the final answer.
 */
export function ExposurePage() {
  const { network, restoring } = useWorkspace()
  const [mode, setMode] = useState('visits')
  const [visits, setVisits] = useState([{ venueId: 'V002', date: '2026-11-01', start: '18:30', end: '19:15' }])
  const [personId, setPersonId] = useState('P031')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  if (restoring) return null
  if (!network) return <Navigate to="/load" replace />

  const update = (i, patch) => setVisits(visits.map((v, j) => (j === i ? { ...v, ...patch } : v)))

  const check = async (event) => {
    event.preventDefault()
    setLoading(true)
    setResult(null)
    try {
      const request =
        mode === 'person'
          ? { networkId: network.networkId, personId: personId.trim() }
          : {
              networkId: network.networkId,
              visits: visits.map((v) => ({ venueId: v.venueId.trim(), start: `${v.date}T${v.start}`, end: `${v.date}T${v.end}` })),
            }
      setResult(await checkExposure(request))
    } catch (e) {
      toast.error('Could not check these visits', { description: e.message })
    } finally {
      setLoading(false)
    }
  }

  const exposed = result?.result === 'EXPOSED'

  return (
    <Page title="Exposure self-check" subtitle="Were you at a place at the same time as a confirmed case?" icon={Sparkles} width="max-w-2xl">
      <GlassPanel>
        <Segmented
          size="sm"
          label="How to check"
          className="mb-4 w-full justify-between"
          value={mode}
          onChange={setMode}
          options={[
            { value: 'visits', label: 'Enter my visits' },
            { value: 'person', label: 'Use my recorded check-ins' },
          ]}
        />
        <form onSubmit={check} className="flex flex-col gap-3">
          {mode === 'person' ? (
            <Field label="Your person id" value={personId} onChange={(e) => setPersonId(e.target.value)} required hint="Shown on your check-in receipt, for example P031." />
          ) : (
            <>
              {visits.map((v, i) => (
                <div key={i} className="inset grid grid-cols-2 gap-2 rounded-2xl p-3 sm:grid-cols-[1fr_1.2fr_0.8fr_0.8fr_auto]">
                  <Field label="Venue" placeholder="V002" value={v.venueId} onChange={(e) => update(i, { venueId: e.target.value })} required />
                  <Field label="Date" type="date" value={v.date} onChange={(e) => update(i, { date: e.target.value })} required />
                  <Field label="From" type="time" value={v.start} onChange={(e) => update(i, { start: e.target.value })} required />
                  <Field label="To" type="time" value={v.end} onChange={(e) => update(i, { end: e.target.value })} required />
                  <div className="flex items-end justify-end">
                    <IconButton icon={Trash2} label="Remove this visit" disabled={visits.length === 1} onClick={() => setVisits(visits.filter((_, j) => j !== i))} />
                  </div>
                </div>
              ))}
              <Button size="sm" variant="ghost" icon={Plus} onClick={() => setVisits([...visits, blankVisit()])} disabled={visits.length >= 20}>
                Add another visit
              </Button>
            </>
          )}
          <Button type="submit" variant="primary" loading={loading}>
            Check my visits
          </Button>
        </form>
        <p className="mt-3 flex items-center gap-1.5 text-xs text-faint">
          <Lock className="size-3" /> Only venues and dates are shown. A confirmed case is never named.
        </p>
      </GlassPanel>

      <AnimatePresence mode="wait">
        {result && (
          <motion.div key={result.result + result.quickCheckHits} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="flex flex-col gap-4">
            <GlassPanel className={exposed ? 'border-case/50' : ''}>
              <div className="flex items-start gap-4">
                <span className={`grid size-14 shrink-0 place-items-center rounded-full ${exposed ? 'bg-case/15 text-case' : 'bg-vaccinated/15 text-vaccinated'}`}>
                  {exposed ? <ShieldAlert className="size-7" /> : <CircleCheck className="size-7" />}
                </span>
                <div>
                  <p className="text-xs font-semibold tracking-wide text-muted uppercase">Confirmed result</p>
                  <p className="text-2xl font-medium tracking-tight">{exposed ? 'Exposure confirmed' : 'Not exposed'}</p>
                  <p className="mt-1 text-sm text-muted">{result.message}</p>
                </div>
              </div>
              <div className="mt-4 grid grid-cols-2 gap-3 text-xs">
                <Step n="1" title="Quick check (Bloom filter)">
                  <Pill color={result.quickCheck === 'POSSIBLY_EXPOSED' ? 'var(--warning)' : 'var(--vaccinated)'}>
                    {result.quickCheck === 'POSSIBLY_EXPOSED' ? 'Possibly exposed' : 'Not exposed'}
                  </Pill>
                  <p className="mt-1 text-muted">Fast, never misses an exposure, but can raise a false alarm.</p>
                </Step>
                <Step n="2" title="Exact check (records)">
                  <Pill color={exposed ? 'var(--case)' : 'var(--vaccinated)'}>{exposed ? 'Overlap confirmed' : 'No overlap'}</Pill>
                  <p className="mt-1 text-muted">Only run when the quick check says “possibly”.</p>
                </Step>
              </div>
            </GlassPanel>

            <GlassPanel delay={0.05}>
              <PanelTitle icon={MapPin} title="Your visits" />
              <ul className="flex flex-col gap-2">
                {result.visits.map((v, i) => (
                  <li key={i} className="inset rounded-2xl p-3 text-sm">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-semibold">{v.venueId}</span>
                      <span className="text-xs text-muted">{dayFormat.format(new Date(v.start))}, {v.start.slice(11, 16)} to {v.end.slice(11, 16)}</span>
                      <span className="ml-auto flex gap-1.5">
                        <Pill color={v.quickCheck === 'POSSIBLY_EXPOSED' ? 'var(--warning)' : 'var(--vaccinated)'}>
                          {v.quickCheck === 'POSSIBLY_EXPOSED' ? `Possibly (${v.hoursFlagged}/${v.hoursChecked} h)` : 'Quick: no'}
                        </Pill>
                        {v.quickCheck === 'POSSIBLY_EXPOSED' && (
                          <Pill color={v.confirmed ? 'var(--case)' : 'var(--vaccinated)'}>{v.confirmed ? `${v.overlapMinutes} min overlap` : 'Cleared'}</Pill>
                        )}
                      </span>
                    </div>
                    <p className="mt-1.5 text-xs text-muted">{v.explanation}</p>
                  </li>
                ))}
              </ul>
              <p className="mt-3 flex items-start gap-1.5 text-xs text-faint">
                <CircleHelp className="mt-0.5 size-3 shrink-0" />
                The filter holds every hour a confirmed case spent at a venue ({formatNumber(result.filter.keys)} venue-hours in{' '}
                {formatNumber(result.filter.bits)} bits, k = {result.filter.hashes}). Expected false alarm rate{' '}
                {(result.filter.expectedFalsePositiveRate * 100).toFixed(2)}% per lookup.
              </p>
            </GlassPanel>
          </motion.div>
        )}
      </AnimatePresence>
    </Page>
  )
}

function Step({ n, title, children }) {
  return (
    <div className="inset rounded-2xl p-3">
      <p className="mb-1.5 flex items-center gap-2 font-semibold text-text">
        <span className="grid size-5 place-items-center rounded-full bg-accent text-xs text-white">{n}</span>
        {title}
      </p>
      {children}
    </div>
  )
}
