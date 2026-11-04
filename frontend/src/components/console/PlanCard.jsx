import { motion } from 'motion/react'
import { ArrowRight, ShieldCheck } from 'lucide-react'
import { useNavigate } from 'react-router'
import { useWorkspace } from '../../state/context.js'
import { Button } from '../ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../ui/GlassPanel.jsx'
import { Notice } from '../ui/Notice.jsx'
import { Segmented } from '../ui/Segmented.jsx'
import { StatTile } from '../ui/StatTile.jsx'

/** Generate the plan and show the doses and the guarantee in plain words. */
export function PlanCard({ delay }) {
  const { plan, cases, ring, method, setMethod, createPlan, busy } = useWorkspace()
  const navigate = useNavigate()
  const ratio = plan?.provenRatioBound

  return (
    <GlassPanel delay={delay}>
      <PanelTitle icon={ShieldCheck} title="Vaccination plan" subtitle="Fewest doses that protect every link" />
      <Segmented
        size="sm"
        label="Method"
        className="mb-3 w-full justify-between"
        value={method}
        onChange={setMethod}
        options={[
          { value: 'APPROX', label: 'Approximation' },
          { value: 'EXACT', label: 'Exact' },
        ]}
      />
      {!plan && (
        <Button
          variant="primary"
          className="w-full"
          loading={busy === 'plan'}
          disabled={cases.length === 0 || !ring}
          onClick={() => createPlan().catch(() => {})}
        >
          Generate plan
        </Button>
      )}
      {!plan && cases.length === 0 && <p className="mt-2 text-xs text-muted">Mark a confirmed case first.</p>}
      {!plan && method === 'EXACT' && cases.length > 0 && (
        <p className="mt-2 text-xs text-muted">Exact works on small rings only (up to 40 people to decide).</p>
      )}
      {plan && (
        <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} className="flex flex-col gap-3">
          <div className="flex items-end justify-between gap-3">
            <StatTile label="Doses" value={plan.doses} size="lg" tone="var(--planned)" />
            <div className="pb-2 text-right text-xs text-muted">
              <p>{plan.method === 'EXACT' ? 'Exact minimum' : '2-approximation'}</p>
              <p>{plan.runtimeMs.toFixed(plan.runtimeMs < 10 ? 2 : 0)} ms in Java</p>
            </div>
          </div>
          {ratio != null && (
            <div>
              <div className="flex justify-between text-xs text-muted">
                <span>Distance from the minimum</span>
                <span className="font-semibold text-text">at most {ratio.toFixed(2)}×</span>
              </div>
              <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-inset">
                <motion.div
                  className="h-full rounded-full"
                  style={{ background: ratio <= 1.0001 ? 'var(--vaccinated)' : 'var(--planned)' }}
                  initial={{ width: 0 }}
                  animate={{ width: `${Math.min(ratio / 2, 1) * 100}%` }}
                  transition={{ duration: 0.8, ease: 'easeOut' }}
                />
              </div>
              <div className="mt-1 flex justify-between text-xs text-faint">
                <span>1× (optimal)</span>
                <span>2× (guarantee)</span>
              </div>
            </div>
          )}
          <p className="text-xs leading-relaxed text-muted">{plan.guarantee}</p>
          {plan.unprotectableLinks.length > 0 && (
            <Notice tone="warning" title={`${plan.unprotectableLinks.length} links cannot be protected by vaccination`}>
              Both people on these links cannot be vaccinated. Quarantine or monitor them instead.
            </Notice>
          )}
          <div className="flex gap-2">
            <Button size="sm" className="flex-1" onClick={() => createPlan().catch(() => {})} loading={busy === 'plan'}>
              Recalculate
            </Button>
            <Button size="sm" variant="primary" className="flex-1" icon={ArrowRight} onClick={() => navigate('/plan')}>
              Plan details
            </Button>
          </div>
        </motion.div>
      )}
    </GlassPanel>
  )
}
