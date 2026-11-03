import { CircleAlert, Info, TriangleAlert } from 'lucide-react'
import { cn } from '../../lib/cn.js'

const tones = {
  info: { icon: Info, color: 'var(--accent)' },
  warning: { icon: TriangleAlert, color: 'var(--warning)' },
  error: { icon: CircleAlert, color: 'var(--danger)' },
}

export function Notice({ tone = 'info', title, children, className }) {
  const { icon: Icon, color } = tones[tone]
  return (
    <div className={cn('inset flex gap-3 rounded-2xl p-3 text-xs', className)} role={tone === 'error' ? 'alert' : 'status'}>
      <Icon className="mt-0.5 size-4 shrink-0" style={{ color }} />
      <div className="min-w-0">
        {title && <p className="font-semibold text-text">{title}</p>}
        <div className="text-muted">{children}</div>
      </div>
    </div>
  )
}

export function Empty({ icon: Icon, title, children }) {
  return (
    <div className="flex flex-col items-center gap-2 px-4 py-6 text-center">
      {Icon && (
        <span className="inset grid size-10 place-items-center rounded-full">
          <Icon className="size-4 text-muted" />
        </span>
      )}
      <p className="text-sm font-semibold">{title}</p>
      {children && <p className="max-w-64 text-xs text-muted">{children}</p>}
    </div>
  )
}
