import { AnimatePresence, motion } from 'motion/react'
import { ChevronDown, LayoutGrid, Moon, Plus, Sun, Syringe } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { NavLink, useNavigate } from 'react-router'
import { listNetworks } from '../../api/endpoints.js'
import { cn } from '../../lib/cn.js'
import { formatNumber, initials } from '../../lib/format.js'
import { useWorkspace } from '../../state/context.js'
import { Launcher } from './Launcher.jsx'

/** One slim bar: brand, current network, navigation and theme. */
export function Header() {
  const [tools, setTools] = useState(false)
  return (
    <header className="relative z-30 px-4 pt-4">
      <div className="glass mx-auto flex h-14 max-w-[1600px] items-center gap-3 rounded-full py-2 pr-2 pl-2">
        <div className="flex items-center gap-2.5 pr-2">
          <span className="grid size-10 place-items-center rounded-full bg-case/12 text-case">
            <Syringe className="size-[18px]" />
          </span>
          <span className="hidden text-[15px] font-semibold tracking-tight sm:block">Ring Vaccination Planner</span>
        </div>
        <span className="hidden h-6 w-px bg-[var(--glass-border)] sm:block" />
        <NetworkSwitcher />
        <nav aria-label="Main" className="ml-auto flex items-center gap-1">
          <TopLink to="/load">Load data</TopLink>
          <TopLink to="/network">Network</TopLink>
          <button
            type="button"
            onClick={() => setTools(true)}
            className="flex h-10 items-center gap-2 rounded-full px-3 text-sm font-semibold text-muted transition hover:bg-inset hover:text-text"
          >
            <LayoutGrid className="size-4" />
            <span className="hidden md:inline">All tools</span>
          </button>
          <ThemeButton />
        </nav>
      </div>
      <AnimatePresence>{tools && <Launcher onClose={() => setTools(false)} />}</AnimatePresence>
    </header>
  )
}

function TopLink({ to, children }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          'flex h-10 items-center rounded-full px-4 text-sm font-semibold transition',
          isActive ? 'bg-accent text-white shadow-[0_6px_18px_-8px_var(--accent)]' : 'text-muted hover:bg-inset hover:text-text',
        )
      }
    >
      {children}
    </NavLink>
  )
}

const THEME_KEY = 'ringvax.theme'

function initialTheme() {
  try {
    const saved = localStorage.getItem(THEME_KEY)
    if (saved === 'light' || saved === 'dark') return saved
  } catch {
    // Storage can be blocked; fall back to the system setting.
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function ThemeButton() {
  const [theme, setTheme] = useState(initialTheme)
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#0b0f17' : '#f5f5f3')
    window.dispatchEvent(new Event('ringvax-theme'))
    try {
      localStorage.setItem(THEME_KEY, theme)
    } catch {
      // ignore blocked storage
    }
  }, [theme])
  const dark = theme === 'dark'
  return (
    <button
      type="button"
      onClick={() => setTheme(dark ? 'light' : 'dark')}
      aria-label={dark ? 'Switch to light theme' : 'Switch to dark theme'}
      title={dark ? 'Light theme' : 'Dark theme'}
      className="grid size-10 place-items-center rounded-full text-muted transition hover:bg-inset hover:text-text"
    >
      {dark ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </button>
  )
}

function NetworkSwitcher() {
  const { network, switchNetwork } = useWorkspace()
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const navigate = useNavigate()
  const ref = useRef(null)

  useEffect(() => {
    if (!open) return
    listNetworks().then(setItems).catch(() => setItems([]))
    const close = (event) => {
      if (!ref.current?.contains(event.target)) setOpen(false)
    }
    const escape = (event) => event.key === 'Escape' && setOpen(false)
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', escape)
    }
  }, [open])

  return (
    <div ref={ref} className="relative min-w-0">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-haspopup="listbox"
        className="flex h-10 max-w-72 items-center gap-2.5 rounded-full py-1 pr-3 pl-1 text-left transition hover:bg-inset"
      >
        <span className="inset grid size-8 shrink-0 place-items-center rounded-full text-xs font-bold">
          {network ? initials(network.name) : '–'}
        </span>
        <span className="min-w-0 leading-tight">
          <span className="block truncate text-sm font-semibold">{network ? network.name : 'No network loaded'}</span>
          <span className="block truncate text-xs text-muted">
            {network ? `${formatNumber(network.people)} people · ${formatNumber(network.contacts)} contacts` : 'Load a sample to begin'}
          </span>
        </span>
        <ChevronDown className={cn('size-4 shrink-0 text-muted transition', open && 'rotate-180')} />
      </button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.15 }}
            className="glass glass-strong absolute top-12 left-0 z-50 w-80 rounded-[20px] p-2"
            role="listbox"
          >
            <p className="px-3 pt-2 pb-1 text-xs font-semibold text-muted">Loaded networks</p>
            {items.length === 0 && <p className="px-3 py-2 text-sm text-muted">Nothing loaded yet.</p>}
            {items.map((item) => (
              <button
                key={item.networkId}
                type="button"
                role="option"
                aria-selected={item.networkId === network?.networkId}
                onClick={() => {
                  setOpen(false)
                  switchNetwork(item.networkId).then(() => navigate('/network'))
                }}
                className="flex w-full items-center gap-3 rounded-2xl px-3 py-2 text-left hover:bg-inset aria-selected:bg-accent-soft"
              >
                <span className="inset grid size-8 shrink-0 place-items-center rounded-full text-xs font-bold">
                  {initials(item.name)}
                </span>
                <span className="min-w-0">
                  <span className="block truncate text-sm font-semibold">{item.name}</span>
                  <span className="block text-xs text-muted">
                    {formatNumber(item.people)} people · {item.caseIds.length} cases
                  </span>
                </span>
              </button>
            ))}
            <button
              type="button"
              onClick={() => {
                setOpen(false)
                navigate('/load')
              }}
              className="mt-1 flex w-full items-center gap-2 rounded-2xl px-3 py-2.5 text-sm font-semibold text-accent hover:bg-inset"
            >
              <Plus className="size-4" /> Load another network
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
