import { useAutoAnimate } from '@formkit/auto-animate/react'
import { motion } from 'motion/react'
import { ArrowRight, CircleCheck, TriangleAlert } from 'lucide-react'
import { formatNumber, sentence } from '../../lib/format.js'
import { Button } from '../ui/Button.jsx'
import { StatTile } from '../ui/StatTile.jsx'

/** What was read from the files, and every row that was rejected or merged. */
export function LoadReport({ network, onOpen }) {
  const [listRef] = useAutoAnimate()
  const issues = network.warnings
  const quality = network.qualityWarnings
  const clean = issues.length === 0 && quality.length === 0
  return (
    <motion.section
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass rounded-[20px] p-5"
      aria-label="Load report"
    >
      <div className="flex flex-wrap items-center gap-4">
        <span className={`grid size-11 place-items-center rounded-full ${clean ? 'bg-vaccinated/15' : 'bg-warning/15'}`}>
          {clean ? <CircleCheck className="size-5 text-vaccinated" /> : <TriangleAlert className="size-5 text-warning" />}
        </span>
        <div className="min-w-0 flex-1">
          <h2 className="text-lg font-semibold tracking-tight">{network.name}</h2>
          <p className="text-xs text-muted">
            {clean
              ? 'Every row was read. No problems found.'
              : `${formatNumber(issues.length)} rows rejected or merged, ${formatNumber(quality.length)} data warnings. Nothing was silently dropped.`}
          </p>
        </div>
        <Button variant="primary" icon={ArrowRight} onClick={onOpen}>
          Open the network
        </Button>
      </div>
      <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatTile label="People" value={network.people} />
        <StatTile label="Contacts" value={network.contacts} />
        <StatTile label="Confirmed cases" value={network.caseIds.length} tone="var(--case)" />
        <StatTile label="Venue visits" value={network.checkIns} />
      </div>
      {!clean && (
        <ul ref={listRef} className="scroll-thin mt-4 grid max-h-56 gap-1.5 overflow-y-auto pr-1 md:grid-cols-2">
          {[...quality.map((q) => ({ code: q.code, where: q.personId, text: q.message })),
            ...issues.map((w) => ({ code: w.code, where: w.line > 0 ? `${w.file} line ${w.line}` : w.file, text: w.message }))]
            .slice(0, 120)
            .map((item, i) => (
              <li key={i} className="inset rounded-xl px-3 py-2">
                <p className="text-xs font-semibold">
                  {sentence(item.code)} <span className="font-normal text-muted">· {item.where}</span>
                </p>
                <p className="text-xs text-muted">{item.text}</p>
              </li>
            ))}
        </ul>
      )}
    </motion.section>
  )
}
