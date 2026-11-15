import { motion } from 'motion/react'
import { ArrowRight, FileUp, Network, ShieldCheck } from 'lucide-react'
import { useNavigate } from 'react-router'
import { Button } from '../components/ui/Button.jsx'

/** The front page: what the website is for, in plain words, and where to start. */

const STEPS = [
  {
    icon: FileUp,
    title: 'Load who met whom',
    text: 'Start from a sample, your own CSV files, or a synthetic town. Rows that cannot be read are reported, never dropped quietly.',
  },
  {
    icon: Network,
    title: 'Find the rings',
    text: 'Mark the confirmed cases. The app finds everyone they met (ring 1) and everyone those people met (ring 2).',
  },
  {
    icon: ShieldCheck,
    title: 'Get a vaccination plan',
    text: 'It picks a small group so that every contact in the rings has a vaccinated person on at least one side, and gives a reason for every dose.',
  },
]

const MORE = [
  ['Compare methods', 'Three ways of choosing who to vaccinate, side by side, with how many doses each one needs.'],
  ['Outbreak clusters', 'Which groups of people are connected, kept up to date as new contacts are reported.'],
  ['Deadlines and monitoring', 'The date by which each person should be vaccinated, and who to keep an eye on.'],
  ['Exposure self-check', 'Anyone can check whether they visited a place at the same time as a case.'],
  ['Outbreak simulation', 'How many people would fall ill with no vaccine, with random doses, and with each plan.'],
  ['Benchmarks', 'How fast each method runs, measured on networks of up to a million contacts.'],
]

const rise = (delay) => ({
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.6, delay, ease: [0.22, 1, 0.36, 1] },
})

export function HomePage() {
  const navigate = useNavigate()
  return (
    <div className="mx-auto w-full max-w-[1100px] px-6 pt-16 pb-20 sm:pt-24">
      {/* What it is */}
      <section className="max-w-3xl">
        <motion.p {...rise(0)} className="text-sm font-bold tracking-[0.18em] text-accent uppercase">
          Ring Vaccination Planner
        </motion.p>
        <motion.h1
          {...rise(0.08)}
          className="mt-5 text-[clamp(2.4rem,5vw,4.2rem)]/[1.06] font-extrabold tracking-tight text-balance"
        >
          Decide who to vaccinate first when an outbreak starts.
        </motion.h1>
        <motion.p {...rise(0.16)} className="mt-6 text-[clamp(1.05rem,1.6vw,1.25rem)] leading-relaxed text-pretty text-muted">
          When a disease starts to spread and there are not enough vaccines for everyone, health teams vaccinate a ring around
          each confirmed case: the people the case met, and the people those people met. This website takes a record of who met
          whom, finds those rings, and works out a small group of people to vaccinate so the virus has no easy way through.
        </motion.p>
        <motion.div {...rise(0.24)} className="mt-9 flex flex-wrap gap-3">
          <Button variant="primary" onClick={() => navigate('/load')} className="h-12 px-6 text-[15px]">
            Load data to begin
            <ArrowRight className="size-4" />
          </Button>
          <Button variant="glass" onClick={() => navigate('/story')} className="h-12 px-6 text-[15px]">
            Read the story
          </Button>
        </motion.div>
      </section>

      {/* How it works */}
      <section className="mt-24">
        <motion.h2 {...rise(0.3)} className="text-2xl font-bold tracking-tight">
          How it works
        </motion.h2>
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {STEPS.map((step, i) => (
            <motion.div key={step.title} {...rise(0.36 + i * 0.08)} className="glass rounded-[24px] p-6">
              <div className="flex items-center gap-3">
                <span className="grid size-10 place-items-center rounded-full bg-accent-soft text-accent">
                  <step.icon className="size-[18px]" />
                </span>
                <span className="text-sm font-bold text-faint tabular-nums">0{i + 1}</span>
              </div>
              <h3 className="mt-5 text-lg font-semibold tracking-tight">{step.title}</h3>
              <p className="mt-2 text-[15px] leading-relaxed text-muted">{step.text}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* What else is inside */}
      <section className="mt-24">
        <motion.h2 {...rise(0.5)} className="text-2xl font-bold tracking-tight">
          Also inside
        </motion.h2>
        <motion.p {...rise(0.54)} className="mt-2 text-muted">
          Once a network is loaded, these are in All tools at the top of the page.
        </motion.p>
        <div className="mt-6 grid gap-x-10 gap-y-6 sm:grid-cols-2">
          {MORE.map(([title, text], i) => (
            <motion.div key={title} {...rise(0.58 + i * 0.05)} className="border-l-2 border-[var(--glass-border)] pl-4">
              <h3 className="font-semibold">{title}</h3>
              <p className="mt-1 text-[15px] leading-relaxed text-muted">{text}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Behind it */}
      <motion.section {...rise(0.9)} className="mt-24 max-w-3xl">
        <h2 className="text-2xl font-bold tracking-tight">Behind it</h2>
        <p className="mt-4 text-[15px] leading-relaxed text-muted">
          Choosing the fewest people to vaccinate so every contact is covered is a hard problem: no one knows a fast way to get
          the exact answer for large networks. So the planner uses a method that is fast and is proven never to need more than
          twice the doses of the best possible plan, and checks the exact answer on small rings. Every algorithm runs in Java on
          the server; the browser only draws the results.
        </p>
        <p className="mt-4 text-[15px] leading-relaxed text-muted">
          It is a course project for Advanced Algorithms, and it assumes contact records are known. The simulation shows how the
          plan holds up when some of them are missing.
        </p>
      </motion.section>
    </div>
  )
}
