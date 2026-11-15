import NumberFlow from '@number-flow/react'
import { motion } from 'motion/react'
import { useEffect, useState } from 'react'

/*
 * Small illustrations for the project story. They are drawings, not results:
 * every real number on the story slides comes from the benchmarks and tests,
 * and every real algorithm runs in the Java backend.
 */

const ease = [0.22, 1, 0.36, 1]

/** Counts 0, 1, 2 … steps-1 on a timer, then holds and starts again. */
function useStepper(steps, ms, hold = 2) {
  const [step, setStep] = useState(0)
  useEffect(() => {
    const id = setInterval(() => setStep((s) => (s + 1) % (steps + hold)), ms)
    return () => clearInterval(id)
  }, [steps, ms, hold])
  return Math.min(step, steps - 1)
}

function Frame({ children, viewBox = '0 0 400 300', label }) {
  return (
    <svg viewBox={viewBox} className="h-full w-full overflow-visible" role="img" aria-label={label}>
      {children}
    </svg>
  )
}

// ------------------------------------------------------------------ the sample ring

const NODES = [
  [200, 150, 0],
  [130, 95, 1],
  [275, 90, 1],
  [285, 205, 1],
  [125, 210, 1],
  [60, 50, 2],
  [205, 35, 2],
  [350, 45, 2],
  [365, 150, 2],
  [340, 265, 2],
  [200, 272, 2],
  [55, 250, 2],
  [40, 145, 2],
]
const EDGES = [
  [0, 1], [0, 2], [0, 3], [0, 4], [1, 2], [3, 4], [1, 5], [1, 6], [2, 6],
  [2, 7], [2, 8], [3, 8], [3, 9], [3, 10], [4, 10], [4, 11], [4, 12], [1, 12],
]
const LAYER_COLOUR = ['var(--case)', 'var(--ring1)', 'var(--ring2)']

function Edge({ a, b, state = 'plain', delay = 0 }) {
  const [x1, y1] = NODES[a]
  const [x2, y2] = NODES[b]
  const stroke = state === 'picked' ? 'var(--planned)' : state === 'safe' ? 'var(--vaccinated)' : 'var(--edge)'
  return (
    <motion.line
      x1={x1}
      y1={y1}
      x2={x2}
      y2={y2}
      initial={{ pathLength: 0, opacity: 0 }}
      animate={{ pathLength: 1, opacity: state === 'faded' ? 0.25 : 1 }}
      transition={{ duration: 0.7, delay, ease }}
      style={{ stroke, strokeWidth: state === 'picked' ? 4 : 2, transition: 'stroke 0.4s, stroke-width 0.4s' }}
      strokeLinecap="round"
    />
  )
}

function Node({ i, fill = 'var(--node)', delay = 0, ring = false }) {
  const [x, y] = NODES[i]
  return (
    <g>
      {ring && (
        <motion.circle
          cx={x}
          cy={y}
          r={14}
          initial={{ scale: 0.4, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.4, ease }}
          style={{ fill: 'none', stroke: 'var(--planned)', strokeWidth: 2.5, transformBox: 'fill-box', transformOrigin: 'center' }}
        />
      )}
      <motion.circle
        cx={x}
        cy={y}
        r={8}
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: 'spring', stiffness: 260, damping: 18, delay }}
        style={{ fill, transition: 'fill 0.5s', transformBox: 'fill-box', transformOrigin: 'center' }}
      />
    </g>
  )
}

/** Title slide: one case and the rings that spread out from it. */
export function RingsPulse() {
  return (
    <Frame label="A case with two rings of contacts around it">
      {[0, 1, 2].map((i) => (
        <motion.circle
          key={i}
          cx={200}
          cy={150}
          r={130}
          initial={{ scale: 0.1, opacity: 0.9 }}
          animate={{ scale: 1, opacity: 0 }}
          transition={{ duration: 3.6, delay: i * 1.2, repeat: Infinity, ease: 'easeOut' }}
          style={{ fill: 'none', stroke: 'var(--ring1)', strokeWidth: 1.5, transformBox: 'fill-box', transformOrigin: 'center' }}
        />
      ))}
      <circle cx={200} cy={150} r={70} style={{ fill: 'none', stroke: 'var(--ring1)', strokeOpacity: 0.35, strokeDasharray: '4 6' }} />
      <circle cx={200} cy={150} r={125} style={{ fill: 'none', stroke: 'var(--ring2)', strokeOpacity: 0.3, strokeDasharray: '4 6' }} />
      {Array.from({ length: 6 }, (_, i) => {
        const a = (i / 6) * Math.PI * 2 + 0.3
        return (
          <motion.circle
            key={`a${i}`}
            cx={200 + 70 * Math.cos(a)}
            cy={150 + 70 * Math.sin(a)}
            r={7}
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.3 + i * 0.08, type: 'spring' }}
            style={{ fill: 'var(--ring1)', transformBox: 'fill-box', transformOrigin: 'center' }}
          />
        )
      })}
      {Array.from({ length: 11 }, (_, i) => {
        const a = (i / 11) * Math.PI * 2
        return (
          <motion.circle
            key={`b${i}`}
            cx={200 + 125 * Math.cos(a)}
            cy={150 + 125 * Math.sin(a)}
            r={6}
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.8 + i * 0.05, type: 'spring' }}
            style={{ fill: 'var(--ring2)', transformBox: 'fill-box', transformOrigin: 'center' }}
          />
        )
      })}
      <motion.circle
        cx={200}
        cy={150}
        r={13}
        animate={{ scale: [1, 1.15, 1] }}
        transition={{ duration: 1.6, repeat: Infinity }}
        style={{ fill: 'var(--case)', transformBox: 'fill-box', transformOrigin: 'center' }}
      />
    </Frame>
  )
}

/** People become nodes, contacts become links. */
export function NetworkForms() {
  return (
    <Frame label="People drawn as points and contacts drawn as lines between them">
      {EDGES.map(([a, b], i) => (
        <Edge key={i} a={a} b={b} delay={0.6 + i * 0.05} />
      ))}
      {NODES.map((_, i) => (
        <Node key={i} i={i} delay={i * 0.04} />
      ))}
    </Frame>
  )
}

/** Breadth-first search colours the network one layer at a time. */
export function BfsLayers() {
  const layer = useStepper(3, 900, 3)
  return (
    <Frame label="Breadth-first search finds ring 1 and then ring 2">
      {EDGES.map(([a, b], i) => (
        <Edge key={i} a={a} b={b} delay={0} />
      ))}
      {NODES.map(([, , l], i) => (
        <Node key={i} i={i} fill={l <= layer ? LAYER_COLOUR[l] : 'var(--node)'} />
      ))}
      <Legend
        items={[
          ['Case', 'var(--case)'],
          ['Ring 1', 'var(--ring1)'],
          ['Ring 2', 'var(--ring2)'],
        ]}
      />
    </Frame>
  )
}

function Legend({ items }) {
  return (
    <g transform="translate(0 302)">
      {items.map(([label, colour], i) => (
        <g key={label} transform={`translate(${i * 100} 0)`}>
          <circle cx={6} cy={6} r={5} style={{ fill: colour }} />
          <text x={16} y={10} style={{ fill: 'var(--text-muted)', fontSize: 12, fontWeight: 600 }}>
            {label}
          </text>
        </g>
      ))}
    </g>
  )
}

/** A vertex cover: every link needs a vaccinated person on at least one end. */
export function CoverEdges() {
  const order = [1, 2, 3, 4]
  const step = useStepper(order.length + 1, 900, 3)
  const chosen = new Set(order.slice(0, step))
  return (
    <Frame label="Four vaccinated people protect every link">
      {EDGES.map(([a, b], i) => (
        <Edge key={i} a={a} b={b} state={chosen.has(a) || chosen.has(b) ? 'safe' : 'plain'} />
      ))}
      {NODES.map((_, i) => (
        <Node key={i} i={i} fill={chosen.has(i) ? 'var(--planned)' : 'var(--node)'} ring={chosen.has(i)} />
      ))}
      <Legend
        items={[
          ['Vaccinated', 'var(--planned)'],
          ['Protected link', 'var(--vaccinated)'],
        ]}
      />
    </Frame>
  )
}

/** The 2-approximation: take any unprotected link and vaccinate both ends. */
export function MatchingSteps() {
  const picks = [
    [0, 1],
    [2, 7],
    [3, 9],
    [4, 11],
  ]
  const step = useStepper(picks.length + 1, 1100, 3)
  const chosen = new Set(picks.slice(0, step).flat())
  const picked = new Set(picks.slice(0, step).map(([a, b]) => `${a}-${b}`))
  return (
    <Frame label="The approximation picks an unprotected link and vaccinates both ends">
      {EDGES.map(([a, b], i) => {
        const key = `${a}-${b}`
        const state = picked.has(key) ? 'picked' : chosen.has(a) || chosen.has(b) ? 'safe' : 'plain'
        return <Edge key={i} a={a} b={b} state={state} />
      })}
      {NODES.map((_, i) => (
        <Node key={i} i={i} fill={chosen.has(i) ? 'var(--planned)' : 'var(--node)'} ring={chosen.has(i)} />
      ))}
      <text x={400} y={20} textAnchor="end" style={{ fill: 'var(--text)', fontSize: 16, fontWeight: 700 }}>
        {chosen.size} doses
      </text>
    </Frame>
  )
}

/** Branch and bound: a search tree where hopeless branches are cut. */
export function BranchTree() {
  const tree = [
    { x: 200, y: 30, p: null, cut: false },
    { x: 110, y: 110, p: 0, cut: false },
    { x: 290, y: 110, p: 0, cut: true },
    { x: 60, y: 190, p: 1, cut: false },
    { x: 160, y: 190, p: 1, cut: true },
    { x: 30, y: 270, p: 3, cut: true },
    { x: 95, y: 270, p: 3, cut: false, best: true },
  ]
  return (
    <Frame label="A search tree with cut branches and one best answer">
      {tree.map((n, i) => {
        if (n.p === null) return null
        const p = tree[n.p]
        return (
          <motion.line
            key={`e${i}`}
            x1={p.x}
            y1={p.y}
            x2={n.x}
            y2={n.y}
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ delay: i * 0.35, duration: 0.4 }}
            style={{ stroke: n.cut ? 'var(--danger)' : 'var(--edge-solid)', strokeWidth: 2, strokeDasharray: n.cut ? '5 5' : 'none', opacity: n.cut ? 0.6 : 0.8 }}
          />
        )
      })}
      {tree.map((n, i) => (
        <motion.g key={i} initial={{ opacity: 0, scale: 0.5 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: i * 0.35 + 0.2 }} style={{ transformBox: 'fill-box', transformOrigin: 'center' }}>
          <circle cx={n.x} cy={n.y} r={16} style={{ fill: n.best ? 'var(--vaccinated)' : n.cut ? 'transparent' : 'var(--glass-strong)', stroke: n.cut ? 'var(--danger)' : 'var(--glass-border)', strokeWidth: 2 }} />
          {n.cut && (
            <text x={n.x} y={n.y + 5} textAnchor="middle" style={{ fill: 'var(--danger)', fontSize: 15, fontWeight: 800 }}>
              ✕
            </text>
          )}
          {n.best && (
            <text x={n.x} y={n.y + 5} textAnchor="middle" style={{ fill: 'white', fontSize: 14, fontWeight: 800 }}>
              ✓
            </text>
          )}
        </motion.g>
      ))}
      <motion.text x={330} y={200} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 2.6 }} style={{ fill: 'var(--text-muted)', fontSize: 13, fontWeight: 600 }}>
        <tspan x={250} dy={0}>Cut: this branch can't</tspan>
        <tspan x={250} dy={18}>beat the best so far</tspan>
      </motion.text>
    </Frame>
  )
}

/** Indexed max-heap: the most connected person rises to the top; neighbours drop. */
export function HeapBars() {
  const states = [
    { A: 6, B: 4, C: 4, D: 3, E: 2, F: 1 },
    { B: 3, C: 3, D: 2, E: 2, F: 1 },
    { C: 2, D: 2, E: 1, F: 1 },
    { D: 1, E: 1, F: 0 },
  ]
  const step = useStepper(states.length, 1300, 2)
  const rows = Object.entries(states[step]).sort((x, y) => y[1] - x[1] || x[0].localeCompare(y[0]))
  return (
    <div className="flex h-full w-full flex-col justify-center gap-2.5" role="img" aria-label="A heap keeps the person with the most unprotected links on top">
      {rows.map(([name, degree], i) => (
        <motion.div key={name} layout transition={{ type: 'spring', stiffness: 300, damping: 30 }} className="flex items-center gap-3">
          <span className={`grid size-9 place-items-center rounded-full text-sm font-bold ${i === 0 ? 'bg-planned text-white' : 'inset text-muted'}`}>{name}</span>
          <motion.div
            className={`h-9 rounded-full ${i === 0 ? 'bg-planned' : 'bg-[var(--edge)]'}`}
            animate={{ width: `${Math.max(degree, 0.15) * 14}%` }}
            transition={{ duration: 0.6, ease }}
          />
          <span className="text-sm font-semibold text-muted">{degree}</span>
        </motion.div>
      ))}
      <p className="mt-2 text-sm text-faint">Top of the heap gets the dose; its neighbours' keys drop (decrease-key).</p>
    </div>
  )
}

/** Bar pairs for the greedy trap graphs. */
export function TrapBars() {
  const groups = [
    { name: 'Trap graph A', optimum: 12, greedy: 23 },
    { name: 'Trap graph B', optimum: 18, greedy: 40 },
  ]
  const max = 42
  return (
    <div className="flex h-full w-full flex-col justify-center gap-8" role="img" aria-label="Greedy uses 23 doses where 12 are enough, and 40 where 18 are enough">
      {groups.map((g, gi) => (
        <div key={g.name} className="space-y-2">
          <p className="text-sm font-semibold text-muted">{g.name}</p>
          {[
            ['Fewest possible', g.optimum, 'bg-vaccinated'],
            ['Greedy', g.greedy, 'bg-danger'],
          ].map(([label, value, colour], i) => (
            <div key={label} className="flex items-center gap-3">
              <span className="w-32 shrink-0 text-sm text-faint">{label}</span>
              <div className="relative h-8 flex-1">
                <motion.div
                  className={`h-8 rounded-full ${colour}`}
                  initial={{ width: 0 }}
                  animate={{ width: `${(value / max) * 100}%` }}
                  transition={{ duration: 1, delay: 0.3 + gi * 0.6 + i * 0.25, ease }}
                />
                {i === 0 && (
                  <motion.div
                    className="absolute top-[-6px] h-[calc(200%+14px)] border-l-2 border-dashed border-accent"
                    style={{ left: `${((g.optimum * 2) / max) * 100}%` }}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 1.8 + gi * 0.4 }}
                  />
                )}
              </div>
              <span className="w-8 text-right font-bold">{value}</span>
            </div>
          ))}
        </div>
      ))}
      <p className="text-sm text-faint">
        <span className="mr-2 inline-block h-3 border-l-2 border-dashed border-accent align-middle" />
        Twice the fewest: the approximation can never cross this line
      </p>
    </div>
  )
}

/** Quickselect: partition around a pivot and keep only the side with the answer. */
export function SelectNarrow() {
  const values = [7, 2, 9, 4, 12, 5, 1, 10, 3, 8, 6, 11]
  const ranges = [
    [0, 11],
    [5, 11],
    [8, 11],
    [9, 9],
  ]
  const step = useStepper(ranges.length, 1200, 2)
  const sorted = [...values].sort((a, b) => a - b)
  const shown = step === 0 ? values : sorted
  const [lo, hi] = ranges[step]
  return (
    <div className="flex h-full w-full flex-col justify-center gap-6" role="img" aria-label="Quickselect narrows the search to one side each round">
      <div className="flex h-48 items-end gap-2">
        {shown.map((v, i) => {
          const inside = i >= lo && i <= hi
          const found = lo === hi && inside
          return (
            <motion.div
              key={v}
              layout
              transition={{ type: 'spring', stiffness: 220, damping: 26 }}
              className={`flex-1 rounded-t-lg ${found ? 'bg-planned' : inside ? 'bg-accent' : 'bg-[var(--edge)]'}`}
              style={{ height: `${v * 8}%`, opacity: inside ? 1 : 0.45 }}
            />
          )
        })}
      </div>
      <p className="text-sm text-faint">Each round throws away the side that can't hold the answer: expected O(n), not O(n log n).</p>
    </div>
  )
}

/** Union-find: clusters merging as new contacts arrive. */
export function DsuMerge() {
  const layouts = [
    [[60, 80], [120, 60], [100, 130], [280, 70], [340, 110], [300, 150], [180, 230], [240, 250]],
    [[110, 100], [150, 80], [140, 140], [280, 70], [340, 110], [300, 150], [180, 230], [240, 250]],
    [[180, 110], [220, 90], [210, 150], [250, 110], [290, 130], [260, 160], [180, 230], [240, 250]],
    [[160, 120], [200, 100], [190, 160], [240, 120], [280, 140], [250, 170], [200, 210], [240, 220]],
  ]
  const groupColour = [
    ['var(--case)', 'var(--case)', 'var(--case)', 'var(--ring1)', 'var(--ring1)', 'var(--ring1)', 'var(--planned)', 'var(--vaccinated)'],
    ['var(--case)', 'var(--case)', 'var(--case)', 'var(--ring1)', 'var(--ring1)', 'var(--ring1)', 'var(--planned)', 'var(--vaccinated)'],
    ['var(--case)', 'var(--case)', 'var(--case)', 'var(--case)', 'var(--case)', 'var(--case)', 'var(--planned)', 'var(--vaccinated)'],
    Array(8).fill('var(--case)'),
  ]
  const counts = [4, 4, 3, 1]
  const step = useStepper(layouts.length, 1300, 2)
  return (
    <Frame label="Separate outbreak clusters merging into one as contacts are reported">
      {layouts[step].map(([x, y], i) => (
        <motion.circle
          key={i}
          r={13}
          animate={{ cx: x, cy: y }}
          initial={false}
          transition={{ type: 'spring', stiffness: 120, damping: 16 }}
          style={{ fill: groupColour[step][i], transition: 'fill 0.5s' }}
        />
      ))}
      <text x={200} y={295} textAnchor="middle" style={{ fill: 'var(--text)', fontSize: 16, fontWeight: 700 }}>
        {counts[step]} {counts[step] === 1 ? 'cluster' : 'clusters'}
      </text>
    </Frame>
  )
}

/** Hashing a venue visit to k bit positions. */
export function HashBits() {
  const bits = 16
  const hits = [2, 9, 13]
  return (
    <Frame viewBox="0 0 400 260" label="A venue visit is hashed to three bit positions">
      <motion.g initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>
        <rect x={95} y={10} width={210} height={44} rx={22} style={{ fill: 'var(--glass-strong)', stroke: 'var(--glass-border)' }} />
        <text x={200} y={37} textAnchor="middle" style={{ fill: 'var(--text)', fontSize: 14, fontWeight: 700, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}>
          V07|2026-11-02T18
        </text>
      </motion.g>
      {hits.map((h, i) => (
        <motion.path
          key={h}
          d={`M200 58 C 200 130, ${20 + h * 23 + 9} 120, ${20 + h * 23 + 9} 196`}
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ delay: 0.6 + i * 0.4, duration: 0.7, ease }}
          style={{ fill: 'none', stroke: 'var(--accent)', strokeWidth: 2 }}
        />
      ))}
      {Array.from({ length: bits }, (_, i) => {
        const on = hits.indexOf(i)
        return (
          <g key={i}>
            <motion.rect
              x={20 + i * 23}
              y={198}
              width={19}
              height={30}
              rx={5}
              initial={{ opacity: 0.5 }}
              animate={{ opacity: 1 }}
              transition={{ delay: on >= 0 ? 1.2 + on * 0.4 : 0 }}
              style={{
                fill: on >= 0 ? 'var(--accent)' : 'var(--glass-inset)',
                stroke: 'var(--glass-border)',
                transition: `fill 0.3s ${on >= 0 ? 1.2 + on * 0.4 : 0}s`,
              }}
            />
            <text x={29.5 + i * 23} y={248} textAnchor="middle" style={{ fill: 'var(--text-faint)', fontSize: 10 }}>
              {i}
            </text>
          </g>
        )
      })}
      <motion.text x={20} y={110} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 2.4 }} style={{ fill: 'var(--text-muted)', fontSize: 13, fontWeight: 600 }}>
        h₁ + i·h₂
      </motion.text>
    </Frame>
  )
}

/** A large animated number with a caption. */
export function BigNumber({ value, suffix = '', prefix = '', decimals = 0, caption }) {
  const [shown, setShown] = useState(0)
  useEffect(() => {
    const id = setTimeout(() => setShown(value), 250)
    return () => clearTimeout(id)
  }, [value])
  return (
    <div className="flex h-full flex-col items-center justify-center text-center">
      <div className="text-[clamp(4rem,11vw,9rem)] leading-none font-extrabold tracking-tight">
        {prefix}
        <NumberFlow value={shown} format={{ minimumFractionDigits: decimals, maximumFractionDigits: decimals }} />
        {suffix}
      </div>
      {caption && <p className="mt-4 max-w-sm text-base text-muted">{caption}</p>}
    </div>
  )
}

/** Two measured numbers side by side. */
export function VersusNumbers({ left, right }) {
  return (
    <div className="grid h-full grid-cols-2 items-center gap-6">
      {[left, right].map((s, i) => (
        <motion.div
          key={s.label}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 + i * 0.25, duration: 0.6, ease }}
          className="glass rounded-[28px] p-6 text-center"
        >
          <div className={`text-[clamp(2rem,4.5vw,3.6rem)] leading-none font-extrabold tracking-tight ${s.good ? 'text-vaccinated' : 'text-muted'}`}>
            <AnimatedValue value={s.value} decimals={s.decimals ?? 0} />
            <span className="ml-1 text-[0.45em] font-bold">{s.unit}</span>
          </div>
          <p className="mt-3 text-sm font-semibold text-muted">{s.label}</p>
        </motion.div>
      ))}
    </div>
  )
}

function AnimatedValue({ value, decimals }) {
  const [shown, setShown] = useState(0)
  useEffect(() => {
    const id = setTimeout(() => setShown(value), 400)
    return () => clearTimeout(id)
  }, [value])
  return <NumberFlow value={shown} format={{ minimumFractionDigits: decimals, maximumFractionDigits: decimals }} />
}

/** SIR curves: susceptible, infected, recovered over time (illustrative shapes). */
export function SirCurves() {
  const curves = [
    ['Susceptible', 'var(--node)', 'M10 30 C 120 30, 150 60, 190 150 S 290 250, 390 255'],
    ['Infected', 'var(--case)', 'M10 268 C 110 266, 140 120, 190 110 S 270 240, 390 262'],
    ['Recovered', 'var(--vaccinated)', 'M10 270 C 130 270, 170 220, 210 140 S 300 50, 390 45'],
  ]
  return (
    <Frame label="Susceptible, infected and recovered counts over the course of an outbreak">
      <line x1={10} y1={272} x2={390} y2={272} style={{ stroke: 'var(--glass-border)' }} />
      {curves.map(([label, colour, d], i) => (
        <motion.path
          key={label}
          d={d}
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 2, delay: i * 0.3, ease: 'easeInOut' }}
          style={{ fill: 'none', stroke: colour, strokeWidth: 4, strokeLinecap: 'round' }}
        />
      ))}
      <Legend items={curves.map(([l, c]) => [l, c])} />
    </Frame>
  )
}

/** Browser → REST API → Java engine, with requests flowing along. */
export function ArchitectureFlow() {
  const boxes = [
    { x: 10, title: 'React', sub: 'draws and asks' },
    { x: 145, title: 'REST API', sub: 'Spring Boot' },
    { x: 280, title: 'Java engine', sub: 'every algorithm' },
  ]
  return (
    <Frame viewBox="0 0 400 200" label="The React frontend calls a Spring Boot REST API, which runs the Java algorithms">
      <line x1={120} y1={100} x2={145} y2={100} style={{ stroke: 'var(--edge)', strokeWidth: 2 }} />
      <line x1={255} y1={100} x2={280} y2={100} style={{ stroke: 'var(--edge)', strokeWidth: 2 }} />
      {[0, 1, 2].map((i) => (
        <motion.circle
          key={i}
          r={4}
          cy={100}
          initial={{ cx: 120, opacity: 0 }}
          animate={{ cx: [120, 280], opacity: [0, 1, 1, 0] }}
          transition={{ duration: 1.6, delay: 1 + i * 0.55, repeat: Infinity, repeatDelay: 0.2 }}
          style={{ fill: 'var(--accent)' }}
        />
      ))}
      {boxes.map((b, i) => (
        <motion.g key={b.title} initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.25, duration: 0.5, ease }}>
          <rect x={b.x} y={55} width={110} height={90} rx={20} style={{ fill: 'var(--scene-bottom)' }} />
          <rect x={b.x} y={55} width={110} height={90} rx={20} style={{ fill: 'var(--glass-strong)', stroke: 'var(--glass-border)' }} />
          <text x={b.x + 55} y={95} textAnchor="middle" style={{ fill: 'var(--text)', fontSize: 15, fontWeight: 700 }}>
            {b.title}
          </text>
          <text x={b.x + 55} y={116} textAnchor="middle" style={{ fill: 'var(--text-faint)', fontSize: 11, fontWeight: 600 }}>
            {b.sub}
          </text>
        </motion.g>
      ))}
    </Frame>
  )
}

/** Closing: everything the project uses, appearing one by one. */
export function Toolkit({ items }) {
  return (
    <div className="flex h-full flex-wrap content-center justify-center gap-2.5">
      {items.map((t, i) => (
        <motion.span
          key={t}
          initial={{ opacity: 0, scale: 0.8, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ delay: 0.15 + i * 0.07, type: 'spring', stiffness: 260, damping: 20 }}
          className="glass rounded-full px-4 py-2 text-sm font-semibold"
        >
          {t}
        </motion.span>
      ))}
    </div>
  )
}
