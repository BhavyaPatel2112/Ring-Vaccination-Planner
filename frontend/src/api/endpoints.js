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

export const buildRing = (networkId, caseIds, depth) => apiPost(`/networks/${networkId}/ring`, { caseIds, depth })

export const makePlan = (networkId, request) => apiPost(`/networks/${networkId}/plan`, request)

export const compareMethods = (networkId, caseIds, depth) =>
  apiPost(`/networks/${networkId}/compare`, { caseIds, depth })
