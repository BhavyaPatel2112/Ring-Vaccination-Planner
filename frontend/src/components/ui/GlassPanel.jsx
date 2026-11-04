import { motion } from 'motion/react'
import { cn } from '../../lib/cn.js'

/** Frosted glass card with one quiet entrance. */
export function GlassPanel({ className, children, delay = 0, as = 'section', ...props }) {
  const Component = motion[as]
  return (
    <Component
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay, ease: [0.22, 1, 0.36, 1] }}
      className={cn('glass rounded-[20px] p-4 text-text', className)}
      {...props}
    >
      {children}
    </Component>
  )
}

export function PanelTitle({ icon: Icon, title, subtitle, action }) {
  return (
    <div className="mb-3 flex items-center gap-3">
      {Icon && (
        <span className="inset grid size-8 shrink-0 place-items-center rounded-full">
          <Icon className="size-4 text-muted" />
        </span>
      )}
      <div className="min-w-0 flex-1">
        <h2 className="truncate text-[15px] font-semibold tracking-tight">{title}</h2>
        {subtitle && <p className="text-xs leading-snug text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}
