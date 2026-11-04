import { useAutoAnimate } from '@formkit/auto-animate/react'
import { FileCheck2, FileUp, Upload } from 'lucide-react'
import { useRef, useState } from 'react'
import { cn } from '../../lib/cn.js'
import { Button } from '../ui/Button.jsx'
import { GlassPanel, PanelTitle } from '../ui/GlassPanel.jsx'

const SLOTS = [
  { part: 'people', label: 'people.csv', hint: 'person_id, name, age, role, vaccinated', required: true },
  { part: 'contacts', label: 'contacts.csv', hint: 'person_a, person_b, date, duration_minutes', required: true },
  { part: 'checkins', label: 'checkins.csv', hint: 'Optional: venue visits', required: false },
  { part: 'cases', label: 'cases.csv', hint: 'Optional: confirmed cases', required: false },
]

/** Guess which slot a dropped file belongs to from its name. */
function slotFor(file) {
  const name = file.name.toLowerCase()
  return SLOTS.find((s) => name.includes(s.part.replace(/s$/, '')))?.part
}

export function UploadPanel({ onUpload, busy, delay }) {
  const [files, setFiles] = useState({})
  const [name, setName] = useState('')
  const [dragging, setDragging] = useState(false)
  const [listRef] = useAutoAnimate()
  const inputs = useRef({})

  const accept = (list) => {
    const next = { ...files }
    for (const file of list) {
      const slot = slotFor(file)
      if (slot) next[slot] = file
    }
    setFiles(next)
  }

  const ready = files.people && files.contacts

  return (
    <GlassPanel delay={delay} className="flex h-full flex-col">
      <PanelTitle icon={FileUp} title="Upload your files" subtitle="CSV files from your contact tracing team" />
      <div
        onDragOver={(event) => {
          event.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault()
          setDragging(false)
          accept(event.dataTransfer.files)
        }}
        className={cn(
          'rounded-2xl border border-dashed p-3 transition',
          dragging ? 'border-accent bg-accent-soft' : 'border-[var(--glass-border)]',
        )}
      >
        <p className="mb-2 flex items-center gap-2 text-xs text-muted">
          <Upload className="size-4" /> Drop all files here, or pick each one.
        </p>
        <ul ref={listRef} className="flex flex-col gap-1.5">
          {SLOTS.map((slot) => {
            const file = files[slot.part]
            return (
              <li key={slot.part}>
                <button
                  type="button"
                  onClick={() => inputs.current[slot.part]?.click()}
                  className="inset flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left transition hover:border-accent/40"
                >
                  {file ? (
                    <FileCheck2 className="size-4 shrink-0 text-vaccinated" />
                  ) : (
                    <span className={cn('size-2 shrink-0 rounded-full', slot.required ? 'bg-accent' : 'bg-[var(--text-faint)]')} />
                  )}
                  <span className="min-w-0 flex-1">
                    <span className="block text-xs font-semibold">
                      {slot.label}
                      {slot.required && <span className="text-accent"> *</span>}
                    </span>
                    <span className="block truncate text-xs text-muted">{file ? file.name : slot.hint}</span>
                  </span>
                </button>
                <input
                  ref={(el) => (inputs.current[slot.part] = el)}
                  type="file"
                  accept=".csv,text/csv"
                  className="hidden"
                  aria-label={slot.label}
                  onChange={(event) => {
                    const file = event.target.files?.[0]
                    if (file) setFiles((f) => ({ ...f, [slot.part]: file }))
                    event.target.value = ''
                  }}
                />
              </li>
            )
          })}
        </ul>
      </div>
      <input
        value={name}
        onChange={(event) => setName(event.target.value)}
        placeholder="Name this outbreak (optional)"
        aria-label="Network name"
        className="inset mt-3 w-full rounded-xl px-3 py-2 text-xs outline-none placeholder:text-faint"
      />
      <div className="mt-auto pt-3">
        <Button variant="primary" className="w-full" disabled={!ready} loading={busy} onClick={() => onUpload(files, name)}>
          {ready ? 'Upload and check' : 'Add people.csv and contacts.csv'}
        </Button>
      </div>
    </GlassPanel>
  )
}
