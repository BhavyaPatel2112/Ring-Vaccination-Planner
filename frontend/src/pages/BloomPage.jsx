import { CategoryScale, Chart as ChartJS, Legend, LinearScale, LineElement, LogarithmicScale, PointElement, Tooltip } from 'chart.js'
import { Database, Filter, HardDrive } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Line } from 'react-chartjs-2'
import { toast } from 'sonner'
import { bloomStats, tuneBloom } from '../api/endpoints.js'
import { Page, TextStat } from '../components/shared/Page.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Notice } from '../components/ui/Notice.jsx'
import { Segmented } from '../components/ui/Segmented.jsx'
import { Slider } from '../components/ui/Slider.jsx'
import { StatTile } from '../components/ui/StatTile.jsx'
import { chartOptions, useChartColors } from '../lib/chartTheme.js'
import { formatNumber } from '../lib/format.js'
import { useWorkspace } from '../state/context.js'

ChartJS.register(LineElement, PointElement, CategoryScale, LinearScale, LogarithmicScale, Tooltip, Legend)

// The m slider is logarithmic: position 0 to 100 covers 1,000 to 10,000,000 bits.
const mFromPosition = (p) => Math.round(1000 * 10 ** (p / 25))
const positionFromM = (m) => Math.round(25 * Math.log10(m / 1000))

function bytes(n) {
  if (n >= 1024 * 1024) return `${(n / 1024 / 1024).toFixed(1)} MB`
  if (n >= 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${n} B`
}

const percent = (v) => (v >= 0.0001 ? `${(v * 100).toFixed(3)}%` : `${(v * 100).toExponential(1)}%`)

/** Bloom filter tuning: m and k against the measured and theoretical false positive rate and the memory. */
export function BloomPage() {
  const { network } = useWorkspace()
  const colors = useChartColors()
  const [n, setN] = useState(10000)
  const [position, setPosition] = useState(positionFromM(100000))
  const [k, setK] = useState(7)
  const [result, setResult] = useState(null)
  const [live, setLive] = useState(null)
  const m = mFromPosition(position)

  // Re-measure shortly after the sliders stop moving.
  useEffect(() => {
    const timer = setTimeout(() => {
      tuneBloom({ m, k, n })
        .then(setResult)
        .catch((e) => toast.error('Could not measure the filter', { description: e.message }))
    }, 300)
    return () => clearTimeout(timer)
  }, [m, k, n])

  useEffect(() => {
    if (!network) return
    bloomStats(network.networkId).then(setLive).catch(() => setLive(null))
  }, [network])

  const r = result?.result
  const chart = useMemo(() => {
    if (!result) return null
    const theory = result.theoryByK
    return {
      labels: theory.map((t) => t.k),
      datasets: [
        {
          label: 'Theory: (1 − e^(−kn/m))^k',
          data: theory.map((t) => Math.max(t.rate, 1e-9)),
          borderColor: colors.accent,
          backgroundColor: colors.accent,
          pointRadius: 2,
          tension: 0.3,
        },
        {
          label: 'Measured at your k',
          data: theory.map((t) => (t.k === r.k ? Math.max(r.measuredRate, 1e-9) : null)),
          borderColor: colors.case,
          backgroundColor: colors.case,
          pointRadius: 7,
          pointHoverRadius: 8,
          showLine: false,
        },
      ],
    }
  }, [result, r, colors])

  return (
    <Page title="Bloom filter tuning" subtitle="Bits (m) and hash functions (k) against accuracy and memory, measured in Java" icon={Filter}>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[340px_minmax(0,1fr)]">
        <div className="flex flex-col gap-4">
          <GlassPanel>
            <PanelTitle icon={Filter} title="Settings" subtitle="Each change builds a new filter and runs 100,000 queries" />
            <div className="flex flex-col gap-5">
              <div>
                <p className="mb-2 text-xs font-medium text-muted">Keys stored (n)</p>
                <Segmented
                  size="sm"
                  label="Keys"
                  value={n}
                  onChange={setN}
                  options={[
                    { value: 1000, label: '1K' },
                    { value: 10000, label: '10K' },
                    { value: 100000, label: '100K' },
                  ]}
                />
              </div>
              <Slider label="Bits (m)" value={position} onChange={setPosition} min={0} max={100} format={() => `${formatNumber(m)} (${(m / n).toFixed(1)} per key)`} />
              <Slider label="Hash functions (k)" value={k} onChange={setK} min={1} max={20} />
            </div>
          </GlassPanel>
          {r && (
            <GlassPanel delay={0.04}>
              <PanelTitle icon={HardDrive} title="Memory" subtitle="Same keys, exact HashSet as the baseline" />
              <div className="grid grid-cols-2 gap-3">
                <TextStat label="Bloom filter" value={bytes(r.bloomBytes)} tone="var(--accent)" />
                <TextStat label="HashSet (estimate)" value={bytes(r.hashSetBytes)} />
              </div>
              <div className="mt-3 h-2 overflow-hidden rounded-full bg-inset">
                <div className="h-full rounded-full bg-accent" style={{ width: `${Math.max((100 * r.bloomBytes) / r.hashSetBytes, 1)}%` }} />
              </div>
              <p className="mt-2 text-xs text-muted">The filter needs {r.memoryRatio.toFixed(1)} times less memory than the hash set.</p>
            </GlassPanel>
          )}
        </div>

        <div className="flex min-w-0 flex-col gap-4">
          {r && (
            <GlassPanel>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                <TextStat label="Theory" value={percent(r.theoreticalRate)} tone="var(--accent)" />
                <TextStat label="Measured" value={percent(r.measuredRate)} tone="var(--case)" />
                <StatTile label="Best k here" value={r.optimalK} tone="var(--vaccinated)" />
                <StatTile label="False negatives" value={r.falseNegatives} />
              </div>
              <p className="mt-3 text-xs leading-relaxed text-muted">{result.summary}</p>
            </GlassPanel>
          )}
          {chart && (
            <GlassPanel delay={0.04}>
              <PanelTitle title="False positive rate by k" subtitle="Log scale. Too few hashes and too many both hurt; the best is k = (m/n) ln 2." />
              <div className="h-72">
                <Line
                  data={chart}
                  options={chartOptions(colors, {
                    x: { title: { display: true, text: 'k (hash functions)', color: colors['text-muted'] } },
                    y: { type: 'logarithmic', title: { display: true, text: 'false positive rate', color: colors['text-muted'] } },
                  })}
                />
              </div>
            </GlassPanel>
          )}
          {live && (
            <GlassPanel delay={0.08}>
              <PanelTitle icon={Database} title="Filters in use for this network" subtitle={network?.name} />
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {live.map((f) => (
                  <div key={f.name} className="inset rounded-2xl p-3 text-xs">
                    <p className="font-semibold text-text">{f.name}</p>
                    <p className="mt-1 text-muted">
                      {formatNumber(f.keys)} keys · {formatNumber(f.bits)} bits · k = {f.hashes} · {bytes(f.memoryBytes)} against about{' '}
                      {bytes(f.hashSetBytes)} for a hash set
                    </p>
                    <p className="mt-1 text-muted">
                      {(f.fillRatio * 100).toFixed(0)}% of bits set · expected false positive rate {percent(f.expectedFalsePositiveRate)}
                    </p>
                  </div>
                ))}
              </div>
            </GlassPanel>
          )}
          <Notice tone="info" title="Why a hit is only “possibly”">
            Adding a key sets k bits, and bits are never cleared, so a stored key is always found: no false negatives. A key
            that was never added can still find all k of its bits set by other keys, which is a false positive. That is why
            the exposure check and the registry confirm every hit against the exact records.
          </Notice>
        </div>
      </div>
    </Page>
  )
}
