import { AnimatePresence, motion } from 'motion/react'
import { ArrowUpRight, LayoutGrid, Orbit } from 'lucide-react'
import { useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router'
import { TOOLS } from '../../lib/tools.js'
import { Segmented } from '../ui/Segmented.jsx'

/**
 * "Overview | network focus | all tools" switch under the top bar. Focus hides the
 * side panels so the network fills the screen; the grid opens every tool.
 */
export function ViewSwitch() {
  const [launcher, setLauncher] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const [params] = useSearchParams()
  const focus = location.pathname === '/network' && params.get('focus') === '1'
  const value = launcher ? 'tools' : focus ? 'focus' : 'overview'

  const change = (next) => {
    if (next === 'tools') {
      setLauncher(true)
      return
    }
    setLauncher(false)
    navigate(next === 'focus' ? '/network?focus=1' : '/network')
  }

  return (
    <>
      <div className="relative z-20 flex justify-center pt-3">
        <Segmented
          label="View"
          value={value}
          onChange={change}
          options={[
            { value: 'overview', label: 'Overview' },
            { value: 'focus', label: 'Network focus', icon: Orbit, iconOnly: true },
            { value: 'tools', label: 'All tools', icon: LayoutGrid, iconOnly: true },
          ]}
        />
      </div>
      <AnimatePresence>{launcher && <Launcher onClose={() => setLauncher(false)} />}</AnimatePresence>
    </>
  )
}

function Launcher({ onClose }) {
  const navigate = useNavigate()
  return (
    <motion.div
      className="fixed inset-0 z-50 grid place-items-center bg-black/30 p-4 backdrop-blur-sm"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
      onKeyDown={(event) => event.key === 'Escape' && onClose()}
      role="dialog"
      aria-modal="true"
      aria-label="All tools"
    >
      <motion.div
        className="glass glass-strong w-full max-w-3xl rounded-[28px] p-5"
        initial={{ scale: 0.96, y: 12 }}
        animate={{ scale: 1, y: 0 }}
        exit={{ scale: 0.96, y: 12 }}
        transition={{ type: 'spring', stiffness: 380, damping: 32 }}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold tracking-tight">All tools</h2>
          <button type="button" onClick={onClose} className="text-sm font-semibold text-muted hover:text-text" autoFocus>
            Close
          </button>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {TOOLS.map((tool, i) => (
            <motion.button
              key={tool.path}
              type="button"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.03 * i }}
              onClick={() => {
                onClose()
                navigate(tool.path)
              }}
              className="inset group flex items-center gap-3 rounded-2xl p-3 text-left transition hover:border-accent/40 hover:bg-accent-soft"
            >
              <span className="glass grid size-10 shrink-0 place-items-center rounded-full">
                <tool.icon className="size-4" />
              </span>
              <span className="min-w-0 flex-1">
                <span className="flex items-center gap-2 text-sm font-semibold">
                  {tool.title}
                  {!tool.ready && (
                    <span className="rounded-full bg-inset px-2 py-0.5 text-[10px] font-semibold text-faint">Next</span>
                  )}
                </span>
                <span className="block truncate text-xs text-muted">{tool.description}</span>
              </span>
              <ArrowUpRight className="size-4 text-faint transition group-hover:text-accent" />
            </motion.button>
          ))}
        </div>
      </motion.div>
    </motion.div>
  )
}
