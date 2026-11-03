// Small wrapper around fetch so every screen handles backend errors the same way.
// The backend returns errors as { error, message, details }.

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Request failed with status ${status}`)
    this.status = status
    this.code = body?.error
    this.details = body?.details || []
  }
}

/** Thrown when the backend is not running at all. */
export class BackendDownError extends Error {
  constructor() {
    super('The backend is not reachable. Start it with ./mvnw spring-boot:run in the backend folder.')
    this.code = 'BACKEND_DOWN'
    this.details = []
  }
}

export function apiGet(path) {
  return request(path, { method: 'GET' })
}

export function apiPost(path, body) {
  const isForm = body instanceof FormData
  return request(path, {
    method: 'POST',
    headers: isForm || body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: isForm ? body : body === undefined ? undefined : JSON.stringify(body),
  })
}

async function request(path, options) {
  let response
  try {
    response = await fetch(`/api${path}`, options)
  } catch {
    throw new BackendDownError()
  }
  // The Vite proxy answers 502/504 when the backend is down.
  if ((response.status === 502 || response.status === 504) && !response.headers.get('content-type')?.includes('json')) {
    throw new BackendDownError()
  }
  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }
  if (!response.ok) {
    throw new ApiError(response.status, data)
  }
  return data
}
