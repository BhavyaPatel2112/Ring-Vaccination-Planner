import { AnimatePresence, motion } from 'motion/react'
import { ChevronDown, Plus, Syringe } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { listNetworks } from '../../api/endpoints.js'
import { formatNumber, initials } from '../../lib/format.js'
import { useWorkspace } from '../../state/context.js'

/** Floating pills along the top: app badge, network switcher and clock. */
export function TopBar() {
  return (
    <header className="relative z-30 flex flex-wrap items-center justify-center gap-2 px-4 pt-4">
      <div className="glass flex h-14 items-center gap-3 rounded-full py-2 pr-5 pl-2">
        <span className="grid size-10 place-items-center rounded-full bg-case/15 text-case">
          <Syringe className="size-5" />
        </span>
        <div className="leading-tight">
          <p className="text-[15px] font-semibold tracking-tight">Ring Vaccination Planner</p>
          <p className="text-[11px] text-muted">Outbreak response</p>
        </div>
      </div>
      <NetworkSwitcher />
      <Clock />
    </header>
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
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-haspopup="listbox"
        className="glass flex h-14 items-center gap-3 rounded-full py-2 pr-3 pl-2 text-left transition hover:bg-[var(--glass-strong)]"
      >
        <span className="inset grid size-10 place-items-center rounded-full text-sm font-bold">
          {network ? initials(network.name) : '–'}
        </span>
        <span className="max-w-56 leading-tight">
          <span className="block truncate text-[13px] font-semibold">{network ? network.name : 'No network loaded'}</span>
          <span className="block truncate text-[11px] text-muted">
            {network
              ? `${formatNumber(network.people)} people · ${formatNumber(network.contacts)} contacts`
              : 'Load a sample or upload your files'}
          </span>
        </span>
        <span className="mx-1 h-8 w-px bg-[var(--glass-border)]" />
        <ChevronDown className={`size-4 text-muted transition ${open ? 'rotate-180' : ''}`} />
      </button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.18 }}
            className="glass glass-strong absolute top-16 left-0 z-50 w-80 rounded-[20px] p-2"
            role="listbox"
          >
            <p className="px-3 pt-2 pb-1 text-[11px] font-semibold tracking-wide text-faint uppercase">Loaded networks</p>
            {items.length === 0 && <p className="px-3 py-2 text-xs text-muted">Nothing loaded yet.</p>}
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
                <span className="inset grid size-8 shrink-0 place-items-center rounded-full text-[11px] font-bold">
                  {initials(item.name)}
                </span>
                <span className="min-w-0">
                  <span className="block truncate text-[13px] font-semibold">{item.name}</span>
                  <span className="block text-[11px] text-muted">
                    {formatNumber(item.people)} people · {item.caseIds.length} cases · {item.source}
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
              className="mt-1 flex w-full items-center gap-2 rounded-2xl px-3 py-2.5 text-[13px] font-semibold text-accent hover:bg-inset"
            >
              <Plus className="size-4" /> Load another network
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

function Clock() {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 15_000)
    return () => clearInterval(timer)
  }, [])
  const time = now.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true })
  const [clock, meridiem] = time.split(' ')
  return (
    <div className="glass hidden h-14 items-center gap-2 rounded-full px-5 sm:flex" aria-label={`Local time ${time}`}>
      <span className="text-2xl font-medium tracking-tight tabular-nums">{clock}</span>
      <span className="text-xs font-semibold text-muted uppercase">{meridiem}</span>
      <span className="ml-2 text-[11px] text-muted">
        {now.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })}
      </span>
    </div>
  )
}
