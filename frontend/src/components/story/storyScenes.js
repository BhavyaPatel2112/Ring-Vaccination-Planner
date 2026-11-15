/*
 * The project story is one drawing made of the same N shapes. Each scene says
 * where every shape is, how big, how round, and what colour; scrolling moves
 * and reshapes the shapes from one scene into the next. A rounded rectangle can
 * be a dot (fully round), a pill, a bar or a square, so one shape can turn into
 * another. Links are drawn between shape centres, so they follow the shapes.
 *
 * These are illustrations only. The real algorithms run in the Java backend,
 * and the numbers shown are the measured ones from docs/benchmarks and README.
 */

export const N = 50

// ------------------------------------------------------------------ shape helpers

/** x, y are the centre; r is roundness (0.5 = fully round, 0 = square corners). */
const dot = (x, y, c = 'node', s = 16, o = 1) => ({ x, y, w: s, h: s, r: 0.5, c, o })
const box = (x, y, w, h, c, r = 0.5, o = 1) => ({ x, y, w, h, r, c, o })
const barH = (left, y, len, h, c, o = 1) => ({ x: left + len / 2, y, w: len, h, r: 0.5, c, o })
const barV = (x, bottom, len, w, c, o = 1) => ({ x, y: bottom - len / 2, w, h: len, r: 0.3, c, o })

/** Unused shapes shrink to nothing inside a visible one, so they grow out of it later. */
function pad(shapes) {
  const out = [...shapes]
  for (let i = shapes.length; i < N; i++) {
    const a = shapes[i % shapes.length]
    out.push({ x: a.x, y: a.y, w: 0, h: 0, r: 0.5, c: a.c, o: 0 })
  }
  return out
}

const link = (pairs, c = 'edge', w = 2, o = 1) => Object.fromEntries(pairs.map(([a, b]) => [`${a}-${b}`, { c, w, o }]))
const text = (x, y, value, opts = {}) => ({ t: 'text', x, y, value, size: 13, c: 'muted', weight: 600, anchor: 'middle', ...opts })
const ring = (r, c) => ({ t: 'circle', x: 200, y: 150, r, c })

// ------------------------------------------------------------------ the sample contact network

const NET = [
  [200, 150, 0], [130, 95, 1], [275, 90, 1], [285, 205, 1], [125, 210, 1],
  [60, 50, 2], [205, 35, 2], [350, 45, 2], [365, 150, 2], [340, 265, 2], [200, 272, 2], [55, 250, 2], [40, 145, 2],
]
const NET_EDGES = [
  [0, 1], [0, 2], [0, 3], [0, 4], [1, 2], [3, 4], [1, 5], [1, 6], [2, 6],
  [2, 7], [2, 8], [3, 8], [3, 9], [3, 10], [4, 10], [4, 11], [4, 12], [1, 12],
]
const LAYER = ['case', 'ring1', 'ring2']

function rings(inner, outer, centre = 28) {
  const s = [dot(200, 150, 'case', centre)]
  for (let i = 0; i < inner; i++) {
    const a = (i / inner) * Math.PI * 2 + 0.3
    s.push(dot(200 + 72 * Math.cos(a), 150 + 72 * Math.sin(a), 'ring1', 15))
  }
  for (let i = 0; i < outer; i++) {
    const a = (i / outer) * Math.PI * 2
    s.push(dot(200 + 128 * Math.cos(a), 150 + 128 * Math.sin(a), 'ring2', 12))
  }
  return s
}

function network(colour) {
  return NET.map(([x, y, l], i) => {
    const c = colour(i, l)
    return dot(x, y, c, c === 'planned' ? 22 : 16)
  })
}

// ------------------------------------------------------------------ scenes, in the same order as the text

const title = {
  shapes: pad(rings(6, 12)),
  overlays: [ring(72, 'ring1'), ring(128, 'ring2')],
}

const model = {
  shapes: pad(network(() => 'node')),
  edges: link(NET_EDGES),
}

const bfs = {
  shapes: pad(network((_, l) => LAYER[l])),
  edges: link(NET_EDGES),
  overlays: [text(110, 300, '● Case', { c: 'case' }), text(200, 300, '● Ring 1', { c: 'ring1' }), text(290, 300, '● Ring 2', { c: 'ring2' })],
}

const cover = {
  shapes: pad(network((i) => (i >= 1 && i <= 4 ? 'planned' : 'node'))),
  edges: link(NET_EDGES, 'vaccinated', 2.5),
  overlays: [text(200, 300, '4 vaccinated people protect all 18 links')],
}

const MATCHED = [[0, 1], [2, 7], [3, 9], [4, 11]]
const matching = {
  shapes: pad(network((i) => (MATCHED.flat().includes(i) ? 'planned' : 'node'))),
  edges: { ...link(NET_EDGES, 'vaccinated', 2), ...link(MATCHED, 'planned', 5) },
  overlays: [text(200, 300, '8 doses · the fewest is 4 · never more than double')],
}

const TREE = [[200, 40], [120, 115], [280, 115], [70, 190], [170, 190], [40, 265], [110, 265]]
const CUT = [2, 4, 5]
const branch = {
  shapes: pad(
    TREE.map(([x, y], i) => dot(x, y, i === 6 ? 'vaccinated' : CUT.includes(i) ? 'danger' : 'node', 26, CUT.includes(i) ? 0.55 : 1)),
  ),
  edges: {
    ...link([[0, 1], [1, 3], [3, 6]], 'edgeSolid', 2.5),
    ...link([[0, 2], [1, 4], [3, 5]], 'danger', 2, 0.5),
  },
  overlays: [
    ...CUT.map((i) => text(TREE[i][0], TREE[i][1] + 5, '✕', { c: 'white', size: 14, weight: 800 })),
    text(110, 270, '✓', { c: 'white', size: 14, weight: 800 }),
    text(300, 205, 'Cut: this branch', { anchor: 'start', x: 250 }),
    text(300, 223, "can't beat the best", { anchor: 'start', x: 250 }),
  ],
}

const HEAP = [6, 4, 4, 3, 2, 1]
const heap = {
  shapes: pad(HEAP.map((d, i) => barH(80, 55 + i * 38, d * 44, 26, i === 0 ? 'planned' : 'node', i === 0 ? 1 : 0.5))),
  overlays: [
    ...HEAP.map((d, i) => text(55, 60 + i * 38, 'ABCDEF'[i], { c: i === 0 ? 'planned' : 'muted', weight: 800 })),
    ...HEAP.map((d, i) => text(80 + d * 44 + 16, 60 + i * 38, String(d), { anchor: 'start' })),
    text(200, 292, "The top gets the dose; its neighbours' keys drop"),
  ],
}

const S = 250 / 42
const trap = {
  shapes: pad([
    barH(120, 70, 12 * S, 24, 'vaccinated'),
    barH(120, 102, 23 * S, 24, 'danger'),
    barH(120, 185, 18 * S, 24, 'vaccinated'),
    barH(120, 217, 40 * S, 24, 'danger'),
    box(120 + 24 * S, 86, 3, 68, 'accent', 0.5),
    box(120 + 36 * S, 201, 3, 68, 'accent', 0.5),
  ]),
  overlays: [
    text(120, 42, 'Trap graph A', { anchor: 'start', c: 'text' }),
    text(120, 157, 'Trap graph B', { anchor: 'start', c: 'text' }),
    text(110, 75, 'Fewest', { anchor: 'end' }),
    text(110, 107, 'Greedy', { anchor: 'end' }),
    text(110, 190, 'Fewest', { anchor: 'end' }),
    text(110, 222, 'Greedy', { anchor: 'end' }),
    text(405, 75, '12', { anchor: 'end', c: 'text', weight: 800 }),
    text(405, 107, '23', { anchor: 'end', c: 'text', weight: 800 }),
    text(405, 190, '18', { anchor: 'end', c: 'text', weight: 800 }),
    text(405, 222, '40', { anchor: 'end', c: 'text', weight: 800 }),
    text(200, 280, 'Blue line: twice the fewest. The approximation never crosses it.'),
  ],
}

const select = {
  shapes: pad(
    Array.from({ length: 12 }, (_, i) => barV(57 + i * 26, 250, (i + 1) * 17, 18, i >= 10 ? 'planned' : 'node', i >= 10 ? 1 : 0.45)),
  ),
  overlays: [text(200, 285, 'Partition, keep only the side with the answer, repeat')],
}

const CLUSTERS = [
  [90, 75, 'case'], [130, 105, 'case'], [80, 125, 'case'],
  [290, 70, 'ring1'], [330, 100, 'ring1'], [285, 125, 'ring1'],
  [110, 215, 'planned'], [150, 240, 'planned'],
  [270, 230, 'vaccinated'], [315, 210, 'vaccinated'],
]
const INSIDE = [[0, 1], [1, 2], [3, 4], [4, 5], [6, 7], [8, 9]]
const BRIDGES = [[1, 5], [2, 6], [7, 8]]
const clusters = {
  shapes: pad(CLUSTERS.map(([x, y, c]) => dot(x, y, c, 20))),
  edges: link(INSIDE),
  overlays: [text(200, 292, '4 separate clusters', { c: 'text', size: 15, weight: 700 })],
}

const MERGED = [[170, 70], [205, 95], [165, 120], [235, 60], [265, 90], [240, 125], [195, 150], [230, 165], [270, 150], [140, 160]]
const measured = {
  shapes: pad(MERGED.map(([x, y]) => dot(x, y, 'case', 20))),
  edges: { ...link(INSIDE), ...link(BRIDGES, 'case', 2.5) },
  overlays: [
    text(110, 238, '0.023 µs', { c: 'vaccinated', size: 30, weight: 800 }),
    text(110, 262, 'union-find, per update'),
    text(295, 238, '64.58 ms', { c: 'muted', size: 30, weight: 800 }),
    text(295, 262, 'BFS rebuild, per update'),
  ],
}

const BITS = 16
const HITS = [2, 9, 13]
const bitX = (i) => 27 + i * 23
const hashing = {
  shapes: pad([
    ...Array.from({ length: BITS }, (_, i) => box(bitX(i), 215, 19, 30, HITS.includes(i) ? 'accent' : 'node', 0.2, HITS.includes(i) ? 1 : 0.22)),
    box(200, 50, 220, 42, 'glass', 0.5),
  ]),
  edges: link(HITS.map((h) => [BITS, h]), 'accent', 2),
  overlays: [
    text(200, 55, 'V07|2026-11-02T18', { c: 'text', size: 14, weight: 700, mono: true }),
    ...Array.from({ length: BITS }, (_, i) => text(bitX(i), 248, String(i), { size: 9, c: 'faint' })),
    text(60, 130, 'h₁ + i·h₂', { c: 'muted' }),
  ],
}

const SIDE = 170
const bloom = {
  shapes: pad([box(120, 140, SIDE, SIDE, 'node', 0.08, 0.3), box(310, 140 + SIDE / 2 - SIDE / Math.sqrt(88) / 2, SIDE / Math.sqrt(88), SIDE / Math.sqrt(88), 'accent', 0.15)]),
  overlays: [
    text(120, 248, 'HashSet', { c: 'text', weight: 700 }),
    text(310, 248, 'Bloom filter', { c: 'text', weight: 700 }),
    text(310, 150, '88×', { c: 'text', size: 52, weight: 800 }),
    text(310, 175, 'less memory'),
    text(200, 288, 'False positive rate within 3% of the formula'),
  ],
}

function curve(f, c) {
  return Array.from({ length: 12 }, (_, k) => {
    const t = k / 11
    return dot(25 + t * 350, 255 - f(t) * 215, c, 10)
  })
}
const infected = (t) => 0.6 * Math.exp(-(((t - 0.45) / 0.16) ** 2))
const recovered = (t) => 0.95 / (1 + Math.exp(-(t - 0.55) * 11))
const susceptible = (t) => Math.max(0.02, 1 - infected(t) - recovered(t))
const chain = (start) => Array.from({ length: 11 }, (_, k) => [start + k, start + k + 1])
const sir = {
  shapes: pad([...curve(susceptible, 'node'), ...curve(infected, 'case'), ...curve(recovered, 'vaccinated')]),
  edges: { ...link(chain(0), 'node', 3), ...link(chain(12), 'case', 3), ...link(chain(24), 'vaccinated', 3) },
  overlays: [text(110, 295, '● Susceptible', { c: 'node' }), text(200, 295, '● Infected', { c: 'case' }), text(290, 295, '● Recovered', { c: 'vaccinated' })],
}

function people(red) {
  return Array.from({ length: 50 }, (_, i) => dot(65 + (i % 10) * 30, 45 + Math.floor(i / 10) * 30, i < red ? 'case' : 'node', 16, i < red ? 1 : 0.18))
}
const result = {
  shapes: people(3),
  overlays: [text(200, 232, '94%', { c: 'text', size: 50, weight: 800 }), text(200, 262, 'fewer infections than no vaccination')],
}
const messy = {
  shapes: people(8),
  overlays: [text(200, 232, '85%', { c: 'text', size: 50, weight: 800 }), text(200, 262, 'fewer, with half of the contacts missing')],
}

const SPEED = 300 / 326.83
const speed = {
  shapes: pad([barH(50, 120, 17.97 * SPEED, 30, 'vaccinated'), barH(50, 205, 326.83 * SPEED, 30, 'node', 0.55)]),
  overlays: [
    text(50, 92, '2-approximation', { anchor: 'start', c: 'text', weight: 700 }),
    text(80, 126, '17.97 ms', { anchor: 'start', c: 'vaccinated', size: 18, weight: 800 }),
    text(50, 177, 'Greedy with heap', { anchor: 'start', c: 'text', weight: 700 }),
    text(340, 211, '326.83 ms', { anchor: 'end', c: 'text', size: 18, weight: 800 }),
    text(200, 275, 'one million contacts'),
  ],
}

const ARCH = [65, 200, 335]
const architecture = {
  shapes: pad([...ARCH.map((x) => box(x, 150, 112, 88, 'glass', 0.22)), dot(132.5, 150, 'accent', 8), dot(267.5, 150, 'accent', 8)]),
  edges: link([[0, 1], [1, 2]], 'edge', 2),
  overlays: [
    text(65, 146, 'React', { c: 'text', size: 15, weight: 700 }),
    text(65, 166, 'draws and asks', { size: 11, c: 'faint' }),
    text(200, 146, 'REST API', { c: 'text', size: 15, weight: 700 }),
    text(200, 166, 'Spring Boot', { size: 11, c: 'faint' }),
    text(335, 146, 'Java engine', { c: 'text', size: 15, weight: 700 }),
    text(335, 166, 'every algorithm', { size: 11, c: 'faint' }),
  ],
}

const closing = {
  shapes: pad(rings(10, 20, 34)),
  overlays: [ring(72, 'ring1'), ring(128, 'ring2')],
}

export const SCENES = [
  title, model, bfs, cover, matching, branch, heap, trap, select, clusters,
  measured, hashing, bloom, sir, result, messy, speed, architecture, closing,
]

/** Every link that appears in any scene, so each can fade in and out. */
export const ALL_EDGES = [...new Set(SCENES.flatMap((s) => Object.keys(s.edges ?? {})))].map((k) => {
  const [a, b] = k.split('-').map(Number)
  return { key: k, a, b }
})

/**
 * Each scene holds still for the middle of its scroll stretch and morphs in
 * the rest, eased so shapes speed up and settle rather than move at one pace.
 */
export function split(at) {
  const last = SCENES.length - 1
  const i = Math.max(0, Math.min(last, Math.floor(at)))
  const f = Math.max(0, Math.min(1, (at - i - 0.18) / 0.64))
  const t = i === last ? 0 : f * f * (3 - 2 * f)
  return { i, j: Math.min(last, i + 1), t }
}
