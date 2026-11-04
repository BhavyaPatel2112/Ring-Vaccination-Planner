import cytoscape from 'cytoscape'
import fcose from 'cytoscape-fcose'
import { AnimatePresence, motion } from 'motion/react'
import { Expand, Maximize, Minus, Plus, Shrink } from 'lucide-react'
import { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react'
import { IconButton } from '../ui/Button.jsx'
import { buildNetworkStyle, seededPosition } from './networkStyle.js'
import { describePerson } from './personStatus.js'

cytoscape.use(fcose)

const linkKey = (a, b) => (a < b ? `${a}~${b}` : `${b}~${a}`)

/**
 * The contact network, drawn with Cytoscape. It only displays results from the
 * backend: ring levels, plan and unprotected links all come from the Java API.
 *
 * The ref exposes `pointOf(personId)` (screen position of a person, for the
 * callout lines) and `fit()`.
 */
export const NetworkCanvas = forwardRef(function NetworkCanvas(
  { view, ring, plan, cases, selected, hovered, onSelect, onHover, expanded, onToggleExpand },
  ref,
) {
  const container = useRef(null)
  const cyRef = useRef(null)
  const [tip, setTip] = useState(null)

  // Create the graph once per network.
  useEffect(() => {
    if (!view || !container.current) return
    const total = view.nodes.length
    const elements = [
      ...view.nodes.map((node, i) => ({
        group: 'nodes',
        data: { id: node.id, label: node.name ? `${node.name.split(' ')[0]} · ${node.id}` : node.id, degree: node.contacts },
        position: seededPosition(node.id, i, total),
      })),
      ...view.links.map((link) => ({
        group: 'edges',
        data: { id: linkKey(link.source, link.target), source: link.source, target: link.target },
      })),
    ]
    const cy = cytoscape({
      container: container.current,
      elements,
      style: buildNetworkStyle(),
      minZoom: 0.15,
      maxZoom: 4,
      boxSelectionEnabled: false,
      autoungrabify: false,
      pixelRatio: 'auto',
    })
    cyRef.current = cy
    const big = total > 600
    cy.layout({
      name: 'fcose',
      quality: big ? 'draft' : 'proof',
      randomize: true,
      // Computed in one go, so the finished picture appears straight away.
      animate: false,
      nodeRepulsion: 9000,
      idealEdgeLength: big ? 30 : 70,
      edgeElasticity: 0.35,
      nodeSeparation: 80,
      gravity: 0.25,
      numIter: 4000,
      padding: 36,
      fit: true,
    }).run()

    const restyle = () => cy.style(buildNetworkStyle())
    window.addEventListener('ringvax-theme', restyle)
    // Keep the drawing sized to its box (panels opening, focus mode, window resizes).
    let fitted = false
    const resize = new ResizeObserver(() => {
      cy.resize()
      if (!fitted && container.current?.clientHeight > 0) {
        fitted = true
        cy.fit(undefined, 36)
      }
    })
    resize.observe(container.current)
    return () => {
      resize.disconnect()
      window.removeEventListener('ringvax-theme', restyle)
      cy.destroy()
      cyRef.current = null
    }
  }, [view])

  // Pointer events are wired separately so the latest callbacks are used.
  useEffect(() => {
    const cy = cyRef.current
    if (!cy) return
    const tap = (event) => onSelect?.(event.target === cy ? null : event.target.id())
    const over = (event) => onHover?.(event.target.id())
    const out = () => onHover?.(null)
    cy.on('tap', tap)
    cy.on('mouseover', 'node', over)
    cy.on('mouseout', 'node', out)
    return () => {
      cy.off('tap', tap)
      cy.off('mouseover', 'node', over)
      cy.off('mouseout', 'node', out)
    }
  }, [view, onSelect, onHover])

  // Recolour people whenever cases, rings or the plan change.
  const classes = useMemo(() => {
    const level = new Map()
    ring?.rings.forEach((members, i) => members.forEach((id) => level.set(id, i + 1)))
    const planned = new Set(plan?.people.map((p) => p.personId))
    const caseSet = new Set(cases)
    const targets = new Set(ring?.targetLinks.map((l) => linkKey(l.a, l.b)))
    const unprotected = new Set(plan?.unprotectedLinks.map((l) => linkKey(l.a, l.b)))
    return { level, planned, caseSet, targets, unprotected }
  }, [ring, plan, cases])

  useEffect(() => {
    const cy = cyRef.current
    if (!cy || !view) return
    const { level, planned, caseSet, targets, unprotected } = classes
    const hasRing = level.size > 0 || caseSet.size > 0
    cy.batch(() => {
      for (const node of view.nodes) {
        const element = cy.getElementById(node.id)
        const l = level.get(node.id)
        const list = []
        if (caseSet.has(node.id)) list.push('case')
        else if (l) list.push(l >= 3 ? 'ring3' : `ring${l}`)
        else if (hasRing && ring) list.push('dim')
        if (node.vaccinated) list.push('vaccinated')
        if (!node.eligible || node.refused) list.push('excluded')
        if (planned.has(node.id)) list.push('planned')
        if (node.id === selected) list.push('selected')
        if (node.id === hovered) list.push('hovered')
        element.classes(list)
      }
      cy.edges().forEach((edge) => {
        const a = edge.data('source')
        const b = edge.data('target')
        const key = linkKey(a, b)
        const list = []
        if (unprotected.has(key)) list.push('unprotected')
        else if (targets.has(key)) list.push('target')
        else if (caseSet.has(a) || caseSet.has(b)) list.push('caselink')
        else if (ring && !(level.has(a) && level.has(b))) list.push('dim')
        edge.classes(list)
      })
    })
  }, [classes, view, ring, selected, hovered])

  // Hover card position follows the node.
  useEffect(() => {
    const cy = cyRef.current
    if (!cy || !hovered) {
      setTip(null)
      return
    }
    const node = cy.getElementById(hovered)
    if (node.empty()) return
    const p = node.renderedPosition()
    const width = container.current?.clientWidth ?? 400
    setTip({ id: hovered, left: Math.min(p.x + 16, width - 240), top: Math.max(p.y - 20, 8) })
  }, [hovered])

  useImperativeHandle(ref, () => ({
    pointOf(id) {
      const cy = cyRef.current
      const box = container.current?.getBoundingClientRect()
      if (!cy || !box) return null
      const node = cy.getElementById(id)
      if (node.empty()) return null
      const p = node.renderedPosition()
      const x = box.left + p.x
      const y = box.top + p.y
      const inside = p.x >= 0 && p.y >= 0 && p.x <= box.width && p.y <= box.height
      return inside ? { x, y } : null
    },
    fit() {
      cyRef.current?.animate({ fit: { padding: 36 } }, { duration: 500 })
    },
    focusOn(id) {
      const cy = cyRef.current
      const node = cy?.getElementById(id)
      if (node && !node.empty()) cy.animate({ center: { eles: node }, zoom: Math.max(cy.zoom(), 1.4) }, { duration: 500 })
    },
  }))

  const zoom = (factor) => {
    const cy = cyRef.current
    if (!cy) return
    const box = container.current.getBoundingClientRect()
    cy.animate({ zoom: { level: cy.zoom() * factor, renderedPosition: { x: box.width / 2, y: box.height / 2 } } }, { duration: 250 })
  }

  const person = tip ? view?.nodes.find((n) => n.id === tip.id) : null

  return (
    <div className="absolute inset-0">
      {/* Cytoscape forces position: relative on its container, so it fills a sized wrapper. */}
      <div ref={container} className="h-full w-full" aria-label="Contact network" role="img" />
      <div className="glass absolute right-3 bottom-3 flex flex-col gap-1 rounded-full p-1">
        <IconButton icon={Plus} label="Zoom in" onClick={() => zoom(1.3)} />
        <IconButton icon={Minus} label="Zoom out" onClick={() => zoom(1 / 1.3)} />
        <IconButton icon={Maximize} label="Fit to screen" onClick={() => cyRef.current?.animate({ fit: { padding: 36 } }, { duration: 500 })} />
        {onToggleExpand && (
          <IconButton icon={expanded ? Shrink : Expand} label={expanded ? 'Show side panels' : 'Expand the map'} onClick={onToggleExpand} />
        )}
      </div>
      <AnimatePresence>
        {person && (
          <motion.div
            key={person.id}
            initial={{ opacity: 0, y: 6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="glass glass-strong pointer-events-none absolute z-10 w-56 rounded-2xl p-3 text-xs leading-relaxed"
            style={{ left: tip.left, top: tip.top }}
          >
            <p className="text-sm font-semibold">{person.name || person.id}</p>
            <p className="text-muted">
              {person.id}
              {person.age != null && ` · ${person.age} yrs`}
              {person.role && ` · ${person.role}`}
            </p>
            <p className="mt-1.5 text-muted">{person.contacts} contacts</p>
            <p className="mt-1 font-semibold">{describePerson(person, classes)}</p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
})
