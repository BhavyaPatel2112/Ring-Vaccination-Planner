import { AnimatePresence, motion } from 'motion/react'
import { ChevronLeft, ChevronRight, Maximize, Minimize } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
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
 * The project told one idea at a time, for the demo. The numbers are the
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

const variants = {
  enter: (dir) => ({ opacity: 0, x: dir * 60, filter: 'blur(8px)' }),
  centre: { opacity: 1, x: 0, filter: 'blur(0px)' },
  exit: (dir) => ({ opacity: 0, x: dir * -60, filter: 'blur(8px)' }),
}

const ease = [0.22, 1, 0.36, 1]

export function StoryPage() {
  const [[index, dir], setPos] = useState([0, 1])
  const [full, setFull] = useState(false)
  const root = useRef(null)
  const lastWheel = useRef(0)
  const touchX = useRef(null)

  const go = useCallback((to) => {
    setPos(([current]) => {
      const next = Math.max(0, Math.min(SLIDES.length - 1, to(current)))
      return next === current ? [current, 1] : [next, next > current ? 1 : -1]
    })
  }, [])
  const next = useCallback(() => go((i) => i + 1), [go])
  const prev = useCallback(() => go((i) => i - 1), [go])

  useEffect(() => {
    function onKey(e) {
      if (e.target.closest?.('input, textarea, select')) return
      if (['ArrowRight', 'ArrowDown', 'PageDown', ' '].includes(e.key)) {
        e.preventDefault()
        next()
      } else if (['ArrowLeft', 'ArrowUp', 'PageUp'].includes(e.key)) {
        e.preventDefault()
        prev()
      } else if (e.key === 'Home') go(() => 0)
      else if (e.key === 'End') go(() => SLIDES.length - 1)
      else if (e.key === 'f' || e.key === 'F') toggleFull()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [next, prev, go])

  useEffect(() => {
    const onChange = () => setFull(Boolean(document.fullscreenElement))
    document.addEventListener('fullscreenchange', onChange)
    return () => document.removeEventListener('fullscreenchange', onChange)
  }, [])

  function toggleFull() {
    if (document.fullscreenElement) document.exitFullscreen?.()
    else root.current?.requestFullscreen?.()
  }

  function onWheel(e) {
    const now = Date.now()
    if (Math.abs(e.deltaY) < 30 || now - lastWheel.current < 900) return
    lastWheel.current = now
    if (e.deltaY > 0) next()
    else prev()
  }

  const slide = SLIDES[index]

  return (
    <div
      ref={root}
      onWheel={onWheel}
      onTouchStart={(e) => (touchX.current = e.touches[0].clientX)}
      onTouchEnd={(e) => {
        if (touchX.current === null) return
        const dx = e.changedTouches[0].clientX - touchX.current
        touchX.current = null
        if (dx < -50) next()
        else if (dx > 50) prev()
      }}
      className={cn(
        'relative mx-auto flex w-full max-w-[1400px] flex-1 flex-col px-6 select-none',
        full ? 'h-screen max-w-none bg-[var(--scene-bottom)] px-12' : 'h-[calc(100vh-96px)] min-h-[560px]',
      )}
    >
      {/* Progress */}
      <div className="flex items-center gap-1.5 pt-6" aria-hidden="true">
        {SLIDES.map((s, i) => (
          <button
            key={i}
            type="button"
            tabIndex={-1}
            onClick={() => go(() => i)}
            title={s.chapter}
            className="group h-6 flex-1 cursor-pointer"
          >
            <span className="block h-1 overflow-hidden rounded-full bg-[var(--glass-border)] transition group-hover:h-1.5">
              <motion.span
                className="block h-full rounded-full bg-accent"
                initial={false}
                animate={{ width: i <= index ? '100%' : '0%' }}
                transition={{ duration: 0.45, ease }}
              />
            </span>
          </button>
        ))}
      </div>

      {/* Slide */}
      <div className="relative flex flex-1 items-center overflow-hidden">
        <AnimatePresence mode="wait" custom={dir} initial={false}>
          <motion.section
            key={index}
            custom={dir}
            variants={variants}
            initial="enter"
            animate="centre"
            exit="exit"
            transition={{ duration: 0.5, ease }}
            className="grid w-full items-center gap-10 py-8 lg:grid-cols-[1fr_1.1fr] lg:gap-16"
            aria-roledescription="slide"
            aria-label={`${index + 1} of ${SLIDES.length}: ${slide.chapter}`}
          >
            <div>
              <Reveal delay={0.05}>
                <p className="text-sm font-bold tracking-[0.18em] text-accent uppercase">{slide.chapter}</p>
              </Reveal>
              <Reveal delay={0.15}>
                <h1 className="mt-5 text-[clamp(2rem,4.4vw,3.8rem)] leading-[1.08] font-extrabold tracking-tight text-balance">
                  {slide.title}
                </h1>
              </Reveal>
              <Reveal delay={0.3}>
                <p className="mt-6 max-w-xl text-[clamp(1rem,1.5vw,1.25rem)] leading-relaxed text-pretty text-muted">{slide.body}</p>
              </Reveal>
            </div>
            <Reveal delay={0.25} className="h-[clamp(240px,46vh,440px)]">
              {slide.visual}
            </Reveal>
          </motion.section>
        </AnimatePresence>
      </div>

      {/* Controls */}
      <div className="flex items-center justify-between pb-6">
        <span className="text-sm font-semibold text-faint tabular-nums">
          {String(index + 1).padStart(2, '0')} / {SLIDES.length}
        </span>
        <span className="hidden text-xs text-faint md:block">Arrow keys or scroll to move · F for full screen</span>
        <div className="flex items-center gap-2">
          <RoundButton label={full ? 'Exit full screen' : 'Full screen'} onClick={toggleFull}>
            {full ? <Minimize className="size-4" /> : <Maximize className="size-4" />}
          </RoundButton>
          <RoundButton label="Previous" onClick={prev} disabled={index === 0}>
            <ChevronLeft className="size-5" />
          </RoundButton>
          <RoundButton label="Next" onClick={next} disabled={index === SLIDES.length - 1} primary>
            <ChevronRight className="size-5" />
          </RoundButton>
        </div>
      </div>
    </div>
  )
}

function Reveal({ children, delay = 0, className }) {
  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay, ease }}
    >
      {children}
    </motion.div>
  )
}

function RoundButton({ label, onClick, disabled, primary, children }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={onClick}
      disabled={disabled}
      className={cn(
        'grid size-11 place-items-center rounded-full transition disabled:opacity-30',
        primary ? 'bg-accent text-white shadow-[0_6px_18px_-8px_var(--accent)] hover:brightness-110' : 'glass text-muted hover:text-text',
      )}
    >
      {children}
    </button>
  )
}
