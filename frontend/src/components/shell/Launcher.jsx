import { motion } from 'motion/react'
import { ArrowUpRight, Lock } from 'lucide-react'
import { useNavigate } from 'react-router'
import { cn } from '../../lib/cn.js'
import { TOOLS } from '../../lib/tools.js'
import { useWorkspace } from '../../state/context.js'

/**
 * The other screens, as a grid of tiles. Screens that need a network are
 * locked until one is loaded, instead of silently sending you to Load data.
 */
export function Launcher({ onClose }) {
  const navigate = useNavigate()
  const { network } = useWorkspace()
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
        {!network && (
          <p className="mb-3 text-sm text-muted">
            Load a network first to open the locked tools.{' '}
            <button
              type="button"
              className="font-semibold text-accent hover:underline"
              onClick={() => {
                onClose()
                navigate('/load')
              }}
            >
              Load data
            </button>
          </p>
        )}
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {TOOLS.map((tool, i) => {
            const locked = tool.needsNetwork && !network
            return (
              <motion.button
                key={tool.path}
                type="button"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.03 * i }}
                disabled={locked}
                title={locked ? 'Load a network first' : undefined}
                onClick={() => {
                  onClose()
                  navigate(tool.path)
                }}
                className={cn(
                  'inset group flex items-center gap-3 rounded-2xl p-3 text-left transition',
                  locked ? 'cursor-not-allowed opacity-45' : 'hover:border-accent/40 hover:bg-accent-soft',
                )}
              >
                <span className="glass grid size-10 shrink-0 place-items-center rounded-full">
                  <tool.icon className="size-4" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2 text-sm font-semibold">
                    {tool.title}
                    {!tool.ready && <span className="rounded-full bg-inset px-2 py-0.5 text-xs font-semibold text-faint">Next</span>}
                  </span>
                  <span className="block truncate text-xs text-muted">{locked ? 'Load a network first' : tool.description}</span>
                </span>
                {locked ? (
                  <Lock className="size-4 text-faint" />
                ) : (
                  <ArrowUpRight className="size-4 text-faint transition group-hover:text-accent" />
                )}
              </motion.button>
            )
          })}
        </div>
      </motion.div>
    </motion.div>
  )
}
