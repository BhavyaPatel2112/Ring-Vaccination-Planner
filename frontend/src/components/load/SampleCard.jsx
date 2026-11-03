import { motion } from 'motion/react'
import { LoaderCircle } from 'lucide-react'

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
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.08 * index, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -8, rotateX: 4, rotateY: -4 }}
      whileTap={{ scale: 0.98 }}
      style={{ transformPerspective: 900 }}
      className="glass glass-strong group relative flex h-72 w-60 shrink-0 snap-start flex-col overflow-hidden rounded-[22px] p-4 text-left"
    >
      <p className="relative z-10 text-center text-[17px] leading-tight font-medium tracking-tight">{sample.label}</p>
      <p className="relative z-10 mt-1 line-clamp-2 min-h-[2.6em] text-center text-[11px] leading-snug text-muted">
        {sample.description}
      </p>
      <div className="relative -mx-2 mt-1 min-h-0 flex-1">
        <Illustration id={sample.id} />
      </div>
      <span className="absolute bottom-4 left-4 grid size-9 place-items-center rounded-full bg-accent text-white shadow-lg">
        {loading ? <LoaderCircle className="size-4 animate-spin" /> : <span className="text-xs font-bold">{index + 1}</span>}
      </span>
      <span className="absolute right-4 bottom-5 text-[11px] font-semibold text-accent opacity-0 transition group-hover:opacity-100">
        Open →
      </span>
    </motion.button>
  )
}
