import { Check } from 'lucide-react'
import { cn } from '../../lib/cn.js'
import { useWorkspace } from '../../state/context.js'

/** Three steps with the current one explained in a single sentence. */
export function GuideCard() {
  const { cases, ring, plan } = useWorkspace()
  const steps = [
    { title: 'Mark confirmed cases', hint: 'Click a person on the map, or add a case by ID below.', done: cases.length > 0 },
    { title: 'Review the rings', hint: 'Ring 1 is vaccinated first. Check who cannot be vaccinated.', done: !!ring },
    { title: 'Generate the plan', hint: 'Choose a method on the right and generate the plan.', done: !!plan },
  ]
  const current = steps.findIndex((s) => !s.done)

  return (
    <section className="glass rounded-[20px] p-4" aria-label="Steps">
      <ol className="flex flex-col gap-2.5">
        {steps.map((step, i) => {
          const state = step.done ? 'done' : i === current ? 'current' : 'todo'
          return (
            <li key={step.title} className="flex gap-3">
              <span
                className={cn(
                  'mt-0.5 grid size-6 shrink-0 place-items-center rounded-full text-xs font-bold',
                  state === 'done' && 'bg-vaccinated text-white',
                  state === 'current' && 'bg-accent text-white',
                  state === 'todo' && 'inset text-muted',
                )}
              >
                {state === 'done' ? <Check className="size-3.5" strokeWidth={3} /> : i + 1}
              </span>
              <div className="min-w-0">
                <p className={cn('text-sm font-semibold', state === 'todo' && 'text-muted')}>{step.title}</p>
                {state === 'current' && <p className="mt-0.5 text-xs leading-relaxed text-muted">{step.hint}</p>}
              </div>
            </li>
          )
        })}
      </ol>
      {current === -1 && (
        <p className="mt-3 rounded-xl bg-vaccinated/10 px-3 py-2 text-xs font-medium text-text">
          Plan ready. Open plan details to see who to vaccinate and why.
        </p>
      )}
    </section>
  )
}
