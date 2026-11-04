import { motion } from 'motion/react'
import { ArrowRight, LoaderCircle } from 'lucide-react'

const PALETTE = ['var(--case)', 'var(--ring1)', 'var(--ring2)', 'var(--vaccinated)', 'var(--planned)', 'var(--excluded)']

/** Small seeded random generator so each sample always gets the same picture. */
function seeded(text) {
  let h = 2166136261
  for (let i = 0; i < text.length; i++) h = Math.imul(h ^ text.charCodeAt(i), 16777619)
  return () => {
    h = Math.imul(h ^ (h >>> 15), 2246822507)
    h = Math.imul(h ^ (h >>> 13), 3266489909)
    return ((h ^= h >>> 16) >>> 0) / 4294967296
  }
}

/** Decorative mini network: a case in the middle with two rings around it. */
function Illustration({ id }) {
  const rand = seeded(id)
  const nodes = [{ x: 100, y: 90, r: 7, c: PALETTE[0] }]
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2 + rand()
    nodes.push({ x: 100 + Math.cos(a) * 38, y: 90 + Math.sin(a) * 34, r: 4.5, c: PALETTE[1] })
  }
  for (let i = 0; i < 11; i++) {
    const a = (i / 11) * Math.PI * 2 + rand() * 0.5
    const d = 66 + rand() * 12
    nodes.push({ x: 100 + Math.cos(a) * d, y: 90 + Math.sin(a) * d * 0.8, r: 3.5, c: rand() > 0.75 ? PALETTE[3] : PALETTE[2] })
  }
  const edges = []
  for (let i = 1; i <= 6; i++) edges.push([0, i])
  for (let i = 7; i < nodes.length; i++) edges.push([i, 1 + Math.floor(rand() * 6)])
  return (
    <svg viewBox="0 0 200 180" className="h-full w-full" aria-hidden="true">
      <circle cx="100" cy="90" r="38" fill="none" stroke="var(--ring1)" strokeOpacity="0.25" strokeDasharray="3 4" />
      <circle cx="100" cy="90" r="70" fill="none" stroke="var(--ring2)" strokeOpacity="0.2" strokeDasharray="3 4" />
      {edges.map(([a, b], i) => (
        <line key={i} x1={nodes[a].x} y1={nodes[a].y} x2={nodes[b].x} y2={nodes[b].y} stroke="var(--edge)" />
      ))}
      {nodes.map((n, i) => (
        <circle key={i} cx={n.x} cy={n.y} r={n.r} fill={n.c} />
      ))}
    </svg>
  )
}

/** Tall floating card for one sample network. */
export function SampleCard({ sample, index, loading, onOpen }) {
  return (
    <motion.button
      type="button"
      onClick={onOpen}
      disabled={loading}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.04 * index, duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -4 }}
      whileTap={{ scale: 0.98 }}
      className="glass group relative flex h-64 w-56 shrink-0 snap-start flex-col overflow-hidden rounded-[20px] p-4 text-left transition-shadow hover:shadow-[0_20px_50px_-25px_var(--accent)]"
    >
      <p className="relative z-10 text-[15px] leading-tight font-semibold tracking-tight">{sample.label}</p>
      <p className="relative z-10 mt-1 line-clamp-2 min-h-[2.8em] text-xs leading-snug text-muted">
        {sample.description}
      </p>
      <div className="relative -mx-2 my-1 min-h-0 flex-1">
        <Illustration id={sample.id} />
      </div>
      <span className="flex items-center justify-between text-xs font-semibold text-accent">
        {loading ? (
          <span className="flex items-center gap-2">
            <LoaderCircle className="size-4 animate-spin" /> Loading
          </span>
        ) : (
          <span>Open sample</span>
        )}
        <ArrowRight className="size-4 transition group-hover:translate-x-0.5" />
      </span>
    </motion.button>
  )
}
