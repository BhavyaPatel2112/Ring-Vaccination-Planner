import { useAutoAnimate } from '@formkit/auto-animate/react'
import { statusOf } from '../network/personStatus.js'

const LIMIT = 80

/**
 * Rows of people with a coloured status chip. Hovering a row highlights the
 * person in the network and draws a callout line to them.
 */
export function PeopleList({ ids, people, ctx, onHover, onSelect, empty }) {
  const [listRef] = useAutoAnimate()
  if (ids.length === 0) return <p className="px-1 py-2 text-xs text-muted">{empty}</p>
  const shown = ids.slice(0, LIMIT)
  return (
    <>
      <ul ref={listRef} className="scroll-thin flex max-h-64 flex-col gap-1 overflow-y-auto pr-1">
        {shown.map((id) => {
          const person = people.get(id) ?? { id, name: '', eligible: true }
          const status = statusOf(person, ctx)
          return (
            <li key={id}>
              <button
                type="button"
                data-callout={`row-${id}`}
                onMouseEnter={() => onHover(id)}
                onMouseLeave={() => onHover(null)}
                onFocus={() => onHover(id)}
                onBlur={() => onHover(null)}
                onClick={() => onSelect(id)}
                className="inset flex w-full items-center gap-2 rounded-xl px-2.5 py-2 text-left transition hover:border-accent/40"
              >
                <span className="size-2 shrink-0 rounded-full" style={{ background: status.color }} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-xs font-semibold">{person.name || id}</span>
                  <span className="block truncate text-[11px] text-muted">
                    {id}
                    {person.role && ` · ${person.role}`}
                  </span>
                </span>
                <span
                  className="shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold"
                  style={{ color: status.color, background: `color-mix(in srgb, ${status.color} 14%, transparent)` }}
                >
                  {status.label.split(':')[0]}
                </span>
              </button>
            </li>
          )
        })}
      </ul>
      {ids.length > LIMIT && <p className="px-1 pt-2 text-[11px] text-muted">and {ids.length - LIMIT} more</p>}
    </>
  )
}
