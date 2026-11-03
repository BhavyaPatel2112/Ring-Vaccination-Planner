import {
  CalendarClock,
  ChartColumn,
  FileUp,
  Filter,
  Network,
  Scale,
  ShieldCheck,
  Sparkles,
  UsersRound,
  Waves,
} from 'lucide-react'

/**
 * Every screen in the app. `ready: false` screens show a placeholder until they
 * are built; they keep the same layout and components.
 */
export const TOOLS = [
  { path: '/load', title: 'Load data', description: 'Samples, your CSV files or a synthetic town', icon: FileUp, ready: true },
  { path: '/network', title: 'Network and rings', description: 'Mark cases and see ring 1 and ring 2', icon: Network, ready: true },
  { path: '/plan', title: 'Plan details', description: 'Who to vaccinate, why, and the dose budget', icon: ShieldCheck, ready: false },
  { path: '/compare', title: 'Compare methods', description: 'Approximation, greedy and exact side by side', icon: Scale, ready: false },
  { path: '/clusters', title: 'Outbreak clusters', description: 'Connected groups, updated as contacts arrive', icon: UsersRound, ready: false },
  { path: '/follow-up', title: 'Deadlines and monitoring', description: 'Vaccinate-by dates and who to watch', icon: CalendarClock, ready: false },
  { path: '/exposure', title: 'Exposure self-check', description: 'Public check of venue visits', icon: Sparkles, ready: false },
  { path: '/bloom', title: 'Bloom filter tuning', description: 'Memory against false positive rate', icon: Filter, ready: false },
  { path: '/simulation', title: 'Outbreak simulation', description: 'Infections under each plan', icon: Waves, ready: false },
  { path: '/benchmarks', title: 'Benchmarks', description: 'Run times and approximation quality', icon: ChartColumn, ready: false },
]
