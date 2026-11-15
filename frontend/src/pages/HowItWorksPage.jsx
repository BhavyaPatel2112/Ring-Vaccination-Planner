import { useAutoAnimate } from '@formkit/auto-animate/react'
import { ChevronLeft, ChevronRight, Filter, GraduationCap, Pause, Play, RotateCcw, Search, ShieldCheck } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Navigate } from 'react-router'
import { toast } from 'sonner'
import { approxWalkthrough, bloomDemo } from '../api/endpoints.js'
import { CurvedGrid } from '../components/network/CurvedGrid.jsx'
import { NetworkCanvas } from '../components/network/NetworkCanvas.jsx'
import { Field, NeedCases, Page, Pill, TextStat } from '../components/shared/Page.jsx'
import { Button, IconButton } from '../components/ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../components/ui/GlassPanel.jsx'
import { Notice } from '../components/ui/Notice.jsx'
import { Segmented } from '../components/ui/Segmented.jsx'
import { Slider } from '../components/ui/Slider.jsx'
import { cn } from '../lib/cn.js'
import { linkKey } from '../lib/linkKey.js'
import { useWorkspace } from '../state/context.js'

/**
 * Step-by-step views of two algorithms. Every step comes from the Java code (the
 * real 2-approximation reports what it does, and the real Bloom filter reports
 * which bits a key uses); this screen only replays them.
 */
export function HowItWorksPage() {
  const [tab, setTab] = useState('approx')
  return (
    <Page
      title="How it works"
      subtitle="Watch the algorithms make their decisions, one step at a time"
      icon={GraduationCap}
      width="max-w-[1600px]"
      actions={
        <Segmented
          size="sm"
          label="Algorithm"
          value={tab}
          onChange={setTab}
          options={[
            { value: 'approx', label: '2-approximation' },
            { value: 'bloom', label: 'Bloom filter' },
          ]}
        />
      }
    >
      {tab === 'approx' ? <ApproxWalkthrough /> : <BloomWalkthrough />}
    </Page>
  )
}

// ------------------------------------------------------------------ 2-approximation

const SPEEDS = { slow: 1400, normal: 700, fast: 250 }

function ApproxWalkthrough() {
  const { network, view, cases, depth, ring, contactSettings, restoring } = useWorkspace()
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [step, setStep] = useState(0)
  const [playing, setPlaying] = useState(false)
  const [speed, setSpeed] = useState('normal')
  const listRef = useRef(null)

  useEffect(() => {
    if (!network || cases.length === 0) return
    let live = true
    approxWalkthrough(network.networkId, cases, depth, contactSettings)
      .then((result) => {
        if (!live) return
        setData(result)
        setError(null)
        setStep(0)
        setPlaying(false)
      })
      .catch((e) => live && setError(e.message))
    return () => {
      live = false
    }
  }, [network, cases, depth, contactSettings])

  const total = data?.steps.length ?? 0

  // Autoplay: one step per tick, stopping at the end.
  useEffect(() => {
    if (!playing) return
    const timer = setInterval(() => {
      setStep((s) => {
        if (s >= total) {
          setPlaying(false)
          return s
        }
        return s + 1
      })
    }, SPEEDS[speed])
    return () => clearInterval(timer)
  }, [playing, speed, total])

  // Keep the current step visible in the list.
  useEffect(() => {
    listRef.current?.querySelector('[data-current="true"]')?.scrollIntoView({ block: 'nearest' })
  }, [step])

  // Replay the first `step` recorded steps: who has a dose, which links are protected.
  const state = useMemo(() => {
    if (!data) return null
    const dosed = new Set(data.ring1)
    let forced = 0
    let matched = 0
    for (const s of data.steps.slice(0, step)) {
      if (s.kind === 'FORCED') {
        dosed.add(s.person)
        forced++
      } else if (s.kind === 'MATCHED') {
        dosed.add(s.a)
        dosed.add(s.b)
        matched++
      }
    }
    const covered = new Set()
    for (const l of ring?.targetLinks ?? []) {
      if (dosed.has(l.a) || dosed.has(l.b)) covered.add(linkKey(l.a, l.b))
    }
    const current = step > 0 ? data.steps[step - 1] : null
    return {
      dosed,
      covered,
      forced,
      matched,
      current,
      bound: data.ring1.length + forced + matched,
      plan: { people: [...dosed].map((personId) => ({ personId })), unprotectedLinks: [] },
      focus: current && current.kind !== 'FORCED' ? { a: current.a, b: current.b, kind: current.kind } : current ? { a: current.person, b: current.b, kind: 'MATCHED' } : null,
    }
  }, [data, step, ring])

  if (restoring) return null
  if (!network || !view) return <Navigate to="/load" replace />
  if (cases.length === 0) return <NeedCases what="the walkthrough" />
  if (error) {
    return (
      <Notice tone="warning" title="Too big to step through">
        {error} The “Neighbourhood outbreak” and “Greedy trap” samples work well.
      </Notice>
    )
  }
  if (!data || !state) return null

  const done = step >= total
  const current = state.current

  return (
    <div className="grid grid-cols-1 gap-4 lg:h-[calc(100dvh-180px)] lg:grid-cols-[minmax(0,1fr)_380px]">
      <div className="glass relative min-h-[60vh] overflow-hidden rounded-[24px] lg:min-h-0">
        <CurvedGrid />
        <NetworkCanvas view={view} ring={ring} plan={state.plan} cases={cases} focus={state.focus} covered={state.covered} />
        <div className="glass glass-strong absolute top-3 left-3 flex flex-wrap gap-2 rounded-2xl px-3 py-2 text-xs font-semibold">
          <span className="flex items-center gap-1.5"><span className="h-1 w-5 rounded bg-[var(--accent)]" /> link being checked</span>
          <span className="flex items-center gap-1.5"><span className="h-1 w-5 rounded bg-[var(--vaccinated)]" /> protected</span>
          <span className="flex items-center gap-1.5"><span className="h-1 w-5 rounded bg-[var(--ring2)]" /> still to protect</span>
        </div>
      </div>

      <div className="scroll-thin flex flex-col gap-4 lg:min-h-0 lg:overflow-y-auto">
        <GlassPanel>
          <PanelTitle icon={ShieldCheck} title="Maximal matching" subtitle={`Step ${step} of ${total}`} />
          <div className="mb-3 grid grid-cols-3 gap-3">
            <TextStat label="Doses so far" value={state.dosed.size} tone="var(--planned)" />
            <TextStat label="Minimum ≥" value={state.bound} tone="var(--vaccinated)" />
            <TextStat label="Links left" value={(ring?.targetLinks.length ?? 0) - state.covered.size} tone="var(--ring2)" />
          </div>
          <div className="inset min-h-[92px] rounded-2xl p-3 text-sm">
            {step === 0 ? (
              <p>
                <span className="font-semibold">Start:</span> ring 1 ({data.ring1.length} people) gets doses first, because they met a case
                directly. The algorithm then scans the {total} remaining links in order. Press play.
              </p>
            ) : (
              <>
                <Pill className="mb-1.5" color={current.kind === 'MATCHED' ? 'var(--accent)' : current.kind === 'FORCED' ? 'var(--excluded)' : 'var(--text-muted)'}>
                  {current.kind === 'MATCHED' ? 'Matched: 2 doses' : current.kind === 'FORCED' ? 'Forced: 1 dose' : 'Skipped'}
                </Pill>
                <p>{current.explanation}</p>
              </>
            )}
            {done && total > 0 && (
              <p className="mt-2 font-semibold text-vaccinated">
                Done: {data.doses} doses. Any plan must vaccinate ring 1 ({data.ring1.length}) and the forced people ({state.forced}). The{' '}
                {state.matched} matched links share no person, so each needs at least one more dose: the minimum is at least{' '}
                {data.lowerBound}, and {data.doses} ≤ 2 × {data.lowerBound}.
              </p>
            )}
          </div>
          <div className="mt-3 flex items-center gap-2">
            <IconButton icon={RotateCcw} label="Back to the start" onClick={() => {
                setPlaying(false)
                setStep(0)
              }} />
            <IconButton icon={ChevronLeft} label="Previous step" disabled={step === 0} onClick={() => setStep(step - 1)} />
            <Button variant="primary" className="flex-1" icon={playing ? Pause : Play} disabled={done} onClick={() => setPlaying(!playing)}>
              {playing ? 'Pause' : step === 0 ? 'Play' : 'Continue'}
            </Button>
            <IconButton icon={ChevronRight} label="Next step" disabled={done} onClick={() => setStep(step + 1)} />
          </div>
          <div className="mt-3 flex items-center justify-between gap-2">
            <input
              type="range"
              aria-label="Step"
              className="w-full accent-[var(--accent)]"
              min={0}
              max={total}
              value={step}
              onChange={(e) => {
                setPlaying(false)
                setStep(Number(e.target.value))
              }}
            />
            <Segmented
              size="sm"
              label="Speed"
              value={speed}
              onChange={setSpeed}
              options={[
                { value: 'slow', label: 'Slow' },
                { value: 'normal', label: '1×' },
                { value: 'fast', label: 'Fast' },
              ]}
            />
          </div>
        </GlassPanel>

        <GlassPanel delay={0.04} className="flex min-h-0 flex-col">
          <PanelTitle title="Every step" subtitle="Click one to jump there" />
          <ol ref={listRef} className="scroll-thin max-h-[34vh] overflow-y-auto pr-1 text-xs">
            {data.steps.map((s) => (
              <li key={s.index}>
                <button
                  type="button"
                  data-current={s.index === step}
                  onClick={() => {
                    setPlaying(false)
                    setStep(s.index)
                  }}
                  className={cn(
                    'flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left hover:bg-accent-soft',
                    s.index === step && 'bg-accent-soft font-semibold',
                    s.index > step && 'opacity-50',
                  )}
                >
                  <span className="w-7 text-faint">{s.index}</span>
                  <span className="flex-1">{s.kind === 'FORCED' ? `${s.person} (only option with ${s.b})` : `${s.a} – ${s.b}`}</span>
                  <span className={s.kind === 'MATCHED' ? 'text-accent' : s.kind === 'FORCED' ? 'text-excluded' : 'text-faint'}>
                    {s.kind === 'MATCHED' ? '+2 doses' : s.kind === 'FORCED' ? '+1 dose' : 'skip'}
                  </span>
                </button>
              </li>
            ))}
          </ol>
        </GlassPanel>
      </div>
    </div>
  )
}

// ------------------------------------------------------------------ Bloom filter

const KEY_COLORS = ['accent', 'ring1', 'vaccinated', 'ring2', 'planned', 'case', 'warning', 'excluded']
const DEFAULT_KEYS = 'V001|2026-11-01T16\nV002|2026-11-01T18\nV005|2026-11-01T16\nV005|2026-11-01T17'

function BloomWalkthrough() {
  const [m, setM] = useState(32)
  const [k, setK] = useState(3)
  const [keysText, setKeysText] = useState(DEFAULT_KEYS)
  const [query, setQuery] = useState('V002|2026-11-01T18')
  const [result, setResult] = useState(null)
  const [focusKey, setFocusKey] = useState(null)
  const [searching, setSearching] = useState(false)
  const [chips] = useAutoAnimate()

  const keys = useMemo(() => keysText.split('\n').map((s) => s.trim()).filter(Boolean).slice(0, 20), [keysText])

  useEffect(() => {
    if (!query.trim()) return
    const timer = setTimeout(() => {
      bloomDemo({ m, k, keys, query })
        .then(setResult)
        .catch((e) => toast.error('Could not build the filter', { description: e.message }))
    }, 200)
    return () => clearTimeout(timer)
  }, [m, k, keys, query])

  // Asks the Java filter about made-up keys, 25 at a time, until one is a false positive.
  const findFalsePositive = async () => {
    setSearching(true)
    try {
      for (let start = 0; start < 300; start += 25) {
        const candidates = Array.from({ length: 25 }, (_, j) => {
          const i = start + j + 1
          return `V${String(100 + i).padStart(3, '0')}|2026-11-${String(2 + (i % 27)).padStart(2, '0')}T${String(i % 24).padStart(2, '0')}`
        })
        const answers = await Promise.all(candidates.map((candidate) => bloomDemo({ m, k, keys, query: candidate })))
        const hit = answers.findIndex((r) => r.verdict === 'FALSE_POSITIVE')
        if (hit >= 0) {
          const tries = start + hit + 1
          setQuery(candidates[hit])
          setResult(answers[hit])
          toast.success(`Found one after ${tries} ${tries === 1 ? 'try' : 'tries'}`, {
            description: `${candidates[hit]} was never added, but all its bits are 1.`,
          })
          return
        }
      }
      toast.info('No false positive in 300 tries', { description: 'Add more keys or lower m to fill more bits.' })
    } catch (e) {
      toast.error('Search stopped', { description: e.message })
    } finally {
      setSearching(false)
    }
  }

  const queryBits = new Set(result?.query.positions ?? [])
  const focusBits = new Set(focusKey == null ? [] : (result?.keys[focusKey]?.positions ?? []))
  const filled = result ? result.setBy.filter((s) => s.length > 0).length : 0
  const verdictColor = { DEFINITELY_NOT: 'var(--vaccinated)', PRESENT: 'var(--accent)', FALSE_POSITIVE: 'var(--case)' }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[360px_minmax(0,1fr)]">
      <div className="flex flex-col gap-4">
        <GlassPanel>
          <PanelTitle icon={Filter} title="A tiny filter" subtitle="Small enough to see every bit" />
          <div className="flex flex-col gap-4">
            <Slider label="Bits (m)" value={m} onChange={setM} min={8} max={128} step={8} />
            <Slider label="Hash functions (k)" value={k} onChange={setK} min={1} max={6} />
            <label className="block">
              <span className="text-xs font-medium text-muted">Keys added (one per line, at most 20)</span>
              <textarea
                className="inset mt-1 h-32 w-full resize-none rounded-xl p-3 font-mono text-xs text-text outline-none focus:border-accent"
                value={keysText}
                onChange={(e) => setKeysText(e.target.value)}
              />
            </label>
            <Field label="Look up" value={query} onChange={(e) => setQuery(e.target.value)} />
            <Button icon={Search} loading={searching} onClick={findFalsePositive}>
              Find a false positive
            </Button>
          </div>
        </GlassPanel>
      </div>

      <div className="flex min-w-0 flex-col gap-4">
        {result && (
          <>
            <GlassPanel>
              <PanelTitle title={`The bit array: ${filled} of ${m} bits are 1`} subtitle="Each key sets k bits, chosen by (FNV-1a + i × MurmurHash3) mod m. Hover a key to see its bits." />
              <div className="grid gap-1.5" style={{ gridTemplateColumns: `repeat(${Math.min(m, 16)}, minmax(0, 1fr))` }}>
                {result.setBy.map((owners, bit) => {
                  const isQuery = queryBits.has(bit)
                  const isFocus = focusBits.has(bit)
                  const color = owners.length ? `var(--${KEY_COLORS[owners[0] % KEY_COLORS.length]})` : null
                  return (
                    <div
                      key={bit}
                      title={`bit ${bit}: ${owners.length ? `set by ${owners.map((o) => result.keys[o].key).join(', ')}` : '0'}`}
                      className={cn(
                        'relative grid aspect-square place-items-center rounded-lg border text-[11px] font-semibold transition',
                        owners.length ? 'border-transparent text-white' : 'border-[var(--glass-border)] text-faint',
                        isQuery && 'ring-2 ring-[var(--text)] ring-offset-2 ring-offset-[var(--glass-strong)]',
                        focusKey != null && !isFocus && 'opacity-30',
                      )}
                      style={owners.length ? { background: color } : undefined}
                    >
                      {owners.length ? 1 : 0}
                      <span className="absolute bottom-0.5 text-[8px] opacity-70">{bit}</span>
                    </div>
                  )
                })}
              </div>
              <div ref={chips} className="mt-4 flex flex-wrap gap-1.5">
                {result.keys.map((key, i) => (
                  <button
                    key={key.key}
                    type="button"
                    onMouseEnter={() => setFocusKey(i)}
                    onMouseLeave={() => setFocusKey(null)}
                    onFocus={() => setFocusKey(i)}
                    onBlur={() => setFocusKey(null)}
                    className="rounded-full"
                  >
                    <Pill color={`var(--${KEY_COLORS[i % KEY_COLORS.length]})`}>
                      {key.key} → {key.positions.join(', ')}
                    </Pill>
                  </button>
                ))}
              </div>
            </GlassPanel>

            <GlassPanel delay={0.04} className={result.verdict === 'FALSE_POSITIVE' ? 'border-case/50' : ''}>
              <PanelTitle title={`Looking up “${result.query.key}”`} subtitle={`Its bits: ${result.query.positions.join(', ')} (outlined above)`} />
              <Pill color={verdictColor[result.verdict]}>
                {result.verdict === 'DEFINITELY_NOT' ? 'Definitely not added' : result.verdict === 'PRESENT' ? 'Possibly added (and it was)' : 'Possibly added: false positive'}
              </Pill>
              <p className="mt-2 text-sm">{result.explanation}</p>
              <p className="mt-2 text-xs text-muted">
                With {result.keys.length} keys in {m} bits and k = {k}, the formula (1 − e^(−kn/m))^k predicts a{' '}
                {(result.expectedFalsePositiveRate * 100).toFixed(1)}% chance that a key never added still finds all its bits set.
              </p>
            </GlassPanel>
          </>
        )}
        <Notice tone="info" title="Why the app can trust a “no” but not a “yes”">
          Adding a key only ever turns bits on, so a key that was added always finds all its bits set: no false negatives. A key
          that was never added can find its bits set by other keys. That is why the exposure check and the double-dose guard
          confirm every “possibly” against the exact records.
        </Notice>
      </div>
    </div>
  )
}
