/** Same key for a link whichever way round its two people are given. */
export const linkKey = (a, b) => (a < b ? `${a}~${b}` : `${b}~${a}`)
