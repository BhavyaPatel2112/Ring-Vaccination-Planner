import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { toast } from 'sonner'
import * as api from '../api/endpoints.js'
import { WorkspaceContext } from './context.js'

/**
 * Shared state for the current outbreak: the loaded network, the confirmed
 * cases, the rings and the plan. Every screen reads from here so they stay in step.
 */
const STORAGE_KEY = 'ringvax.networkId'

function remember(networkId) {
  try {
    if (networkId) localStorage.setItem(STORAGE_KEY, networkId)
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Storage can be blocked (private mode); the app still works without it.
  }
}

function recall() {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export function WorkspaceProvider({ children }) {
  const [network, setNetwork] = useState(null)
  const [view, setView] = useState(null)
  const [cases, setCases] = useState([])
  const [depth, setDepthValue] = useState(2)
  const [ring, setRing] = useState(null)
  const [plan, setPlan] = useState(null)
  const [method, setMethod] = useState('APPROX')
  const [busy, setBusy] = useState(null)
  const [backendDown, setBackendDown] = useState(false)
  const [restoring, setRestoring] = useState(true)
  const ringRequest = useRef(0)

  const report = useCallback((error, title) => {
    if (error?.code === 'BACKEND_DOWN') setBackendDown(true)
    toast.error(title, { description: error?.message })
  }, [])

  const open = useCallback(async (summary) => {
    const networkView = await api.getNetworkView(summary.networkId)
    setNetwork(summary)
    setView(networkView)
    setCases(summary.caseIds)
    setRing(null)
    setPlan(null)
    setBackendDown(false)
    remember(summary.networkId)
    return summary
  }, [])

  // Reopen the last network after a page refresh, if the backend still has it.
  useEffect(() => {
    const saved = recall()
    const check = api.health().then(() => setBackendDown(false))
    const restore = saved ? api.getSummary(saved).then(open) : Promise.resolve()
    Promise.allSettled([check, restore]).then(([health, restored]) => {
      if (health.status === 'rejected') setBackendDown(true)
      if (restored.status === 'rejected') remember(null)
      setRestoring(false)
    })
  }, [open])

  const run = useCallback(
    async (label, task, failTitle) => {
      setBusy(label)
      try {
        return await task()
      } catch (error) {
        report(error, failTitle)
        throw error
      } finally {
        setBusy(null)
      }
    },
    [report],
  )

  const loadSample = useCallback(
    (sampleId) => run('load', async () => open(await api.loadSample(sampleId)), 'Could not load the sample'),
    [run, open],
  )

  const upload = useCallback(
    (files, name) => run('load', async () => open(await api.uploadNetwork(files, name)), 'Upload rejected'),
    [run, open],
  )

  const generate = useCallback(
    (settings) => run('load', async () => open(await api.generateNetwork(settings)), 'Could not generate the network'),
    [run, open],
  )

  const switchNetwork = useCallback(
    (networkId) => run('load', async () => open(await api.getSummary(networkId)), 'Could not open that network'),
    [run, open],
  )

  // Rings are cheap, so they are rebuilt whenever the cases or the depth change.
  useEffect(() => {
    if (!network || cases.length === 0) return
    const id = ++ringRequest.current
    api
      .buildRing(network.networkId, cases, depth)
      .then((result) => {
        if (id === ringRequest.current) setRing(result)
      })
      .catch((error) => {
        if (id === ringRequest.current) report(error, 'Could not build the rings')
      })
  }, [network, cases, depth, report])

  // Changing the cases or the depth makes an existing plan out of date.
  const toggleCase = useCallback((personId) => {
    setPlan(null)
    setCases((current) => (current.includes(personId) ? current.filter((id) => id !== personId) : [...current, personId]))
  }, [])

  const setDepth = useCallback((value) => {
    setPlan(null)
    setDepthValue(value)
  }, [])

  const createPlan = useCallback(
    (options = {}) =>
      run(
        'plan',
        async () => {
          const result = await api.makePlan(network.networkId, { caseIds: cases, depth, method, ...options })
          setPlan(result)
          toast.success(`Plan ready: ${result.doses} doses`, { description: result.guarantee })
          return result
        },
        'Could not make the plan',
      ),
    [run, network, cases, depth, method],
  )

  const people = useMemo(() => {
    const byId = new Map()
    for (const node of view?.nodes ?? []) byId.set(node.id, node)
    return byId
  }, [view])

  const value = {
    network,
    view,
    people,
    cases,
    depth,
    // A ring left over from before the last case was removed is not shown.
    ring: cases.length > 0 ? ring : null,
    plan,
    method,
    busy,
    backendDown,
    restoring,
    setDepth,
    setMethod,
    toggleCase,
    loadSample,
    upload,
    generate,
    switchNetwork,
    createPlan,
  }
  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
}
