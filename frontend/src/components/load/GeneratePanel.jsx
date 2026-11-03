import { Building2, Dice5 } from 'lucide-react'
import { useState } from 'react'
import { formatNumber } from '../../lib/format.js'
import { Button } from '../ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../ui/GlassPanel.jsx'
import { Slider } from '../ui/Slider.jsx'

const POPULATIONS = [200, 500, 1000, 2500, 5000, 10000, 25000, 50000, 100000]

/** Settings for a seeded synthetic town (households, schools, workplaces). */
export function GeneratePanel({ onGenerate, busy, delay }) {
  const [size, setSize] = useState(3)
  const [contacts, setContacts] = useState(10)
  const [seed, setSeed] = useState(2026)
  const population = POPULATIONS[size]

  return (
    <GlassPanel delay={delay} grabber className="flex h-full flex-col">
      <PanelTitle icon={Building2} title="Generate a town" subtitle="Realistic synthetic contacts, same seed same town" />
      <div className="flex flex-col gap-5">
        <Slider label="People" value={size} onChange={setSize} min={0} max={POPULATIONS.length - 1} format={() => formatNumber(population)} />
        <Slider label="Average contacts per person" value={contacts} onChange={setContacts} min={4} max={40} />
        <label className="block">
          <span className="text-xs font-medium text-muted">Seed</span>
          <div className="inset mt-1.5 flex items-center gap-2 rounded-xl px-3 py-1.5">
            <input
              type="number"
              value={seed}
              onChange={(event) => setSeed(Number(event.target.value))}
              className="min-w-0 flex-1 bg-transparent text-sm font-semibold outline-none"
              aria-label="Seed"
            />
            <button
              type="button"
              onClick={() => setSeed(Math.floor(Math.random() * 100000))}
              className="grid size-7 place-items-center rounded-full text-muted hover:bg-inset hover:text-text"
              aria-label="Random seed"
              title="Random seed"
            >
              <Dice5 className="size-4" />
            </button>
          </div>
        </label>
        <p className="text-[11px] text-muted">
          About {formatNumber(Math.round((population * contacts) / 2))} contacts. Households of 2 to 6, schools and
          workplaces of 20 to 200, plus community links.
        </p>
      </div>
      <div className="mt-auto pt-3">
        <Button
          variant="primary"
          className="w-full"
          loading={busy}
          onClick={() => onGenerate({ population, avgContacts: contacts, seed })}
        >
          Generate network
        </Button>
      </div>
    </GlassPanel>
  )
}
