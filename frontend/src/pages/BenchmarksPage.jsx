import { BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, LineElement, LogarithmicScale, PointElement, Tooltip } from 'chart.js'
import { ChartColumn, Play } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Bar, Line } from 'react-chartjs-2'
import { toast } from 'sonner'
import { getBenchmarks, runBenchmark } from '../api/endpoints.js'
import { Page } from '../components/shared/Page.jsx'
import { Button } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Empty, Notice } from '../components/ui/Notice.jsx'
import { Segmented } from '../components/ui/Segmented.jsx'
import { Slider } from '../components/ui/Slider.jsx'
import { chartOptions, useChartColors } from '../lib/chartTheme.js'
import { formatNumber } from '../lib/format.js'

ChartJS.register(LineElement, PointElement, BarElement, CategoryScale, LinearScale, LogarithmicScale, Tooltip, Legend)

/** Turns a { header, rows } table into objects with numbers where possible. */
function records(table) {
  if (!table) return []
  return table.rows.map((row) =>
    Object.fromEntries(table.header.map((h, i) => [h, row[i] === '' || Number.isNaN(Number(row[i])) ? row[i] || null : Number(row[i])])),
  )
}

const line = (label, data, color, extra = {}) => ({ label, data, borderColor: color, backgroundColor: color, pointRadius: 3, tension: 0.25, spanGaps: true, ...extra })

/** Run time charts and quality results from every experiment, plus a quick live run in Java. */
export function BenchmarksPage() {
  const colors = useChartColors()
  const [tables, setTables] = useState(null)
  const [experiment, setExperiment] = useState('approx-vs-greedy')
  const [size, setSize] = useState(100000)
  const [live, setLive] = useState(null)
  const [running, setRunning] = useState(false)

  useEffect(() => {
    getBenchmarks()
      .then(setTables)
      .catch((e) => toast.error('Could not read the benchmark results', { description: e.message }))
  }, [])

  const t = useMemo(() => Object.fromEntries((tables ?? []).map((table) => [table.name, records(table)])), [tables])
  const logLog = (xTitle, yTitle) =>
    chartOptions(colors, {
      x: { type: 'logarithmic', title: { display: true, text: xTitle, color: colors['text-muted'] } },
      y: { type: 'logarithmic', title: { display: true, text: yTitle, color: colors['text-muted'] } },
    })
  const xy = (rows, x, y) => rows.filter((r) => r[y] != null).map((r) => ({ x: r[x], y: Math.max(r[y], 1e-4) }))

  const run = async () => {
    setRunning(true)
    try {
      setLive(await runBenchmark({ experiment, size }))
    } catch (e) {
      toast.error('Benchmark not run', { description: e.message })
    } finally {
      setRunning(false)
    }
  }

  if (tables && tables.length === 0) {
    return (
      <Page title="Benchmarks" icon={ChartColumn}>
        <Empty icon={ChartColumn} title="No saved results">
          Run the benchmark runner (see docs/benchmarks/README.md); the backend reads the CSV files from docs/benchmarks.
        </Empty>
      </Page>
    )
  }

  const greedy = t['approx-vs-greedy'] ?? []
  const quality = t['approx-vs-exact-summary'] ?? []
  const growth = t['exact-growth-summary'] ?? []
  const selection = (t.selection ?? []).filter((r) => r.input === 'random')
  const dsu = t['dsu-vs-bfs'] ?? []
  const bloom = t['bloom-vs-hashset'] ?? []
  const families = [...new Set(quality.map((r) => r.family))]

  return (
    <Page title="Benchmarks" subtitle="Measured in Java with warm-up runs; each point is the median of 5 to 7 runs. Inputs are seeded." icon={ChartColumn}>
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Chart title="Approximation against greedy: run time" subtitle="Linear time against O((V + E) log V), synthetic towns">
          <Line
            data={{ datasets: [line('2-approximation', xy(greedy, 'contacts', 'approx_ms'), colors.planned), line('Greedy (heap)', xy(greedy, 'contacts', 'greedy_ms'), colors.accent)] }}
            options={logLog('contacts', 'ms')}
          />
        </Chart>
        <Chart title="Approximation against the exact minimum" subtitle="Mean ratio over 30 random networks per size; the guarantee is 2">
          <Line
            data={{
              labels: [...new Set(quality.map((r) => r.people))],
              datasets: [
                ...families.map((f, i) => line(f, quality.filter((r) => r.family === f).map((r) => r.mean_ratio), i === 0 ? colors.planned : colors.accent)),
                line('Worst seen', [...new Set(quality.map((r) => r.people))].map((p) => Math.max(...quality.filter((r) => r.people === p).map((r) => r.max_ratio))), colors.case, { borderDash: [6, 4] }),
              ],
            }}
            options={chartOptions(colors, { y: { min: 1, max: 2.1, title: { display: true, text: 'doses ÷ minimum', color: colors['text-muted'] } }, x: { title: { display: true, text: 'people', color: colors['text-muted'] } } })}
          />
        </Chart>
        <Chart title="Exact solver: exponential growth" subtitle="Hard 4-regular networks; NP-hard in general">
          <Line
            data={{
              datasets: [
                line('Branch and bound', xy(growth, 'people', 'median_exact_ms'), colors.vaccinated),
                line('Brute force', xy(growth, 'people', 'median_brute_force_ms'), colors.case),
                line('2-approximation', xy(growth, 'people', 'median_approx_ms'), colors.planned),
              ],
            }}
            options={chartOptions(colors, {
              x: { type: 'linear', title: { display: true, text: 'people', color: colors['text-muted'] } },
              y: { type: 'logarithmic', title: { display: true, text: 'ms', color: colors['text-muted'] } },
            })}
          />
        </Chart>
        <Chart title="Selection: Quickselect against baselines" subtitle="Median of n random integers">
          <Line
            data={{
              datasets: [
                line('Randomized Quickselect', xy(selection.filter((r) => r.algorithm === 'randomized quickselect'), 'size', 'median_ms'), colors.planned),
                line('Median of medians', xy(selection.filter((r) => r.algorithm === 'median of medians'), 'size', 'median_ms'), colors.accent),
                line('Full sort', xy(selection.filter((r) => r.algorithm === 'full sort'), 'size', 'median_ms'), colors.case),
              ],
            }}
            options={logLog('values', 'ms')}
          />
        </Chart>
        <Chart title="Clusters: one update" subtitle="Union-find against recomputing with BFS after every new contact">
          <Line
            data={{
              datasets: [
                line('Union-find (per update)', dsu.map((r) => ({ x: r.insertions, y: r.dsu_per_update_us })), colors.vaccinated),
                line('BFS recompute (per update)', dsu.map((r) => ({ x: r.insertions, y: r.bfs_per_update_ms * 1000 })), colors.case),
              ],
            }}
            options={logLog('contacts', 'microseconds per update')}
          />
        </Chart>
        <Chart title="Bloom filter against a HashSet: memory" subtitle="Filters sized for 1% false positives">
          <Bar
            data={{
              labels: bloom.map((r) => `${formatNumber(r.keys)} keys`),
              datasets: [
                { label: 'Bloom filter', data: bloom.map((r) => r.bloom_bytes / 1024), backgroundColor: colors.accent, borderRadius: 6 },
                { label: 'HashSet (estimate)', data: bloom.map((r) => r.hashset_bytes_estimate / 1024), backgroundColor: colors.case, borderRadius: 6 },
              ],
            }}
            options={chartOptions(colors, { y: { type: 'logarithmic', title: { display: true, text: 'KB', color: colors['text-muted'] } } })}
          />
        </Chart>
      </div>

      {bloom.length > 0 && (
        <GlassPanel>
          <PanelTitle title="Bloom filter accuracy" subtitle="Measured false positive rate against (1 − e^(−kn/m))^k" />
          <div className="scroll-thin overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs text-muted">
                <tr>
                  {['Keys', 'Bits', 'k', 'Theory', 'Measured', 'Difference', 'Memory saved', 'Lookup (filter)', 'Lookup (HashSet)'].map((h) => (
                    <th key={h} className="py-2 pr-4 font-semibold">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {bloom.map((r) => (
                  <tr key={r.keys} className="border-t border-[var(--glass-border)]">
                    <td className="py-2 pr-4">{formatNumber(r.keys)}</td>
                    <td className="py-2 pr-4">{formatNumber(r.bits)}</td>
                    <td className="py-2 pr-4">{r.k}</td>
                    <td className="py-2 pr-4">{(r.theoretical_rate * 100).toFixed(3)}%</td>
                    <td className="py-2 pr-4">{(r.measured_rate * 100).toFixed(3)}%</td>
                    <td className="py-2 pr-4">{(r.relative_error * 100).toFixed(1)}%</td>
                    <td className="py-2 pr-4">{r.memory_ratio.toFixed(0)}×</td>
                    <td className="py-2 pr-4">{r.bloom_query_ns.toFixed(0)} ns</td>
                    <td className="py-2 pr-4">{r.hashset_query_ns.toFixed(0)} ns</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </GlassPanel>
      )}

      <GlassPanel>
        <PanelTitle icon={Play} title="Run one now" subtitle="Three sizes up to the one you pick, measured live in Java (smaller than the saved runs)" />
        <div className="grid grid-cols-1 items-end gap-4 md:grid-cols-[auto_minmax(0,1fr)_auto]">
          <Segmented
            size="sm"
            label="Experiment"
            value={experiment}
            onChange={setExperiment}
            options={[
              { value: 'approx-vs-greedy', label: 'Approx. vs greedy' },
              { value: 'dsu', label: 'Union-find vs BFS' },
              { value: 'bloom', label: 'Bloom vs HashSet' },
            ]}
          />
          <Slider label="Largest input" value={size} onChange={setSize} min={1000} max={200000} step={1000} format={formatNumber} />
          <Button variant="primary" icon={Play} loading={running} onClick={run}>
            Run
          </Button>
        </div>
        {live && (
          <div className="scroll-thin mt-4 overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-muted">
                <tr>{live.header.map((h) => <th key={h} className="py-2 pr-4 font-semibold">{h.replaceAll('_', ' ')}</th>)}</tr>
              </thead>
              <tbody>
                {live.rows.map((row, i) => (
                  <tr key={i} className="border-t border-[var(--glass-border)]">
                    {row.map((cell, j) => <td key={j} className="py-2 pr-4">{cell}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </GlassPanel>
      <Notice tone="info" title="Reading the results">
        Greedy usually needs fewer doses than the 2-approximation on realistic networks, but it has no guarantee: on the
        greedy trap samples it needs up to 2.2 times the minimum. The approximation is never above 2 times the minimum and
        runs several times faster. Full tables and the machine used are in docs/benchmarks.
      </Notice>
    </Page>
  )
}

function Chart({ title, subtitle, children }) {
  return (
    <GlassPanel>
      <PanelTitle title={title} subtitle={subtitle} />
      <div className="h-64">{children}</div>
    </GlassPanel>
  )
}
