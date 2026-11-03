import { motion } from 'motion/react'
import { Check } from 'lucide-react'
import { useNavigate } from 'react-router'
import { cn } from '../../lib/cn.js'
import { useWorkspace } from '../../state/context.js'

/**
 * The five steps of the job along the bottom: load data, mark cases, review
 * rings, plan, act. Shows how far the current outbreak has got.
 */
export function WorkflowRail() {
  const { network, cases, ring, plan } = useWorkspace()
  const navigate = useNavigate()
  const steps = [
    { label: 'Load data', done: !!network, path: '/load' },
    { label: 'Mark cases', done: cases.length > 0, path: '/network' },
    { label: 'Review rings', done: !!ring, path: '/network' },
    { label: 'Plan doses', done: !!plan, path: '/network' },
    { label: 'Act', done: false, path: '/plan' },
  ]
  const current = steps.findIndex((s) => !s.done)
  const progress = (current === -1 ? steps.length : current) / (steps.length - 1)

  return (
    <nav aria-label="Workflow" className="pointer-events-none fixed inset-x-0 bottom-4 z-30 flex justify-center px-20">
      <div className="glass pointer-events-auto relative flex items-center gap-1 rounded-full p-1.5">
        <motion.div
          className="absolute bottom-0 left-4 h-0.5 rounded-full bg-accent"
          initial={false}
          animate={{ width: `calc(${Math.min(progress, 1) * 100}% - 2rem)` }}
          transition={{ type: 'spring', stiffness: 120, damping: 20 }}
        />
        {steps.map((step, i) => {
          const state = step.done ? 'done' : i === current ? 'current' : 'todo'
          return (
            <button
              key={step.label}
              type="button"
              onClick={() => navigate(step.path)}
              aria-current={state === 'current' ? 'step' : undefined}
              className={cn(
                'flex h-9 items-center gap-2 rounded-full px-3 text-xs font-semibold whitespace-nowrap transition',
                state === 'current' && 'bg-accent-soft text-text',
                state === 'done' && 'text-text hover:bg-inset',
                state === 'todo' && 'text-faint hover:text-muted',
              )}
            >
              <span
                className={cn(
                  'grid size-5 place-items-center rounded-full text-[10px]',
                  state === 'done' ? 'bg-vaccinated text-white' : state === 'current' ? 'bg-accent text-white' : 'inset',
                )}
              >
                {state === 'done' ? <Check className="size-3" strokeWidth={3} /> : i + 1}
              </span>
              <span className={cn(state !== 'current' && 'hidden md:inline')}>{step.label}</span>
            </button>
          )
        })}
      </div>
    </nav>
  )
}
