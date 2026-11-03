const number = new Intl.NumberFormat('en-IN')

export function formatNumber(value) {
  return value == null ? '–' : number.format(value)
}

export function plural(count, one, many = `${one}s`) {
  return `${formatNumber(count)} ${count === 1 ? one : many}`
}

/** "P003" -> "P3"-style initials for avatars: first letters of the name, or the id. */
export function initials(name, id) {
  const parts = (name || '').trim().split(/\s+/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (id || '?').slice(0, 2).toUpperCase()
}
