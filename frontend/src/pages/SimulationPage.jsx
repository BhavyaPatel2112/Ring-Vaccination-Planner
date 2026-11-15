import { BarElement, CategoryScale, Chart as ChartJS, Filler, Legend, LinearScale, LineElement, PointElement, Tooltip } from 'chart.js'
import { Play, Settings2, Unlink, Waves } from 'lucide-react'
import { useState } from 'react'
import { Bar, Line } from 'react-chartjs-2'
import { Navigate } from 'react-router'
import { toast } from 'sonner'
import { simulate, simulateRobustness } from '../api/endpoints.js'
import { Field, NeedCases, Page, Pill, TextStat } from '../components/shared/Page.jsx'
import { Button } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Notice } from '../components/ui/Notice.jsx'
import { Slider } from '../components/ui/Slider.jsx'
import { chartOptions, useChartColors } from '../lib/chartTheme.js'
import { useWorkspace } from '../state/context.js'

ChartJS.register(BarElement, LineElement, PointElement, CategoryScale, LinearScale, Filler, Tooltip, Legend)

const TONES = { NONE: 'case', RANDOM: 'warning', APPROX: 'planned', GREEDY: 'accent' }

/** Seeded outbreak simulation: infections with no vaccination, random doses and each ring plan. */
export function SimulationPage() {
  const { network, cases, depth, restoring } = useWorkspace()
  const colors = useChartColors()
  const [transmission, setTransmission] = useState(0.08)
  const [recovery, setRecovery] = useState(4)
  const [trials, setTrials] = useState(200)
  const [efficacy, setEfficacy] = useState(1)
  const [seed, setSeed] = useState('2026')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [robust, setRobust] = useState(null)
  const [robustLoading, setRobustLoading] = useState(false)

  if (restoring) return null
  if (!network) return <Navigate to="/load" replace />
  if (cases.length === 0) {
    return (
      <Page title="Outbreak simulation" icon={Waves}>
        <NeedCases what="the simulation" />
      </Page>
    )
  }

  const run = async () => {
    setLoading(true)
    try {
      setResult(
        await simulate(network.networkId, {
          caseIds: cases,
          depth,
          transmission,
          recoverySteps: recovery,
          trials,
          efficacy,
          seed: seed === '' ? undefined : Number(seed),
        }),
      )
    } catch (e) {
      toast.error('Simulation not run', { description: e.message })
    } finally {
      setLoading(false)
    }
  }

  const settings = {
    caseIds: cases,
    depth,
    transmission,
    recoverySteps: recovery,
    efficacy,
    seed: seed === '' ? undefined : Number(seed),
  }

  const runRobustness = async () => {
    setRobustLoading(true)
    try {
      setRobust(await simulateRobustness(network.networkId, { ...settings, trials: Math.min(trials, 200) }))
    } catch (e) {
      toast.error('Test not run', { description: e.message })
    } finally {
      setRobustLoading(false)
    }
  }

  const scenarios = result?.scenarios ?? []
  const longest = Math.max(0, ...scenarios.map((s) => s.curve.length))

  return (
    <Page title="Outbreak simulation" subtitle="Does the plan actually prevent infections? Same seeded outbreaks for every strategy." icon={Waves}>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[320px_minmax(0,1fr)]">
        <GlassPanel className="h-fit">
          <PanelTitle icon={Settings2} title="Outbreak settings" subtitle="A simple SIR model on the contact network" />
          <div className="flex flex-col gap-4">
            <Slider label="Chance to infect a contact per day" value={transmission} onChange={setTransmission} min={0.01} max={0.5} step={0.01} format={(v) => `${Math.round(v * 100)}%`} />
            <Slider label="Days infectious" value={recovery} onChange={setRecovery} min={1} max={14} />
            <Slider label="Simulated outbreaks" value={trials} onChange={setTrials} min={20} max={1000} step={20} />
            <Slider label="Vaccine protection" value={efficacy} onChange={setEfficacy} min={0.5} max={1} step={0.05} format={(v) => `${Math.round(v * 100)}%`} />
            <Field label="Seed" type="number" value={seed} onChange={(e) => setSeed(e.target.value)} hint="The same seed always gives the same numbers." />
            <Button variant="primary" icon={Play} loading={loading} onClick={run}>
              Run simulation
            </Button>
          </div>
        </GlassPanel>

        <div className="flex min-w-0 flex-col gap-4">
          {!result && (
            <Notice tone="info" title="What this shows">
              The cases start infected and spread along contacts. Each strategy gets the same outbreaks: no vaccination,
              random people with as many doses as the plan, and the two ring plans. Median and 90th percentile are found
              with randomized Quickselect.
            </Notice>
          )}
          {result && (
            <>
              <Notice tone="info" title="Result">
                {result.headline}
              </Notice>
              <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
                {scenarios.map((s, i) => (
                  <GlassPanel key={s.name} delay={i * 0.04}>
                    <p className="text-sm font-semibold">{s.label}</p>
                    <p className="mb-3 text-xs text-muted">{s.doses} doses</p>
                    <TextStat label="Average new infections" value={s.mean.toFixed(1)} tone={colors[TONES[s.name]]} />
                    <p className="mt-2 text-xs text-muted">
                      Median {s.median} · 90th percentile {s.p90} · worst {s.max}
                    </p>
                    {s.reductionVsNone != null && s.name !== 'NONE' && (
                      <Pill className="mt-2" color={s.reductionVsNone > 0 ? 'var(--vaccinated)' : 'var(--case)'}>
                        {s.reductionVsNone >= 0 ? '−' : '+'}
                        {Math.abs(s.reductionVsNone).toFixed(0)}% vs none
                      </Pill>
                    )}
                  </GlassPanel>
                ))}
              </div>
              <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                <GlassPanel>
                  <PanelTitle title="New infections" subtitle="Average and 90th percentile over all outbreaks" />
                  <div className="h-64">
                    <Bar
                      data={{
                        labels: scenarios.map((s) => s.label.replace(' (2-approximation)', '').replace(', same doses as the plan', '')),
                        datasets: [
                          { label: 'Average', data: scenarios.map((s) => s.mean), backgroundColor: scenarios.map((s) => colors[TONES[s.name]]), borderRadius: 6 },
                          { label: '90th percentile', data: scenarios.map((s) => s.p90), backgroundColor: colors['glass-border'], borderRadius: 6 },
                        ],
                      }}
                      options={chartOptions(colors, { y: { beginAtZero: true } })}
                    />
                  </div>
                </GlassPanel>
                <GlassPanel>
                  <PanelTitle title="Epidemic curve" subtitle="Average number of infectious people each day" />
                  <div className="h-64">
                    <Line
                      data={{
                        labels: Array.from({ length: longest }, (_, i) => i),
                        datasets: scenarios.map((s) => ({
                          label: s.name === 'NONE' ? 'None' : s.name === 'RANDOM' ? 'Random' : s.name === 'APPROX' ? 'Approx. plan' : 'Greedy plan',
                          data: s.curve,
                          borderColor: colors[TONES[s.name]],
                          backgroundColor: colors[TONES[s.name]],
                          pointRadius: 0,
                          tension: 0.3,
                        })),
                      }}
                      options={chartOptions(colors, { x: { title: { display: true, text: 'day', color: colors['text-muted'] } }, y: { beginAtZero: true } })}
                    />
                  </div>
                </GlassPanel>
              </div>
              <p className="text-xs text-faint">
                {result.trials} outbreaks per strategy, seed {result.seed}, {result.runtimeMs.toFixed(0)} ms in Java. A simple model: it
                compares strategies, it does not forecast a real outbreak.
              </p>
            </>
          )}
          <GlassPanel>
            <PanelTitle
              icon={Unlink}
              title="What if contact tracing missed people?"
              subtitle="The plan is made from part of the contacts; the outbreak spreads along all of them"
              action={
                <Button size="sm" variant="primary" loading={robustLoading} onClick={runRobustness}>
                  {robust ? 'Run again' : 'Run the test'}
                </Button>
              }
            />
            {!robust && (
              <p className="text-sm text-muted">
                Hides 0% to 50% of the contacts from the planner (the same seeded contacts each time, more at each step), then runs
                the same outbreaks with each plan. It shows how much of the benefit survives when tracing is incomplete.
              </p>
            )}
            {robust && (
              <>
                <p className="mb-3 text-sm">{robust.headline}</p>
                <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_280px]">
                  <div className="h-64">
                    <Line
                      data={{
                        labels: robust.rows.map((r) => `${Math.round(r.missingShare * 100)}%`),
                        datasets: [
                          {
                            label: 'With the plan',
                            data: robust.rows.map((r) => r.mean),
                            borderColor: colors.planned,
                            backgroundColor: colors.planned,
                            pointRadius: 4,
                            tension: 0.25,
                          },
                          {
                            label: 'No vaccination',
                            data: robust.rows.map(() => robust.noVaccinationMean),
                            borderColor: colors.case,
                            backgroundColor: colors.case,
                            borderDash: [6, 4],
                            pointRadius: 0,
                          },
                        ],
                      }}
                      options={chartOptions(colors, {
                        x: { title: { display: true, text: 'contacts missed by tracing', color: colors['text-muted'] } },
                        y: { beginAtZero: true, title: { display: true, text: 'average new infections', color: colors['text-muted'] } },
                      })}
                    />
                  </div>
                  <table className="w-full self-start text-left text-xs">
                    <thead className="text-muted">
                      <tr>
                        <th className="py-1.5 pr-2 font-semibold">Missed</th>
                        <th className="py-1.5 pr-2 font-semibold">Doses</th>
                        <th className="py-1.5 pr-2 font-semibold">Infections</th>
                        <th className="py-1.5 font-semibold">Fewer</th>
                      </tr>
                    </thead>
                    <tbody>
                      {robust.rows.map((r) => (
                        <tr key={r.missingShare} className="border-t border-[var(--glass-border)]">
                          <td className="py-1.5 pr-2">{Math.round(r.missingShare * 100)}%</td>
                          <td className="py-1.5 pr-2">{r.doses}</td>
                          <td className="py-1.5 pr-2">{r.mean.toFixed(1)}</td>
                          <td className="py-1.5">{r.reductionVsNone == null ? '–' : `${r.reductionVsNone.toFixed(0)}%`}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </GlassPanel>
        </div>
      </div>
    </Page>
  )
}
