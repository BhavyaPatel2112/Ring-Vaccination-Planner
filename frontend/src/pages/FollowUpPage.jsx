import { CalendarClock, Eye, Filter, RefreshCw, Syringe } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router'
import { toast } from 'sonner'
import { followUp } from '../api/endpoints.js'
import { Field, NeedCases, Page, Pill } from '../components/shared/Page.jsx'
import { Button } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Notice } from '../components/ui/Notice.jsx'
import { Slider } from '../components/ui/Slider.jsx'
import { StatTile } from '../components/ui/StatTile.jsx'
import { formatNumber } from '../lib/format.js'
import { useWorkspace } from '../state/context.js'

const STATUS = {
  DATE_UNKNOWN: { label: 'Date unknown', color: 'var(--warning)' },
  URGENT: { label: 'Today', color: 'var(--case)' },
  ON_TIME: { label: 'On time', color: 'var(--vaccinated)' },
  PAST_WINDOW: { label: 'Monitor instead', color: 'var(--excluded)' },
}

const MONITOR = {
  PAST_WINDOW: { label: 'Past the window', color: 'var(--excluded)' },
  CANNOT_BE_VACCINATED: { label: 'Cannot be vaccinated', color: 'var(--excluded)' },
  NOT_IN_PLAN: { label: 'Not in the plan', color: 'var(--ring2)' },
  LEFT_OUT_BY_BUDGET: { label: 'Left out by budget', color: 'var(--warning)' },
}

const dateTime = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
const dateOnly = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
const showDateTime = (v) => (v ? dateTime.format(new Date(v)) : '–')
const showDate = (v) => (v ? dateOnly.format(new Date(`${v}T00:00`)) : 'unknown')

/** Vaccinate-by deadlines (B), the symptom monitoring list (C) and the contact settings (E). */
export function FollowUpPage() {
  const { network, cases, depth, method, contactSettings, setContactSettings, restoring } = useWorkspace()
  const [useMin, setUseMin] = useState(contactSettings.minDurationMinutes != null)
  const [minDuration, setMinDuration] = useState(contactSettings.minDurationMinutes ?? 15)
  const [useWindow, setUseWindow] = useState(contactSettings.windowDays != null)
  const [windowDays, setWindowDays] = useState(contactSettings.windowDays ?? 14)
  const [windowHours, setWindowHours] = useState(72)
  const [incubation, setIncubation] = useState(21)
  const [asOf, setAsOf] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const settings = {
    ...(useMin && { minDurationMinutes: minDuration }),
    ...(useWindow && { windowDays }),
  }
  const settingsKey = JSON.stringify(settings)

  const request = useCallback(() => {
    if (!network || cases.length === 0) return Promise.resolve()
    return followUp(network.networkId, {
      caseIds: cases,
      depth,
      method,
      windowHours,
      incubationDays: incubation,
      ...(asOf && { asOf }),
      ...JSON.parse(settingsKey),
    })
      .then(setResult)
      .catch((e) => toast.error('Could not build the follow-up lists', { description: e.message }))
  }, [network, cases, depth, method, windowHours, incubation, asOf, settingsKey])

  useEffect(() => {
    request()
  }, [request])

  const run = () => {
    setLoading(true)
    request().finally(() => setLoading(false))
  }

  if (restoring) return null
  if (!network) return <Navigate to="/load" replace />
  if (cases.length === 0) {
    return (
      <Page title="Deadlines and monitoring" icon={CalendarClock}>
        <NeedCases what="the deadlines" />
      </Page>
    )
  }

  const applied = JSON.stringify(contactSettings) === settingsKey
  const filter = result?.contactFilter

  return (
    <Page
      title="Deadlines and monitoring"
      subtitle={result ? `As of ${showDateTime(result.asOf)} · ${result.method} plan` : 'The plan as field work'}
      icon={CalendarClock}
      actions={
        <Button icon={RefreshCw} loading={loading} onClick={run}>
          Refresh
        </Button>
      }
    >
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[340px_minmax(0,1fr)]">
        <div className="flex flex-col gap-4">
          <GlassPanel>
            <PanelTitle icon={Filter} title="Which contacts count" subtitle="Contact definition, applied before the rings" />
            <div className="flex flex-col gap-4">
              <div>
                <Toggle checked={useMin} onChange={setUseMin}>Minimum contact time</Toggle>
                {useMin && <Slider label="At least" value={minDuration} onChange={setMinDuration} min={0} max={120} step={5} format={(v) => `${v} min`} />}
              </div>
              <div>
                <Toggle checked={useWindow} onChange={setUseWindow}>Only recent contacts</Toggle>
                {useWindow && <Slider label="Within" value={windowDays} onChange={setWindowDays} min={1} max={60} format={(v) => `${v} days`} />}
              </div>
              {filter && (
                <p className="text-xs text-muted">
                  {formatNumber(filter.total - filter.kept)} of {formatNumber(filter.total)} contacts left out: {filter.tooShort} too short,{' '}
                  {filter.beforeWindow} too old, {filter.afterReference} after {showDate(filter.referenceDate)}. Contacts with no recorded
                  time or date are kept to be safe ({filter.keptUnknownDuration + filter.keptUnknownDate}).
                </p>
              )}
              <Button
                size="sm"
                variant={applied ? 'ghost' : 'primary'}
                disabled={applied}
                onClick={() => {
                  setContactSettings(JSON.parse(settingsKey))
                  toast.success('Contact settings applied', { description: 'The rings, plans and comparisons now use them.' })
                }}
              >
                {applied ? 'Used on every screen' : 'Use these settings on every screen'}
              </Button>
            </div>
          </GlassPanel>

          <GlassPanel delay={0.04}>
            <PanelTitle icon={CalendarClock} title="Time windows" />
            <div className="flex flex-col gap-4">
              <Slider label="Post-exposure vaccination window" value={windowHours} onChange={setWindowHours} min={24} max={168} step={12} format={(v) => `${v} hours`} />
              <Slider label="Incubation period (monitoring)" value={incubation} onChange={setIncubation} min={7} max={42} format={(v) => `${v} days`} />
              <Field
                label="As of"
                type="datetime-local"
                value={asOf}
                onChange={(e) => setAsOf(e.target.value)}
                hint="Empty: the start of the latest confirmation day."
              />
            </div>
          </GlassPanel>
        </div>

        <div className="flex min-w-0 flex-col gap-4">
          <GlassPanel>
            <PanelTitle icon={Syringe} title="Vaccinate by" subtitle="Most urgent first, ordered with our indexed heap" />
            {result && (
              <>
                <div className="mb-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <StatTile label="Today" value={result.urgent} tone="var(--case)" />
                  <StatTile label="On time" value={result.onTime} tone="var(--vaccinated)" />
                  <StatTile label="Monitor instead" value={result.pastWindow} tone="var(--excluded)" />
                  <StatTile label="Date unknown" value={result.dateUnknown} tone="var(--warning)" />
                </div>
                <div className="scroll-thin max-h-[44vh] overflow-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="sticky top-0 bg-[var(--glass-strong)] text-xs text-muted backdrop-blur">
                      <tr>
                        <th className="py-2 pr-3 font-semibold">#</th>
                        <th className="py-2 pr-3 font-semibold">Person</th>
                        <th className="py-2 pr-3 font-semibold">Ring</th>
                        <th className="py-2 pr-3 font-semibold">Last exposure</th>
                        <th className="py-2 pr-3 font-semibold">Vaccinate by</th>
                        <th className="py-2 font-semibold">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {result.doses.map((d) => (
                        <tr key={d.personId} className="border-t border-[var(--glass-border)]">
                          <td className="py-2 pr-3 text-faint">{d.order}</td>
                          <td className="py-2 pr-3">
                            <p className="font-semibold">{d.name || d.personId}</p>
                            <p className="text-xs text-muted">{d.personId}</p>
                          </td>
                          <td className="py-2 pr-3 text-muted">{d.ring}</td>
                          <td className="py-2 pr-3 text-muted">{showDate(d.lastExposure)}</td>
                          <td className="py-2 pr-3">{showDateTime(d.deadline)}</td>
                          <td className="py-2" title={d.advice}>
                            <Pill color={STATUS[d.status].color}>{STATUS[d.status].label}</Pill>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {result.doses.length === 0 && <p className="py-6 text-center text-sm text-muted">The plan has no doses.</p>}
                </div>
              </>
            )}
          </GlassPanel>

          <GlassPanel delay={0.05}>
            <PanelTitle icon={Eye} title="Watch for symptoms" subtitle="Ring members without a dose today, until last exposure + incubation" />
            {result && result.monitoring.length === 0 && <p className="text-sm text-muted">Everyone in the rings is vaccinated or has a dose planned on time.</p>}
            {result && result.monitoring.length > 0 && (
              <div className="scroll-thin max-h-[40vh] overflow-auto">
                <table className="w-full text-left text-sm">
                  <thead className="sticky top-0 bg-[var(--glass-strong)] text-xs text-muted backdrop-blur">
                    <tr>
                      <th className="py-2 pr-3 font-semibold">Person</th>
                      <th className="py-2 pr-3 font-semibold">Ring</th>
                      <th className="py-2 pr-3 font-semibold">Why</th>
                      <th className="py-2 font-semibold">Monitor until</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.monitoring.map((m) => (
                      <tr key={m.personId} className="border-t border-[var(--glass-border)]">
                        <td className="py-2 pr-3">
                          <p className="font-semibold">{m.name || m.personId}</p>
                          <p className="text-xs text-muted">{m.personId}</p>
                        </td>
                        <td className="py-2 pr-3 text-muted">{m.ring}</td>
                        <td className="py-2 pr-3" title={m.explanation}>
                          <Pill color={MONITOR[m.reason].color}>{MONITOR[m.reason].label}</Pill>
                        </td>
                        <td className="py-2">{showDate(m.monitorUntil)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </GlassPanel>
          <Notice tone="info" title="How the dates are worked out">
            Last exposure is the latest contact with someone one ring closer to a case. Dates are days, so the exposure is
            taken as the start of that day, which gives the earliest (safest) deadline.
          </Notice>
        </div>
      </div>
    </Page>
  )
}

function Toggle({ checked, onChange, children }) {
  return (
    <label className="mb-2 flex items-center gap-2 text-sm font-semibold">
      <input type="checkbox" className="size-4 accent-[var(--accent)]" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {children}
    </label>
  )
}
