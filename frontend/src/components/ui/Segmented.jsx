import { motion } from 'motion/react'
import { useId } from 'react'
import { cn } from '../../lib/cn.js'

/** Pill-shaped switch between a few options, with a sliding highlight. */
export function Segmented({ options, value, onChange, className, size = 'md', label }) {
  const id = useId()
  return (
    <div role="radiogroup" aria-label={label} className={cn('glass inline-flex items-center gap-1 rounded-full p-1', className)}>
      {options.map((option) => {
        const active = option.value === value
        const Icon = option.icon
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            aria-label={option.ariaLabel || option.label}
            title={option.ariaLabel || option.label}
            onClick={() => onChange(option.value)}
            className={cn(
              'relative inline-flex items-center justify-center gap-1.5 rounded-full font-semibold transition-colors',
              size === 'sm' ? 'h-7 min-w-7 px-2.5 text-xs' : 'h-9 min-w-9 px-3.5 text-sm',
              active ? 'text-white' : 'text-muted hover:text-text',
            )}
          >
            {active && (
              <motion.span
                layoutId={`seg-${id}`}
                className="absolute inset-0 rounded-full bg-accent shadow-[0_6px_18px_-6px_var(--accent)]"
                transition={{ type: 'spring', stiffness: 420, damping: 34 }}
              />
            )}
            <span className="relative z-10 inline-flex items-center gap-1.5">
              {Icon && <Icon className="size-4" />}
              {option.label && !option.iconOnly && option.label}
            </span>
          </button>
        )
      })}
    </div>
  )
}
