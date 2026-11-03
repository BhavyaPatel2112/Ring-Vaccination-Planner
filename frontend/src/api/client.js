// Small wrapper around fetch so every page handles backend errors the same way.
// The backend returns errors as { error, message, details }.

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Request failed with status ${status}`)
    this.status = status
    this.code = body?.error
    this.details = body?.details || []
  }
}

export async function apiGet(path) {
  return request(path, { method: 'GET' })
}

export async function apiPost(path, body) {
  const isForm = body instanceof FormData
  return request(path, {
    method: 'POST',
    headers: isForm ? undefined : { 'Content-Type': 'application/json' },
    body: isForm ? body : JSON.stringify(body),
  })
}

async function request(path, options) {
  const response = await fetch(`/api${path}`, options)
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) {
    throw new ApiError(response.status, data)
  }
  return data
}
