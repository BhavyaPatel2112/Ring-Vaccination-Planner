import { Contrast } from 'lucide-react'
import { useEffect, useState } from 'react'

const KEY = 'ringvax.theme'

function initialTheme() {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved === 'light' || saved === 'dark') return saved
  } catch {
    // ignore blocked storage
  }
  return window.matchMedia?.('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

/** Round light/dark switch in the bottom-left corner. */
export function ThemeToggle() {
  const [theme, setTheme] = useState(initialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#0b0f17' : '#f4f4f2')
    window.dispatchEvent(new Event('ringvax-theme'))
    try {
      localStorage.setItem(KEY, theme)
    } catch {
      // ignore blocked storage
    }
  }, [theme])

  return (
    <button
      type="button"
      onClick={() => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))}
      aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
      title={theme === 'dark' ? 'Light theme' : 'Dark theme'}
      className="glass glass-strong fixed bottom-20 left-4 z-40 sm:bottom-5 sm:left-5 grid size-12 place-items-center rounded-full transition hover:scale-105"
    >
      <Contrast className="size-5" />
    </button>
  )
}
