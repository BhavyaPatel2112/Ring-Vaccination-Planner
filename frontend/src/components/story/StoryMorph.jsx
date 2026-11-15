import { useEffect, useState } from 'react'
import { ALL_EDGES, N, SCENES, split } from './storyScenes.js'

/*
 * Draws the story at a scroll position. `at` is a scene number that can be in
 * between, like 4.3: then every shape is 30% of the way from where scene 4 puts
 * it to where scene 5 puts it, and the same for size, roundness and colour.
 */

const VARS = {
  case: '--case', ring1: '--ring1', ring2: '--ring2', node: '--node', planned: '--planned',
  vaccinated: '--vaccinated', danger: '--danger', accent: '--accent', edge: '--edge',
  edgeSolid: '--edge-solid', glass: '--glass-strong', text: '--text', muted: '--text-muted', faint: '--text-faint',
}

function parse(colour) {
  const c = colour.trim()
  if (c.startsWith('#')) {
    const n = parseInt(c.length === 4 ? [...c.slice(1)].map((d) => d + d).join('') : c.slice(1, 7), 16)
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255, 1]
  }
  const m = c.match(/[\d.]+/g)?.map(Number) ?? [128, 128, 128]
  return [m[0], m[1], m[2], m[3] ?? 1]
}

/** Reads the theme's colours as numbers so they can be blended; re-reads on theme change. */
function usePalette() {
  const read = () => {
    const css = getComputedStyle(document.documentElement)
    const p = { white: [255, 255, 255, 1] }
    for (const [k, v] of Object.entries(VARS)) p[k] = parse(css.getPropertyValue(v) || '#888')
    return p
  }
  const [palette, setPalette] = useState(read)
  useEffect(() => {
    const update = () => requestAnimationFrame(() => setPalette(read()))
    window.addEventListener('ringvax-theme', update)
    return () => window.removeEventListener('ringvax-theme', update)
  }, [])
  return palette
}

const lerp = (a, b, t) => a + (b - a) * t
const mix = (a, b, t) => a.map((v, i) => lerp(v, b[i], t))
const rgba = ([r, g, b, a], o = 1) => `rgba(${r | 0},${g | 0},${b | 0},${(a * o).toFixed(3)})`

export function StoryMorph({ at }) {
  const palette = usePalette()
  const { i, j, t } = split(at)
  const A = SCENES[i]
  const B = SCENES[j]

  const shapes = Array.from({ length: N }, (_, k) => {
    const a = A.shapes[k]
    const b = B.shapes[k]
    return {
      x: lerp(a.x, b.x, t),
      y: lerp(a.y, b.y, t),
      w: lerp(a.w, b.w, t),
      h: lerp(a.h, b.h, t),
      r: lerp(a.r, b.r, t),
      o: lerp(a.o, b.o, t),
      c: mix(palette[a.c], palette[b.c], t),
    }
  })

  const edges = ALL_EDGES.map(({ key, a, b }) => {
    const ea = A.edges?.[key]
    const eb = B.edges?.[key]
    if (!ea && !eb) return null
    const from = ea ?? { ...eb, o: 0 }
    const to = eb ?? { ...ea, o: 0 }
    const o = lerp(from.o, to.o, t) * Math.min(shapes[a].o, shapes[b].o, 1)
    if (o < 0.01) return null
    return { key, a: shapes[a], b: shapes[b], w: lerp(from.w, to.w, t), c: mix(palette[from.c], palette[to.c], t), o }
  })

  // Labels leave early and arrive late, so mid-morph only the shapes are moving.
  const overlays = [
    ...(A.overlays ?? []).map((o, k) => ({ ...o, key: `a${i}-${k}`, fade: Math.max(0, 1 - t * 2.5) })),
    ...(j !== i ? (B.overlays ?? []).map((o, k) => ({ ...o, key: `b${j}-${k}`, fade: Math.max(0, (t - 0.6) / 0.4) })) : []),
  ].filter((o) => o.fade > 0.02)

  return (
    <svg viewBox="-10 0 420 310" className="h-full w-full overflow-visible" role="img" aria-label="Illustration for the current idea">
      {overlays
        .filter((o) => o.t === 'circle')
        .map((o) => (
          <circle key={o.key} cx={o.x} cy={o.y} r={o.r} fill="none" stroke={rgba(palette[o.c], 0.35 * o.fade)} strokeWidth={1.5} strokeDasharray="4 6" />
        ))}
      {edges.map(
        (e) =>
          e && <line key={e.key} x1={e.a.x} y1={e.a.y} x2={e.b.x} y2={e.b.y} stroke={rgba(e.c, e.o)} strokeWidth={e.w} strokeLinecap="round" />,
      )}
      {shapes.map((s, k) =>
        s.o < 0.01 || s.w < 0.2 ? null : (
          <rect
            key={k}
            x={s.x - s.w / 2}
            y={s.y - s.h / 2}
            width={s.w}
            height={s.h}
            rx={s.r * Math.min(s.w, s.h)}
            fill={rgba(s.c, s.o)}
          />
        ),
      )}
      {overlays
        .filter((o) => o.t === 'text')
        .map((o) => (
          <text
            key={o.key}
            x={o.x}
            y={o.y}
            textAnchor={o.anchor}
            fill={rgba(palette[o.c], o.fade)}
            fontSize={o.size}
            fontWeight={o.weight}
            fontFamily={o.mono ? 'ui-monospace, SFMono-Regular, Menlo, monospace' : undefined}
          >
            {o.value}
          </text>
        ))}
    </svg>
  )
}
