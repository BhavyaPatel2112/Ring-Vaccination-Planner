import { AnimatePresence, motion } from 'motion/react'
import { Crosshair, X } from 'lucide-react'
import { initials } from '../../lib/format.js'
import { useWorkspace } from '../../state/context.js'
import { statusOf } from '../network/personStatus.js'
import { Button } from '../ui/Button.jsx'

/** Details of the person clicked in the network, with a callout line to them. */
export function PersonCard({ personId, ctx, onClose, onFocus }) {
  const { people, cases, toggleCase, plan } = useWorkspace()
  const person = personId ? people.get(personId) : null
  const dose = plan?.people.find((p) => p.personId === personId)
  const isCase = cases.includes(personId)

  return (
    <AnimatePresence mode="popLayout">
      {person && (
        <motion.section
          key={person.id}
          data-callout="selected"
          initial={{ opacity: 0, x: 16 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 16 }}
          className="glass glass-strong rounded-[22px] p-4"
          aria-label={`Details for ${person.name || person.id}`}
        >
          <div className="flex items-start gap-3">
            <span className="inset grid size-11 shrink-0 place-items-center rounded-full text-sm font-bold">
              {initials(person.name, person.id)}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[15px] font-semibold">{person.name || person.id}</p>
              <p className="text-xs text-muted">
                {person.id}
                {person.age != null && ` · ${person.age} yrs`}
                {person.role && ` · ${person.role}`}
              </p>
            </div>
            <button type="button" onClick={onClose} aria-label="Close details" className="text-muted hover:text-text">
              <X className="size-4" />
            </button>
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
            <div className="inset rounded-xl p-2">
              <p className="text-muted">Contacts</p>
              <p className="text-lg font-semibold">{person.contacts}</p>
            </div>
            <div className="inset rounded-xl p-2">
              <p className="text-muted">Status</p>
              <p className="font-semibold" style={{ color: statusOf(person, ctx).color }}>
                {statusOf(person, ctx).label}
              </p>
            </div>
          </div>
          {dose && (
            <p className="mt-2 rounded-xl bg-planned/10 p-2 text-xs text-text">
              <span className="font-semibold text-planned">Why: </span>
              {dose.explanation}
            </p>
          )}
          <div className="mt-3 flex gap-2">
            <Button size="sm" variant={isCase ? 'glass' : 'danger'} className="flex-1" onClick={() => toggleCase(person.id)}>
              {isCase ? 'Remove case' : 'Mark as confirmed case'}
            </Button>
            <Button size="sm" icon={Crosshair} onClick={() => onFocus(person.id)} aria-label="Centre on this person" />
          </div>
        </motion.section>
      )}
    </AnimatePresence>
  )
}
