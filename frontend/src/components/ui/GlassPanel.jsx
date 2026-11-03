import { motion } from 'motion/react'
import { cn } from '../../lib/cn.js'

/**
 * Frosted glass card. `grabber` adds the small window handle under the panel,
 * like a floating window you could pick up and move.
 */
export function GlassPanel({ className, children, grabber = false, delay = 0, as = 'section', ...props }) {
  const Component = motion[as]
  return (
    <div className="flex flex-col items-stretch">
      <Component
        initial={{ opacity: 0, y: 14, filter: 'blur(6px)' }}
        animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
        transition={{ duration: 0.55, delay, ease: [0.22, 1, 0.36, 1] }}
        className={cn('glass rounded-[22px] p-4 text-text', className)}
        {...props}
      >
        {children}
      </Component>
      {grabber && <Grabber />}
    </div>
  )
}

export function Grabber() {
  return (
    <div className="flex items-center justify-center gap-2 py-3" aria-hidden="true">
      <span className="size-1.5 rounded-full bg-[var(--text-faint)] opacity-70" />
      <span className="h-1.5 w-24 rounded-full bg-[var(--text-faint)] opacity-50" />
    </div>
  )
}

export function PanelTitle({ icon: Icon, title, subtitle, action }) {
  return (
    <div className="mb-3 flex items-start gap-3">
      {Icon && (
        <span className="inset grid size-9 shrink-0 place-items-center rounded-full">
          <Icon className="size-4 text-muted" />
        </span>
      )}
      <div className="min-w-0 flex-1">
        <h2 className="truncate text-[15px] font-semibold tracking-tight">{title}</h2>
        {subtitle && <p className="text-xs text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}
