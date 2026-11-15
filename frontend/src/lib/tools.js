import {
  CalendarClock,
  ChartColumn,
  Filter,
  GraduationCap,
  Scale,
  ShieldCheck,
  Sparkles,
  UsersRound,
  Waves,
} from 'lucide-react'

/**
 * Every screen in the All tools menu. Load data, Network and Story are in the
 * top bar instead, so they are not repeated here. `needsNetwork` screens only
 * work once a network is loaded; until then the menu shows them locked.
 */
export const TOOLS = [
  { path: '/plan', needsNetwork: true, title: 'Plan details', description: 'Who to vaccinate, why, and the dose budget', icon: ShieldCheck, ready: true },
  { path: '/compare', needsNetwork: true, title: 'Compare methods', description: 'Approximation, greedy and exact side by side', icon: Scale, ready: true },
  { path: '/clusters', needsNetwork: true, title: 'Outbreak clusters', description: 'Connected groups, updated as contacts arrive', icon: UsersRound, ready: true },
  { path: '/follow-up', needsNetwork: true, title: 'Deadlines and monitoring', description: 'Vaccinate-by dates and who to watch', icon: CalendarClock, ready: true },
  { path: '/exposure', needsNetwork: true, title: 'Exposure self-check', description: 'Public check of venue visits', icon: Sparkles, ready: true },
  { path: '/bloom', title: 'Bloom filter tuning', description: 'Memory against false positive rate', icon: Filter, ready: true },
  { path: '/simulation', needsNetwork: true, title: 'Outbreak simulation', description: 'Infections under each plan', icon: Waves, ready: true },
  { path: '/how-it-works', needsNetwork: true, title: 'How it works', description: 'Step through the algorithms one decision at a time', icon: GraduationCap, ready: true },
  { path: '/benchmarks', title: 'Benchmarks', description: 'Run times and approximation quality', icon: ChartColumn, ready: true },
]
