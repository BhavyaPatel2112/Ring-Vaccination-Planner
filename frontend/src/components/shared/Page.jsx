import { ArrowRight, CircleDot } from 'lucide-react'
import { useNavigate } from 'react-router'
import { Button } from '../ui/Button.jsx'
import { GlassPanel } from '../ui/GlassPanel.jsx'

/** Page width, padding and title shared by the tool screens. */
export function Page({ title, subtitle, icon: Icon, actions, children, width = 'max-w-[1400px]' }) {
  return (
    <div className={`mx-auto flex w-full ${width} flex-1 flex-col gap-4 px-4 pt-5`}>
      <div className="flex flex-wrap items-center gap-3">
        {Icon && (
          <span className="glass grid size-11 shrink-0 place-items-center rounded-full">
            <Icon className="size-5 text-accent" />
          </span>
        )}
        <div className="min-w-0 flex-1">
          <h1 className="text-2xl font-medium tracking-tight">{title}</h1>
          {subtitle && <p className="text-sm text-muted">{subtitle}</p>}
        </div>
        {actions}
      </div>
      {children}
    </div>
  )
}

/** Shown when a screen needs confirmed cases and none are marked yet. */
export function NeedCases({ what = 'this screen' }) {
  const navigate = useNavigate()
  return (
    <GlassPanel className="mx-auto mt-6 w-full max-w-md p-8 text-center">
      <span className="inset mx-auto grid size-12 place-items-center rounded-full">
        <CircleDot className="size-5 text-case" />
      </span>
      <p className="mt-3 text-base font-semibold">Mark a confirmed case first</p>
      <p className="mt-1 text-sm text-muted">
        {`The rings, and so ${what}, start from the confirmed cases. Mark one on the network screen.`}
      </p>
      <Button className="mt-5" variant="primary" icon={ArrowRight} onClick={() => navigate('/network')}>
        Go to the network
      </Button>
    </GlassPanel>
  )
}

/** Small coloured label. */
export function Pill({ color = 'var(--accent)', children, className = '' }) {
  return (
    <span
      className={`inline-flex h-6 items-center gap-1.5 rounded-full px-2.5 text-xs font-semibold whitespace-nowrap ${className}`}
      style={{ background: `color-mix(in srgb, ${color} 16%, transparent)`, color }}
    >
      <span className="size-1.5 rounded-full" style={{ background: color }} />
      {children}
    </span>
  )
}

/** Text input styled like the rest of the app. */
export function Field({ label, hint, className = '', ...props }) {
  return (
    <label className={`block ${className}`}>
      {label && <span className="text-xs font-medium text-muted">{label}</span>}
      <input
        className="inset mt-1 h-10 w-full rounded-xl px-3 text-sm text-text outline-none placeholder:text-faint focus:border-accent"
        {...props}
      />
      {hint && <span className="mt-1 block text-xs text-faint">{hint}</span>}
    </label>
  )
}

/** A labelled value that is already formatted text (a percentage, a size), in the StatTile style. */
export function TextStat({ label, value, tone }) {
  return (
    <div className="min-w-0">
      <div className="flex items-center gap-1.5 text-xs font-medium text-muted">
        {tone && <span className="size-1.5 rounded-full" style={{ background: tone }} />}
        <span className="truncate">{label}</span>
      </div>
      <div className="mt-0.5 text-2xl font-medium tracking-tight">{value}</div>
    </div>
  )
}
