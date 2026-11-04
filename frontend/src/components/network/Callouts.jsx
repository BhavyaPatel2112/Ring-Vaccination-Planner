import { useEffect, useRef, useState } from 'react'

/**
 * Dashed callout lines from panel rows to people in the network, like labels
 * pinned to a model. Each link is { key, nodeId, color }; the row it starts
 * from carries data-callout="<key>". Positions are updated every frame by
 * writing straight to the SVG, so React does not re-render.
 */
export function Callouts({ canvas, links }) {
  const svg = useRef(null)
  const [wide, setWide] = useState(() => window.matchMedia('(min-width: 1024px)').matches)

  useEffect(() => {
    const query = window.matchMedia('(min-width: 1024px)')
    const update = () => setWide(query.matches)
    query.addEventListener('change', update)
    return () => query.removeEventListener('change', update)
  }, [])

  useEffect(() => {
    if (!wide) return
    let frame
    const draw = () => {
      const root = svg.current
      if (root) {
        for (const link of links) {
          const group = root.querySelector(`[data-link="${CSS.escape(link.key)}"]`)
          if (!group) continue
          const anchor = document.querySelector(`[data-callout="${CSS.escape(link.key)}"]`)
          const point = canvas.current?.pointOf(link.nodeId)
          const box = anchor?.getBoundingClientRect()
          const visible = box && box.width > 0 && box.height > 0 && point && box.bottom > 0 && box.top < window.innerHeight
          group.style.opacity = visible ? '1' : '0'
          if (!visible) continue
          const fromRight = box.right < point.x
          const sx = fromRight ? box.right + 6 : box.left - 6
          const sy = box.top + box.height / 2
          const ex = sx + (fromRight ? 44 : -44)
          group.querySelector('path').setAttribute('d', `M ${sx} ${sy} L ${ex} ${sy} L ${point.x} ${point.y}`)
          const [start, ring, dot] = group.querySelectorAll('circle')
          start.setAttribute('cx', sx)
          start.setAttribute('cy', sy)
          ring.setAttribute('cx', point.x)
          ring.setAttribute('cy', point.y)
          dot.setAttribute('cx', point.x)
          dot.setAttribute('cy', point.y)
        }
      }
    }
    const loop = () => {
      draw()
      frame = requestAnimationFrame(loop)
    }
    frame = requestAnimationFrame(loop)
    // Browsers pause animation frames in background or throttled views; keep the lines roughly in place there too.
    const timer = setInterval(draw, 200)
    return () => {
      cancelAnimationFrame(frame)
      clearInterval(timer)
    }
  }, [links, canvas, wide])

  if (!wide) return null
  return (
    <svg ref={svg} className="pointer-events-none fixed inset-0 z-[15] h-full w-full" aria-hidden="true">
      {links.map((link) => (
        <g key={link.key} data-link={link.key} style={{ opacity: 0, transition: 'opacity 200ms' }}>
          <path d="" fill="none" stroke={link.color} strokeWidth="1.5" strokeDasharray="4 5" />
          <circle r="4" fill={link.color} />
          <circle r="9" fill="none" stroke={link.color} strokeWidth="1.5" className="callout-pulse" />
          <circle r="3" fill={link.color} />
        </g>
      ))}
    </svg>
  )
}
