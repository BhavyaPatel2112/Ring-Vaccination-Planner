import { motion, useMotionValueEvent, useScroll, useSpring, useTransform } from 'motion/react'
import { ChevronDown, Maximize, Minimize } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import {
  ArchitectureFlow,
  BfsLayers,
  BigNumber,
  BranchTree,
  CoverEdges,
  DsuMerge,
  HashBits,
  HeapBars,
  MatchingSteps,
  NetworkForms,
  RingsPulse,
  SelectNarrow,
  SirCurves,
  Toolkit,
  TrapBars,
  VersusNumbers,
} from '../components/story/StoryVisuals.jsx'
import { cn } from '../lib/cn.js'

/*
 * The project told one idea at a time, for the demo. Scrolling drives it: one stage
 * stays pinned to the screen and the scroll position cross-fades the ideas on it,
 * each rising in, holding, and drifting up as the next one arrives. The numbers are the
 * measured ones from docs/benchmarks and the README; nothing here is computed
 * in the browser.
 */
const SLIDES = [
  {
    chapter: 'Ring Vaccination Planner',
    title: 'Stop an outbreak by vaccinating the right people, not everyone.',
    body: 'Ring vaccination protects the contacts of each case, and their contacts, so the virus runs out of people to reach.',
    visual: <RingsPulse />,
  },
  {
    chapter: 'The model',
    title: 'People are nodes. Contacts are links.',
    body: 'We load who met whom, and when, as a graph. Every question the app answers is a question about this graph.',
    visual: <NetworkForms />,
  },
  {
    chapter: 'Finding the rings',
    title: 'Breadth-first search draws ring 1, then ring 2.',
    body: 'Starting from the confirmed cases, BFS visits direct contacts first, then contacts of contacts, in O(V + E).',
    visual: <BfsLayers />,
  },
  {
    chapter: 'The real question',
    title: 'Who is the smallest group that protects every link?',
    body: 'Every contact link needs a vaccinated person on at least one end. That is minimum vertex cover, which is NP-hard.',
    visual: <CoverEdges />,
  },
  {
    chapter: '2-approximation',
    title: 'Take any unprotected link. Vaccinate both ends.',
    body: 'The picked links share no person, so the best plan needs one dose per link. We use at most two: never more than double.',
    visual: <MatchingSteps />,
  },
  {
    chapter: 'Exact answer',
    title: 'For small rings, branch and bound proves the minimum.',
    body: 'For an unprotected link, it tries vaccinating each end in turn, and cuts any branch that cannot beat the best plan so far.',
    visual: <BranchTree />,
  },
  {
    chapter: 'Greedy',
    title: 'Vaccinate the most connected person first.',
    body: 'An indexed max-heap keeps that person on top. After each dose, decrease-key lowers the neighbours in O(log n).',
    visual: <HeapBars />,
  },
  {
    chapter: 'Why not just greedy?',
    title: 'Greedy has no guarantee. We built graphs that trap it.',
    body: 'On our trap samples greedy used 23 doses where 12 were enough, and 40 where 18 were enough.',
    visual: <TrapBars />,
  },
  {
    chapter: 'Dose budgets',
    title: 'Quickselect finds the top 10% without sorting everyone.',
    body: 'Randomized Quickselect runs in expected linear time. Median-of-medians is the guaranteed linear baseline.',
    visual: <SelectNarrow />,
  },
  {
    chapter: 'Outbreak clusters',
    title: 'Union-find merges clusters as new contacts arrive.',
    body: 'Union by rank and path compression make each new contact almost constant time, instead of a fresh BFS.',
    visual: <DsuMerge />,
  },
  {
    chapter: 'Measured',
    title: 'Updating clusters at a million contacts',
    body: 'Rebuilding with BFS after every contact would take about 18 hours in total.',
    visual: (
      <VersusNumbers
        left={{ label: 'Union-find, per update', value: 0.023, decimals: 3, unit: 'µs', good: true }}
        right={{ label: 'BFS rebuild, per update', value: 64.58, decimals: 2, unit: 'ms' }}
      />
    ),
  },
  {
    chapter: 'Exposure check',
    title: 'Hashing turns a venue visit into a few bits.',
    body: 'FNV-1a and MurmurHash3 give two hashes; double hashing makes k bit positions from them.',
    visual: <HashBits />,
  },
  {
    chapter: 'Bloom filter',
    title: 'Check exposure with a fraction of the memory.',
    body: 'A Bloom filter can say "maybe" by mistake, never "no" by mistake. Every "maybe" is then confirmed exactly.',
    visual: <BigNumber value={88} suffix="×" caption="less memory than a HashSet, with a false positive rate within 3% of the formula" />,
  },
  {
    chapter: 'Does it work?',
    title: 'We simulate the outbreak to find out.',
    body: 'A seeded SIR model runs the same outbreaks with no vaccine, random doses, and each plan, so the comparison is fair.',
    visual: <SirCurves />,
  },
  {
    chapter: 'Result',
    title: 'The ring plan cuts new infections by about 94%.',
    body: 'Against no vaccination, on the sample outbreak. Against the same number of random doses, by about 80%.',
    visual: <BigNumber value={94} suffix="%" caption="fewer infections" />,
  },
  {
    chapter: 'Real life is messy',
    title: 'Even if tracing misses half the contacts, it still works.',
    body: 'Planning from only half of the contacts still cuts infections by about 85%.',
    visual: <BigNumber value={85} suffix="%" caption="fewer infections with 50% of contacts missing" />,
  },
  {
    chapter: 'Speed',
    title: 'At a million contacts, the approximation is 18× faster.',
    body: 'Greedy found about 19% fewer doses on random graphs, but it is slower and has no worst-case promise.',
    visual: (
      <VersusNumbers
        left={{ label: '2-approximation', value: 17.97, decimals: 2, unit: 'ms', good: true }}
        right={{ label: 'Greedy with heap', value: 326.83, decimals: 2, unit: 'ms' }}
      />
    ),
  },
  {
    chapter: 'How it is built',
    title: 'Every algorithm runs in Java. The browser only draws.',
    body: 'Spring Boot serves the plans over a REST API; React shows them. All randomness is seeded, so every run repeats.',
    visual: <ArchitectureFlow />,
  },
  {
    chapter: 'In one place',
    title: 'Eleven screens, eleven algorithms, over 250 tests.',
    body: 'Thank you.',
    visual: (
      <Toolkit
        items={[
          'Breadth-first search',
          'Vertex cover 2-approximation',
          'Branch and bound',
          'Greedy + indexed heap',
          'Randomized Quickselect',
          'Median-of-medians',
          'Union-find',
          'FNV-1a',
          'MurmurHash3',
          'Bloom filter',
          'SIR simulation',
        ]}
      />
    ),
  },
]

const ease = [0.22, 1, 0.36, 1]

/** Scroll distance given to each idea, in screen heights. */
const STEP_VH = 120
const LAST = SLIDES.length - 1

/**
 * How far slide i is from the centre of the screen, in slides: 0 means it is
 * fully shown, -1 means it is one idea away below, +1 one idea away above.
 */
const offsetOf = (progress, i) => progress * LAST - i

export function StoryPage() {
  const track = useRef(null)
  const activeRef = useRef(0)
  const [active, setActive] = useState(0)
  const [full, setFull] = useState(false)
  const { scrollYProgress: p } = useScroll({ target: track, offset: ['start start', 'end end'] })
  const bar = useSpring(p, { stiffness: 200, damping: 30 })

  useMotionValueEvent(p, 'change', (v) => {
    const i = Math.round(v * LAST)
    activeRef.current = i
    setActive(i)
  })

  function jump(i) {
    const el = track.current
    if (!el) return
    const target = Math.max(0, Math.min(LAST, i))
    const top = el.getBoundingClientRect().top + window.scrollY
    const travel = el.offsetHeight - window.innerHeight
    window.scrollTo({ top: top + (travel * target) / LAST, behavior: 'smooth' })
  }

  function toggleFull() {
    if (document.fullscreenElement) document.exitFullscreen?.()
    else document.documentElement.requestFullscreen?.()
  }

  // Keys still work for the live demo: each press glides to the next idea.
  useEffect(() => {
    function onKey(e) {
      if (e.target.closest?.('input, textarea, select')) return
      const i = activeRef.current
      if (['ArrowRight', 'ArrowDown', 'PageDown', ' '].includes(e.key)) {
        e.preventDefault()
        jump(i + 1)
      } else if (['ArrowLeft', 'ArrowUp', 'PageUp'].includes(e.key)) {
        e.preventDefault()
        jump(i - 1)
      } else if (e.key === 'Home') jump(0)
      else if (e.key === 'End') jump(LAST)
      else if (e.key === 'f' || e.key === 'F') toggleFull()
    }
    const onFull = () => setFull(Boolean(document.fullscreenElement))
    window.addEventListener('keydown', onKey)
    document.addEventListener('fullscreenchange', onFull)
    return () => {
      window.removeEventListener('keydown', onKey)
      document.removeEventListener('fullscreenchange', onFull)
    }
  }, [])

  return (
    <div ref={track} className="relative w-full" style={{ height: `calc(${LAST * STEP_VH}vh + 100vh)` }}>
      {/* Reading progress across the top */}
      <motion.div className="fixed inset-x-0 top-0 z-50 h-1 origin-left bg-accent" style={{ scaleX: bar }} aria-hidden="true" />

      {/* The pinned stage: every idea sits here, and scrolling cross-fades them */}
      <div className="sticky top-0 h-screen overflow-hidden">
        {SLIDES.map((slide, i) => (
          <StorySlide key={i} slide={slide} index={i} progress={p} />
        ))}
      </div>

      {/* Chapter dots on the right */}
      <nav aria-label="Chapters" className="fixed top-1/2 right-5 z-40 hidden -translate-y-1/2 flex-col gap-2.5 lg:flex">
        {SLIDES.map((s, i) => (
          <button key={i} type="button" onClick={() => jump(i)} title={s.chapter} aria-label={s.chapter} className="group grid size-4 place-items-center">
            <span
              className={cn(
                'block rounded-full transition-all duration-300',
                i === active ? 'size-2.5 bg-accent' : 'size-1.5 bg-[var(--text-faint)] opacity-50 group-hover:opacity-100',
              )}
            />
          </button>
        ))}
      </nav>

      {/* Corner controls */}
      <div className="fixed right-5 bottom-5 z-40 flex items-center gap-3">
        <span className="glass rounded-full px-3 py-1.5 text-xs font-semibold text-muted tabular-nums">
          {String(active + 1).padStart(2, '0')} / {SLIDES.length}
        </span>
        <button
          type="button"
          onClick={toggleFull}
          aria-label={full ? 'Exit full screen' : 'Full screen'}
          title={full ? 'Exit full screen (F)' : 'Full screen (F)'}
          className="glass grid size-10 place-items-center rounded-full text-muted transition hover:text-text"
        >
          {full ? <Minimize className="size-4" /> : <Maximize className="size-4" />}
        </button>
      </div>
    </div>
  )
}

/** Opacity for a slide `d` slides away from centre: fades over 0.12–0.52, so neighbours just touch. */
function fade(d, first, last) {
  if ((first && d < 0) || (last && d > 0)) return 1
  const a = Math.abs(d)
  if (a <= 0.12) return 1
  if (a >= 0.52) return 0
  return 1 - (a - 0.12) / 0.4
}

/**
 * One idea on the stage. Below centre it waits low and blurred; at centre it
 * is sharp and still; above centre it drifts up and blurs out.
 */
function StorySlide({ slide, index, progress }) {
  const first = index === 0
  const last = index === LAST
  const d = useTransform(progress, (v) => offsetOf(v, index))
  const opacity = useTransform(d, (x) => fade(x, first, last))
  const clampD = (x) => Math.max(-1, Math.min(1, (first && x < 0) || (last && x > 0) ? 0 : x))
  const textY = useTransform(d, (x) => -clampD(x) * 120)
  const visualY = useTransform(d, (x) => -clampD(x) * (x < 0 ? 200 : 60))
  const visualScale = useTransform(d, (x) => 1 + clampD(x) * (x < 0 ? 0.14 : 0.06))
  const filter = useTransform(d, (x) => `blur(${Math.min(14, Math.max(0, Math.abs(clampD(x)) - 0.12) * 24)}px)`)

  // The drawing starts again each time its idea comes back to the centre.
  const [shown, setShown] = useState(first)
  const [run, setRun] = useState(0)
  useMotionValueEvent(d, 'change', (x) => {
    const visible = Math.abs(clampD(x)) < 0.45
    if (visible && !shown) setRun((r) => r + 1)
    if (visible !== shown) setShown(visible)
  })

  return (
    <motion.section
      style={{ opacity }}
      className={cn('absolute inset-0 flex items-center', !shown && 'pointer-events-none')}
      aria-hidden={!shown}
      aria-label={`${index + 1} of ${SLIDES.length}: ${slide.chapter}`}
    >
      <div className="mx-auto grid w-full max-w-[1400px] items-center gap-10 px-8 lg:grid-cols-[1fr_1.1fr] lg:gap-20 lg:px-16">
        <motion.div style={{ y: textY, filter }}>
          <p className="text-sm font-bold tracking-[0.18em] text-accent uppercase">{slide.chapter}</p>
          <h2
            className={cn(
              'mt-5 font-extrabold tracking-tight text-balance',
              first ? 'text-[clamp(2.6rem,5.6vw,5rem)]/[1.05]' : 'text-[clamp(2rem,4.4vw,3.9rem)]/[1.08]',
            )}
          >
            {slide.title}
          </h2>
          <p className="mt-6 max-w-xl text-[clamp(1rem,1.5vw,1.25rem)] leading-relaxed text-pretty text-muted">{slide.body}</p>
          {first && <ScrollHint />}
        </motion.div>
        <motion.div style={{ y: visualY, scale: visualScale }} className="h-[clamp(240px,48vh,460px)]">
          {shown && (
            <div key={run} className="h-full">
              {slide.visual}
            </div>
          )}
        </motion.div>
      </div>
    </motion.section>
  )
}

function ScrollHint() {
  return (
    <motion.div
      className="mt-12 flex items-center gap-2 text-sm font-semibold text-faint"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 1.2, duration: 0.8, ease }}
    >
      <motion.span animate={{ y: [0, 6, 0] }} transition={{ duration: 1.6, repeat: Infinity }}>
        <ChevronDown className="size-5" />
      </motion.span>
      Scroll to explore
    </motion.div>
  )
}
