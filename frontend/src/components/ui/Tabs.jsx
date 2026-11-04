import { motion } from 'motion/react'
import { useId } from 'react'
import { cn } from '../../lib/cn.js'

/** Compact tabs with a count on each and a sliding underline. */
export function Tabs({ tabs, value, onChange, label }) {
  const id = useId()
  return (
    <div role="tablist" aria-label={label} className="flex gap-1 border-b border-[var(--glass-border)]">
      {tabs.map((tab) => {
        const active = tab.value === value
        return (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(tab.value)}
            className={cn(
              'relative flex h-9 items-center gap-1.5 px-2 text-xs font-semibold whitespace-nowrap transition-colors',
              active ? 'text-text' : 'text-muted hover:text-text',
            )}
          >
            {tab.dot && <span className="size-1.5 rounded-full" style={{ background: tab.dot }} />}
            {tab.label}
            {tab.count != null && <span className={active ? 'text-accent' : 'text-faint'}>{tab.count}</span>}
            {active && (
              <motion.span
                layoutId={`tab-${id}`}
                className="absolute inset-x-1 -bottom-px h-0.5 rounded-full bg-accent"
                transition={{ type: 'spring', stiffness: 500, damping: 40 }}
              />
            )}
          </button>
        )
      })}
    </div>
  )
}
