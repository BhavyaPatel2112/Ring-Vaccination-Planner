import { useEffect, useState } from 'react'

/** Reads design tokens for Chart.js, which does not follow CSS variables by itself. */
export function readTokens(names) {
  const css = getComputedStyle(document.documentElement)
  const tokens = {}
  for (const name of names) tokens[name] = css.getPropertyValue(`--${name}`).trim()
  return tokens
}

const BASE = ['text', 'text-muted', 'text-faint', 'glass-border', 'accent', 'planned', 'case', 'ring1', 'ring2', 'vaccinated', 'excluded', 'warning']

/** Chart colours that update when the theme changes (the header fires `ringvax-theme`). */
export function useChartColors() {
  const [colors, setColors] = useState(() => readTokens(BASE))
  useEffect(() => {
    const update = () => setColors(readTokens(BASE))
    window.addEventListener('ringvax-theme', update)
    // The header applies the saved theme in its own effect, which can run before this
    // listener exists, so read the colours once more after mounting.
    const frame = requestAnimationFrame(update)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('ringvax-theme', update)
    }
  }, [])
  return colors
}

/** Shared axis and legend styling for line and bar charts. */
export function chartOptions(colors, extra = {}) {
  const axis = {
    ticks: { color: colors['text-muted'], font: { family: 'Manrope Variable', size: 12 } },
    grid: { color: colors['glass-border'] },
    border: { color: colors['glass-border'] },
  }
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 500 },
    plugins: {
      legend: { labels: { color: colors['text-muted'], boxWidth: 10, boxHeight: 10, font: { family: 'Manrope Variable', size: 12 } } },
      tooltip: { titleFont: { family: 'Manrope Variable' }, bodyFont: { family: 'Manrope Variable' } },
      ...extra.plugins,
    },
    scales: {
      x: { ...axis, ...extra.x },
      y: { ...axis, ...extra.y },
    },
  }
}
