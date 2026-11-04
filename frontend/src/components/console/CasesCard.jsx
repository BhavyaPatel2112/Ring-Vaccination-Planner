import { useAutoAnimate } from '@formkit/auto-animate/react'
import { Plus, X } from 'lucide-react'
import { useState } from 'react'
import { useWorkspace } from '../../state/context.js'
import { Segmented } from '../ui/Segmented.jsx'

/** Confirmed cases, a quick "add case" box and the ring depth. */
export function CasesCard({ onHover, onSelect }) {
  const { cases, people, toggleCase, depth, setDepth, ring } = useWorkspace()
  const [query, setQuery] = useState('')
  const [listRef] = useAutoAnimate()
  const isolated = new Set(ring?.casesWithoutContacts)

  const add = (event) => {
    event.preventDefault()
    const text = query.trim().toLowerCase()
    if (!text) return
    const match =
      people.get(query.trim()) ?? [...people.values()].find((p) => p.id.toLowerCase() === text || p.name.toLowerCase() === text)
    if (match && !cases.includes(match.id)) toggleCase(match.id)
    setQuery('')
  }

  return (
    <section className="glass flex flex-col gap-2 rounded-[20px] p-4" aria-label="Confirmed cases">
      <h2 className="flex items-center gap-2 text-[15px] font-semibold tracking-tight">
        <span className="size-2 rounded-full bg-case" />
        Confirmed cases
        <span className="text-sm font-semibold text-muted">{cases.length}</span>
      </h2>
      <ul ref={listRef} className="flex flex-col gap-1">
        {cases.map((id) => {
          const person = people.get(id)
          return (
            <li key={id}>
              <div
                data-callout={`row-${id}`}
                className="inset flex items-center gap-2 rounded-xl px-2.5 py-2"
                onMouseEnter={() => onHover(id)}
                onMouseLeave={() => onHover(null)}
              >
                <span className="size-2.5 shrink-0 rounded-full bg-case" />
                <button type="button" onClick={() => onSelect(id)} className="min-w-0 flex-1 text-left">
                  <span className="block truncate text-sm font-semibold">{person?.name || id}</span>
                  <span className="block truncate text-xs text-muted">
                    {id}
                    {person?.role && ` · ${person.role}`}
                    {isolated.has(id) && ' · no recorded contacts'}
                  </span>
                </button>
                <button
                  type="button"
                  onClick={() => toggleCase(id)}
                  aria-label={`Remove ${id} from confirmed cases`}
                  className="grid size-8 place-items-center rounded-full text-muted hover:bg-case/15 hover:text-case"
                >
                  <X className="size-4" />
                </button>
              </div>
            </li>
          )
        })}
      </ul>
      {cases.length === 0 && (
        <p className="px-1 text-xs text-muted">No cases yet.</p>
      )}
      <form onSubmit={add} className="inset flex items-center gap-2 rounded-xl px-2.5 py-1.5">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          list="people-options"
          placeholder="Add a case by ID or name"
          aria-label="Add a confirmed case by ID or name"
          className="min-w-0 flex-1 bg-transparent py-1 text-sm outline-none placeholder:text-faint"
        />
        <datalist id="people-options">
          {[...people.values()].slice(0, 2000).map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </datalist>
        <button type="submit" aria-label="Add case" className="grid size-8 place-items-center rounded-full bg-accent text-white">
          <Plus className="size-4" />
        </button>
      </form>
      <div className="flex items-center justify-between px-1 pt-1">
        <span className="text-sm text-muted">Ring depth</span>
        <Segmented
          size="sm"
          label="Ring depth"
          value={depth}
          onChange={setDepth}
          options={[1, 2, 3].map((d) => ({ value: d, label: String(d), ariaLabel: `Depth ${d}` }))}
        />
      </div>
    </section>
  )
}
