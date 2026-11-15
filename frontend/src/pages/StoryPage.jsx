import { motion, useMotionValueEvent, useScroll, useSpring } from 'motion/react'
import { ChevronDown, Maximize, Minimize } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { StoryMorph } from '../components/story/StoryMorph.jsx'
import { SCENES, split } from '../components/story/storyScenes.js'

/*
 * The project told one idea at a time, for the demo. One stage stays pinned
 * while the page scrolls; the scroll position morphs a single drawing from one
 * idea into the next (see StoryMorph), and the words roll up to the next idea.
 * The numbers are the measured ones from docs/benchmarks and the README.
 */
const SLIDES = [
  {
    chapter: 'Ring Vaccination Planner',
    title: 'Stop an outbreak by vaccinating the right people, not everyone.',
    body: 'Ring vaccination protects the contacts of each case, and their contacts, so the virus runs out of people to reach.',
  },
  {
    chapter: 'The model',
    title: 'People are nodes. Contacts are links.',
    body: 'We load who met whom, and when, as a graph. Every question the app answers is a question about this graph.',
  },
  {
    chapter: 'Finding the rings',
    title: 'Breadth-first search draws ring 1, then ring 2.',
    body: 'Starting from the confirmed cases, BFS visits direct contacts first, then contacts of contacts, in O(V + E).',
  },
  {
    chapter: 'The real question',
    title: 'Who is the smallest group that protects every link?',
    body: 'Every contact link needs a vaccinated person on at least one end. That is minimum vertex cover, which is NP-hard.',
  },
  {
    chapter: '2-approximation',
    title: 'Take any unprotected link. Vaccinate both ends.',
    body: 'The picked links share no person, so the best plan needs one dose per link. We use at most two: never more than double.',
  },
  {
    chapter: 'Exact answer',
    title: 'For small rings, branch and bound proves the minimum.',
    body: 'For an unprotected link, it tries vaccinating each end in turn, and cuts any branch that cannot beat the best plan so far.',
  },
  {
    chapter: 'Greedy',
    title: 'Vaccinate the most connected person first.',
    body: 'An indexed max-heap keeps that person on top. After each dose, decrease-key lowers the neighbours in O(log n).',
  },
  {
    chapter: 'Why not just greedy?',
    title: 'Greedy has no guarantee. We built graphs that trap it.',
    body: 'On our trap samples greedy used 23 doses where 12 were enough, and 40 where 18 were enough.',
  },
  {
    chapter: 'Dose budgets',
    title: 'Quickselect finds the top 10% without sorting everyone.',
    body: 'Randomized Quickselect runs in expected linear time. Median-of-medians is the guaranteed linear baseline.',
  },
  {
    chapter: 'Outbreak clusters',
    title: 'Union-find merges clusters as new contacts arrive.',
    body: 'Union by rank and path compression make each new contact almost constant time, instead of a fresh BFS.',
  },
  {
    chapter: 'Measured',
    title: 'Updating clusters at a million contacts',
    body: 'Rebuilding with BFS after every contact would take about 18 hours in total.',
  },
  {
    chapter: 'Exposure check',
    title: 'Hashing turns a venue visit into a few bits.',
    body: 'FNV-1a and MurmurHash3 give two hashes; double hashing makes k bit positions from them.',
  },
  {
    chapter: 'Bloom filter',
    title: 'Check exposure with a fraction of the memory.',
    body: 'A Bloom filter can say "maybe" by mistake, never "no" by mistake. Every "maybe" is then confirmed exactly.',
  },
  {
    chapter: 'Does it work?',
    title: 'We simulate the outbreak to find out.',
    body: 'A seeded SIR model runs the same outbreaks with no vaccine, random doses, and each plan, so the comparison is fair.',
  },
  {
    chapter: 'Result',
    title: 'The ring plan cuts new infections by about 94%.',
    body: 'Against no vaccination, on the sample outbreak. Against the same number of random doses, by about 80%.',
  },
  {
    chapter: 'Real life is messy',
    title: 'Even if tracing misses half the contacts, it still works.',
    body: 'Planning from only half of the contacts still cuts infections by about 85%.',
  },
  {
    chapter: 'Speed',
    title: 'At a million contacts, the approximation is 18× faster.',
    body: 'Greedy found about 19% fewer doses on random graphs, but it is slower and has no worst-case promise.',
  },
  {
    chapter: 'How it is built',
    title: 'Every algorithm runs in Java. The browser only draws.',
    body: 'Spring Boot serves the plans over a REST API; React shows them. All randomness is seeded, so every run repeats.',
  },
  {
    chapter: 'In one place',
    title: 'Eleven screens, eleven algorithms, over 250 tests.',
    body: 'BFS, vertex cover 2-approximation, branch and bound, greedy with an indexed heap, randomized Quickselect, median-of-medians, union-find, FNV-1a, MurmurHash3, Bloom filter and SIR simulation. Thank you.',
  },
]

/** Scroll distance given to each idea, in screen heights. */
const STEP_VH = 110
const LAST = SLIDES.length - 1
if (SCENES.length !== SLIDES.length) throw new Error('Every idea needs exactly one scene')

export function StoryPage() {
  const track = useRef(null)
  const atRef = useRef(0)
  const [at, setAt] = useState(0)
  const [full, setFull] = useState(false)
  const { scrollYProgress } = useScroll({ target: track, offset: ['start start', 'end end'] })
  // A spring between the scroll wheel and the drawing makes the morph glide.
  const smooth = useSpring(scrollYProgress, { stiffness: 140, damping: 28, restDelta: 0.0005 })

  useMotionValueEvent(smooth, 'change', (v) => {
    atRef.current = v * LAST
    setAt(v * LAST)
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
      const i = Math.round(atRef.current)
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

  const { i, t } = split(at)
  const pos = i + t
  const current = Math.round(pos)

  return (
    <div ref={track} className="relative w-full" style={{ height: `calc(${LAST * STEP_VH}vh + 100vh)` }}>
      {/* Reading progress across the top */}
      <motion.div className="fixed inset-x-0 top-0 z-50 h-1 origin-left bg-accent" style={{ scaleX: smooth }} aria-hidden="true" />

      <div className="sticky top-0 flex h-screen items-center overflow-hidden">
        <div className="mx-auto grid w-full max-w-[1400px] items-center gap-10 px-8 lg:grid-cols-[1fr_1.15fr] lg:gap-16 lg:px-16">
          {/* The words: every idea is stacked here and rolls up past the centre */}
          <div className="relative h-[clamp(320px,52vh,480px)]" aria-live="polite">
            {SLIDES.map((slide, k) => {
              const rel = k - pos
              if (Math.abs(rel) > 1) return null
              const opacity = Math.max(0, 1 - Math.abs(rel) * 1.7)
              return (
                <div
                  key={k}
                  className="absolute inset-0 flex flex-col justify-center"
                  style={{ opacity, transform: `translateY(${rel * 90}px)`, pointerEvents: k === current ? 'auto' : 'none' }}
                  aria-hidden={k !== current}
                >
                  <p className="text-sm font-bold tracking-[0.18em] text-accent uppercase">{slide.chapter}</p>
                  <h2
                    className={
                      k === 0
                        ? 'mt-5 text-[clamp(2.6rem,5.4vw,4.8rem)]/[1.05] font-extrabold tracking-tight text-balance'
                        : 'mt-5 text-[clamp(2rem,4.2vw,3.7rem)]/[1.08] font-extrabold tracking-tight text-balance'
                    }
                  >
                    {slide.title}
                  </h2>
                  <p className="mt-6 max-w-xl text-[clamp(1rem,1.45vw,1.2rem)] leading-relaxed text-pretty text-muted">{slide.body}</p>
                  {k === 0 && <ScrollHint />}
                </div>
              )
            })}
          </div>

          {/* The drawing: one set of shapes that moves and reshapes as you scroll */}
          <div className="h-[clamp(260px,56vh,520px)]">
            <StoryMorph at={at} />
          </div>
        </div>
      </div>

      {/* Corner controls */}
      <div className="fixed right-5 bottom-5 z-40 flex items-center gap-3">
        <span className="glass rounded-full px-3 py-1.5 text-xs font-semibold text-muted tabular-nums">
          {String(current + 1).padStart(2, '0')} / {SLIDES.length}
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

function ScrollHint() {
  return (
    <motion.div
      className="mt-12 flex items-center gap-2 text-sm font-semibold text-faint"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 1.2, duration: 0.8 }}
    >
      <motion.span animate={{ y: [0, 6, 0] }} transition={{ duration: 1.6, repeat: Infinity }}>
        <ChevronDown className="size-5" />
      </motion.span>
      Scroll to explore
    </motion.div>
  )
}
