import { apiGet, apiPost } from './client.js'

// One function per backend endpoint. Every algorithm runs in Java; these only move data.

export const health = () => apiGet('/health')

export const listSamples = () => apiGet('/samples')
export const loadSample = (sampleId) => apiPost(`/networks/samples/${encodeURIComponent(sampleId)}`)
export const generateNetwork = (settings) => apiPost('/networks/generate', settings)
export const listNetworks = () => apiGet('/networks')
export const getSummary = (networkId) => apiGet(`/networks/${networkId}/summary`)
export const getNetworkView = (networkId) => apiGet(`/networks/${networkId}`)

/** files: { people, contacts, checkins?, cases? } as File objects. */
export function uploadNetwork(files, name) {
  const form = new FormData()
  for (const [part, file] of Object.entries(files)) {
    if (file) form.append(part, file)
  }
  if (name) form.append('name', name)
  return apiPost('/networks', form)
}

export const buildRing = (networkId, caseIds, depth, settings = {}) =>
  apiPost(`/networks/${networkId}/ring`, { caseIds, depth, ...settings })

export const makePlan = (networkId, request) => apiPost(`/networks/${networkId}/plan`, request)

export const compareMethods = (networkId, caseIds, depth, settings = {}) =>
  apiPost(`/networks/${networkId}/compare`, { caseIds, depth, ...settings })

export const compareAllLinks = (networkId) => apiPost(`/networks/${networkId}/compare-all`)

export const getMethods = () => apiGet('/methods')

// Outbreak clusters (disjoint-set union) and live contact reports.
export const getClusters = (networkId, caseIds = []) =>
  apiGet(`/networks/${networkId}/clusters${caseIds.length ? `?caseIds=${caseIds.map(encodeURIComponent).join(',')}` : ''}`)
export const addContact = (networkId, contact) => apiPost(`/networks/${networkId}/contacts`, contact)

// Vaccinate-by deadlines and the monitoring list.
export const followUp = (networkId, request) => apiPost(`/networks/${networkId}/follow-up`, request)

// Bloom filter features: exposure self-check, double-dose guard, tuning.
export const checkExposure = (request) => apiPost('/exposure/check', request)
export const checkRegistry = (networkId, personIds) => apiPost('/registry/check', { networkId, personIds })
export const tuneBloom = (settings) => apiPost('/bloom/config', settings)
export const bloomStats = (networkId) => apiGet(`/bloom/stats?networkId=${encodeURIComponent(networkId)}`)

// Outbreak simulation, and its robustness to missing contacts.
export const simulate = (networkId, request) => apiPost(`/networks/${networkId}/simulate`, request)
export const simulateRobustness = (networkId, request) => apiPost(`/networks/${networkId}/simulate/robustness`, request)

// "How it works": recorded steps of the 2-approximation, and a tiny Bloom filter to draw.
export const approxWalkthrough = (networkId, caseIds, depth, settings = {}) =>
  apiPost(`/networks/${networkId}/walkthrough/approx`, { caseIds, depth, ...settings })
export const bloomDemo = (request) => apiPost('/bloom/demo', request)

// Benchmarks: saved results from docs/benchmarks, and a quick live run.
export const getBenchmarks = () => apiGet('/benchmarks')
export const runBenchmark = (request) => apiPost('/benchmarks/run', request)
