/** Curved grid wall behind the network, like a room the model stands in. */
export function CurvedGrid() {
  const vertical = Array.from({ length: 21 }, (_, i) => i / 20)
  const horizontal = Array.from({ length: 11 }, (_, i) => i / 10)
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 1000 700"
      preserveAspectRatio="none"
      className="pointer-events-none absolute inset-0 h-full w-full"
      style={{ maskImage: 'radial-gradient(ellipse at center, black 35%, transparent 75%)' }}
    >
      <g fill="none" stroke="var(--grid-line)" strokeWidth="1.2">
        {vertical.map((t) => {
          const x = 1000 * t
          const bow = (t - 0.5) * 120
          return <path key={`v${t}`} d={`M ${x} 0 Q ${x + bow} 350 ${x} 700`} />
        })}
        {horizontal.map((t) => {
          const y = 40 + 620 * t
          const sag = 50 * (1 - Math.abs(t - 0.5) * 2)
          return <path key={`h${t}`} d={`M 0 ${y} Q 500 ${y + sag} 1000 ${y}`} />
        })}
      </g>
    </svg>
  )
}
