/**
 * Cytoscape style sheet built from the CSS colour tokens, so the network
 * follows the light and dark themes. Later rules win over earlier ones.
 */
export function buildNetworkStyle() {
  const css = getComputedStyle(document.documentElement)
  const v = (name) => css.getPropertyValue(name).trim()

  return [
    {
      selector: 'node',
      style: {
        width: 'mapData(degree, 0, 16, 7, 20)',
        height: 'mapData(degree, 0, 16, 7, 20)',
        'background-color': v('--node'),
        'background-opacity': 0.9,
        'border-width': 0,
        label: '',
        'font-family': 'Manrope Variable, system-ui, sans-serif',
        'font-size': 9,
        'font-weight': 600,
        color: v('--text'),
        'text-valign': 'bottom',
        'text-margin-y': 5,
        'text-background-color': v('--scene-top'),
        'text-background-opacity': 0.55,
        'text-background-padding': 2,
        'text-background-shape': 'roundrectangle',
        'underlay-color': v('--accent'),
        'underlay-opacity': 0,
        'underlay-padding': 6,
        'underlay-shape': 'ellipse',
        'transition-property': 'background-color, border-color, border-width, underlay-opacity, opacity',
        'transition-duration': 450,
      },
    },
    {
      selector: 'edge',
      style: {
        width: 1,
        // Cytoscape ignores alpha in colours, so transparency goes in line-opacity.
        'line-color': v('--edge-solid'),
        'line-opacity': 0.2,
        'curve-style': 'haystack',
        'haystack-radius': 0,
        'transition-property': 'line-color, line-opacity, opacity, width',
        'transition-duration': 450,
      },
    },
    { selector: 'node.ring3', style: { 'background-color': v('--ring2'), 'background-opacity': 0.55 } },
    { selector: 'node.ring2', style: { 'background-color': v('--ring2') } },
    { selector: 'node.ring1', style: { 'background-color': v('--ring1') } },
    { selector: 'node.vaccinated', style: { 'background-color': v('--vaccinated') } },
    {
      selector: 'node.excluded',
      style: {
        'background-color': v('--excluded'),
        'background-opacity': 0.45,
        'border-width': 1.5,
        'border-style': 'dashed',
        'border-color': v('--excluded'),
      },
    },
    {
      selector: 'node.planned',
      style: {
        'border-width': 2.5,
        'border-style': 'solid',
        'border-color': v('--planned'),
        'underlay-color': v('--planned'),
        'underlay-opacity': 0.22,
        'underlay-padding': 5,
      },
    },
    {
      selector: 'node.case',
      style: {
        'background-color': v('--case'),
        'background-opacity': 1,
        width: 20,
        height: 20,
        label: 'data(label)',
        'underlay-color': v('--case'),
        'underlay-opacity': 0.28,
        'underlay-padding': 10,
      },
    },
    { selector: 'node.dim', style: { opacity: 0.22 } },
    { selector: 'edge.dim', style: { opacity: 0.18 } },
    { selector: 'edge.caselink', style: { 'line-color': v('--ring1'), 'line-opacity': 0.7, width: 1.4 } },
    { selector: 'edge.target', style: { 'line-color': v('--ring2'), 'line-opacity': 0.6, width: 1.3 } },
    { selector: 'edge.unprotected', style: { 'line-color': v('--case'), 'line-opacity': 1, width: 2.2 } },
    { selector: 'node.hovered', style: { label: 'data(label)', 'underlay-opacity': 0.3, 'underlay-color': v('--accent') } },
    {
      selector: 'node.selected',
      style: {
        label: 'data(label)',
        'border-width': 3,
        'border-style': 'solid',
        'border-color': v('--accent'),
        'underlay-color': v('--accent'),
        'underlay-opacity': 0.3,
        'underlay-padding': 9,
      },
    },
  ]
}

/** Stable starting position for a person, so the layout looks the same on every visit. */
export function seededPosition(id, index, total) {
  let hash = 2166136261
  for (let i = 0; i < id.length; i++) {
    hash ^= id.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  const angle = ((hash >>> 0) % 3600) / 3600 * Math.PI * 2
  const radius = 40 + Math.sqrt(total) * 18 * ((index % 97) / 97 + 0.2)
  return { x: Math.cos(angle) * radius, y: Math.sin(angle) * radius }
}
