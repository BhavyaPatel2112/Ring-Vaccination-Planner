import { BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Tooltip } from 'chart.js'
import { Gauge, RefreshCw, Scale } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Bar } from 'react-chartjs-2'
import { Navigate } from 'react-router'
import { toast } from 'sonner'
import { compareAllLinks, compareMethods } from '../api/endpoints.js'
import { NeedCases, Page, Pill } from '../components/shared/Page.jsx'
import { Button } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Notice } from '../components/ui/Notice.jsx'
import { Segmented } from '../components/ui/Segmented.jsx'
import { StatTile } from '../components/ui/StatTile.jsx'
import { chartOptions, useChartColors } from '../lib/chartTheme.js'
import { METHOD_NAMES } from '../lib/labels.js'
import { useWorkspace } from '../state/context.js'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const COLORS = { APPROX: 'planned', GREEDY: 'accent', EXACT: 'vaccinated' }
const NOTES = {
  APPROX: 'Linear time. Never more than 2 times the minimum.',
  GREEDY: 'Most connected first, with our indexed heap. Often small, but no guarantee.',
  EXACT: 'Branch and bound. The true minimum, but only for small rings: the run time grows exponentially.',
}

/** The three vertex cover methods on the same rings: doses, run time and distance from the minimum. */
export function ComparePage() {
  const { network, cases, depth, contactSettings, restoring } = useWorkspace()
  const colors = useChartColors()
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [scope, setScope] = useState('rings')

  const request = useCallback(() => {
    if (!network) return Promise.resolve()
    if (scope === 'rings' && cases.length === 0) return Promise.resolve()
    const call = scope === 'rings' ? compareMethods(network.networkId, cases, depth, contactSettings) : compareAllLinks(network.networkId)
    return call.then(setResult).catch((e) => toast.error('Could not compare the methods', { description: e.message }))
  }, [network, cases, depth, contactSettings, scope])

  useEffect(() => {
    request()
  }, [request])

  const run = () => {
    setLoading(true)
    request().finally(() => setLoading(false))
  }

  if (restoring) return null
  if (!network) return <Navigate to="/load" replace />
  const scopeSwitch = (
    <Segmented
      size="sm"
      label="What to protect"
      value={scope}
      onChange={(value) => {
        setResult(null)
        setScope(value)
      }}
      options={[
        { value: 'rings', label: 'Around the cases' },
        { value: 'all', label: 'Every link' },
      ]}
    />
  )
  if (scope === 'rings' && cases.length === 0) {
    return (
      <Page title="Compare methods" icon={Scale} actions={scopeSwitch}>
        <NeedCases what="the comparison" />
      </Page>
    )
  }

  const methods = result?.methods ?? []
  const ok = methods.filter((m) => m.doses != null)
  const exact = methods.find((m) => m.method === 'EXACT')

  return (
    <Page
      title="Compare methods"
      subtitle={
        !result
          ? 'Running every method on the same input'
          : scope === 'rings'
            ? `${result.targetLinks} links to protect around ${cases.length} cases, same rings for every method`
            : `Every one of the ${result.targetLinks} links in the network, no rings: the plain vertex cover problem`
      }
      icon={Scale}
      actions={
        <div className="flex flex-wrap items-center gap-2">
          {scopeSwitch}
          <Button icon={RefreshCw} loading={loading} onClick={run}>
            Run again
          </Button>
        </div>
      }
    >
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {methods.map((m, i) => (
          <GlassPanel key={m.method} delay={i * 0.04}>
            <PanelTitle
              icon={Gauge}
              title={METHOD_NAMES[m.method] ?? m.method}
              action={<Pill color={`var(--${COLORS[m.method] ?? 'accent'})`}>{m.method}</Pill>}
            />
            {m.error ? (
              <Notice tone="warning" title="Refused for these rings">
                {m.error}
              </Notice>
            ) : (
              <div className="flex flex-col gap-3">
                <div className="grid grid-cols-2 gap-3">
                  <StatTile label="Doses" value={m.doses} size="lg" tone={`var(--${COLORS[m.method] ?? 'accent'})`} />
                  <div className="pt-1 text-right text-xs text-muted">
                    <p>{m.runtimeMs < 1 ? `${(m.runtimeMs * 1000).toFixed(0)} µs` : `${m.runtimeMs.toFixed(1)} ms`}</p>
                    <p className="mt-1">
                      {m.ratioToExact != null ? (
                        <span className="font-semibold text-text">{m.ratioToExact.toFixed(2)}× the minimum</span>
                      ) : (
                        'No exact minimum to compare with'
                      )}
                    </p>
                  </div>
                </div>
                <p className="text-xs leading-relaxed text-muted">{m.guarantee}</p>
              </div>
            )}
            <p className="mt-3 text-xs text-faint">{NOTES[m.method]}</p>
          </GlassPanel>
        ))}
      </div>

      {ok.length > 0 && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <GlassPanel delay={0.1}>
            <PanelTitle title="Doses per method" subtitle={scope === 'rings' ? 'Ring 1 doses are the same for every method' : 'People chosen to cover every link'} />
            <div className="h-64">
              <Bar
                data={{
                  labels: ok.map((m) => METHOD_NAMES[m.method] ?? m.method),
                  datasets: [{ label: 'Doses', data: ok.map((m) => m.doses), backgroundColor: ok.map((m) => colors[COLORS[m.method]]), borderRadius: 8 }],
                }}
                options={chartOptions(colors, { plugins: { legend: { display: false } }, y: { beginAtZero: true } })}
              />
            </div>
          </GlassPanel>
          <GlassPanel delay={0.14}>
            <PanelTitle title="Run time in Java" subtitle="Milliseconds, log scale" />
            <div className="h-64">
              <Bar
                data={{
                  labels: ok.map((m) => METHOD_NAMES[m.method] ?? m.method),
                  datasets: [{ label: 'ms', data: ok.map((m) => Math.max(m.runtimeMs, 0.001)), backgroundColor: ok.map((m) => colors[COLORS[m.method]]), borderRadius: 8 }],
                }}
                options={chartOptions(colors, { plugins: { legend: { display: false } }, y: { type: 'logarithmic' } })}
              />
            </div>
          </GlassPanel>
        </div>
      )}

      {exact?.error && (
        <Notice tone="info" title="Why the exact solver refuses">
          Minimum vertex cover is NP-hard: no fast method is known that always finds the minimum. The exact solver
          searches a tree that can double with every person left to decide, so it only runs on small rings. The
          2-approximation answers in linear time and is never more than 2 times the minimum.
        </Notice>
      )}
      <Notice tone="info" title="Try the greedy trap">
        Load the sample “Greedy trap (35 people)” and choose “Every link”: greedy picks 23 people where the exact solver
        needs 12. On “Greedy trap (58 people)” greedy picks 40 where 18 are enough (worse than 2 times), and the exact
        solver refuses because the network is too large to search.
      </Notice>
    </Page>
  )
}
