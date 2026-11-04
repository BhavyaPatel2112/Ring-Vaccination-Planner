import NumberFlow from '@number-flow/react'
import { cn } from '../../lib/cn.js'

/** A labelled number that counts smoothly to its new value. */
export function StatTile({ label, value, suffix, tone, className, size = 'md' }) {
  return (
    <div className={cn('min-w-0', className)}>
      <div className="flex items-center gap-1.5 text-xs font-medium text-muted">
        {tone && <span className="size-1.5 rounded-full" style={{ background: tone }} />}
        <span className="truncate">{label}</span>
      </div>
      <div className={cn('mt-0.5 font-medium tracking-tight', size === 'lg' ? 'text-5xl' : 'text-2xl')}>
        {value == null ? (
          <span className="text-faint">–</span>
        ) : (
          <NumberFlow value={value} locales="en-IN" />
        )}
        {suffix && <span className="ml-1 text-xs font-medium text-muted">{suffix}</span>}
      </div>
    </div>
  )
}
