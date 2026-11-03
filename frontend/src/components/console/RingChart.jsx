import { ArcElement, Chart as ChartJS, Tooltip } from 'chart.js'
import { useEffect, useState } from 'react'
import { Doughnut } from 'react-chartjs-2'

ChartJS.register(ArcElement, Tooltip)

const SLICES = [
  ['Ring 1 doses', '--ring1'],
  ['Ring 2', '--ring2'],
  ['Already vaccinated', '--vaccinated'],
  ['Cannot be vaccinated', '--excluded'],
]

function readColors() {
  const css = getComputedStyle(document.documentElement)
  return {
    slices: SLICES.map(([, token]) => css.getPropertyValue(token).trim()),
    border: css.getPropertyValue('--scene-bottom').trim(),
    text: css.getPropertyValue('--text').trim(),
  }
}

/** Doughnut of who is in the rings, with the total in the middle. */
export function RingChart({ ring }) {
  const [colors, setColors] = useState(readColors)
  useEffect(() => {
    const update = () => setColors(readColors())
    window.addEventListener('ringvax-theme', update)
    return () => window.removeEventListener('ringvax-theme', update)
  }, [])

  const ringTwoPlus = ring.rings.slice(1).flat()
  const skip = new Set([...ring.alreadyVaccinated, ...ring.cannotVaccinate])
  const values = [
    ring.mandatory.length,
    ringTwoPlus.filter((id) => !skip.has(id)).length,
    ring.alreadyVaccinated.length,
    ring.cannotVaccinate.length,
  ]
  const total = values.reduce((a, b) => a + b, 0)

  return (
    <div className="flex items-center gap-4">
      <div className="relative size-28 shrink-0">
        <Doughnut
          data={{
            labels: SLICES.map(([label]) => label),
            datasets: [{ data: values, backgroundColor: colors.slices, borderColor: colors.border, borderWidth: 2, hoverOffset: 4 }],
          }}
          options={{ cutout: '72%', plugins: { legend: { display: false } }, animation: { animateRotate: true, duration: 700 } }}
        />
        <div className="pointer-events-none absolute inset-0 grid place-items-center text-center">
          <div>
            <p className="text-xl font-semibold">{total}</p>
            <p className="text-[10px] text-muted">in rings</p>
          </div>
        </div>
      </div>
      <ul className="flex min-w-0 flex-col gap-1.5 text-xs">
        {SLICES.map(([label], i) => (
          <li key={label} className="flex items-center gap-2">
            <span className="size-2 rounded-full" style={{ background: colors.slices[i] }} />
            <span className="truncate text-muted">{label}</span>
            <span className="ml-auto pl-2 font-semibold">{values[i]}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
