const ITEMS = [
  ['Confirmed case', 'var(--case)'],
  ['Ring 1', 'var(--ring1)'],
  ['Ring 2', 'var(--ring2)'],
  ['Already vaccinated', 'var(--vaccinated)'],
  ['Cannot be vaccinated', 'var(--excluded)'],
  ['Dose in plan', 'var(--planned)', true],
]

/** Colour key for the network. */
export function Legend() {
  return (
    <ul className="glass absolute bottom-3 left-3 flex max-w-[calc(100%-5rem)] flex-wrap items-center gap-x-3 gap-y-1 rounded-2xl px-3 py-2 text-[11px] font-medium text-muted">
      {ITEMS.map(([label, color, ring]) => (
        <li key={label} className="flex items-center gap-1.5">
          <span
            className="size-2.5 rounded-full"
            style={ring ? { border: `2px solid ${color}` } : { background: color }}
          />
          {label}
        </li>
      ))}
    </ul>
  )
}
