import { LoaderCircle } from 'lucide-react'
import { cn } from '../../lib/cn.js'

const variants = {
  primary: 'bg-accent text-white shadow-[0_8px_24px_-8px_var(--accent)] hover:brightness-110',
  glass: 'glass hover:bg-[var(--glass-strong)]',
  ghost: 'text-muted hover:bg-inset hover:text-text',
  danger: 'bg-case/90 text-white hover:bg-case',
}

export function Button({ variant = 'glass', size = 'md', loading = false, icon: Icon, className, children, ...props }) {
  return (
    <button
      type="button"
      disabled={loading || props.disabled}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-full font-semibold transition disabled:cursor-not-allowed disabled:opacity-50',
        size === 'sm' ? 'h-8 px-3 text-xs' : 'h-10 px-4 text-sm',
        variants[variant],
        className,
      )}
      {...props}
    >
      {loading ? <LoaderCircle className="size-4 animate-spin" /> : Icon && <Icon className="size-4" />}
      {children}
    </button>
  )
}

export function IconButton({ icon: Icon, label, className, active, ...props }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={cn(
        'grid size-9 place-items-center rounded-full transition',
        active ? 'bg-accent text-white' : 'text-muted hover:bg-inset hover:text-text',
        className,
      )}
      {...props}
    >
      <Icon className="size-4" />
    </button>
  )
}
