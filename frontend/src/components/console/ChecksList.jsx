import { useAutoAnimate } from '@formkit/auto-animate/react'
import { sentence } from '../../lib/format.js'

/** Rejected rows from the upload and contact tracing warnings, each with its line or person. */
export function ChecksList({ warnings, quality }) {
  const [listRef] = useAutoAnimate()
  const items = [
    ...quality.map((q, i) => ({ key: `q${i}`, code: q.code, text: q.message, where: q.personId })),
    ...warnings.map((w, i) => ({ key: `w${i}`, code: w.code, text: w.message, where: w.line > 0 ? `${w.file} line ${w.line}` : w.file })),
  ]
  if (items.length === 0) return <p className="px-1 py-3 text-sm text-muted">No problems found in the data.</p>
  return (
    <ul ref={listRef} className="scroll-thin flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto pr-1">
      {items.slice(0, 100).map((item) => (
        <li key={item.key} className="inset rounded-xl px-2.5 py-2">
          <p className="text-xs font-semibold text-text">
            {sentence(item.code)}
            {item.where && <span className="font-normal text-muted"> · {item.where}</span>}
          </p>
          <p className="mt-0.5 text-xs leading-relaxed text-muted">{item.text}</p>
        </li>
      ))}
    </ul>
  )
}
