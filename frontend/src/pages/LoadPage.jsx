import { motion } from 'motion/react'
import { Database } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { listSamples } from '../api/endpoints.js'
import { GeneratePanel } from '../components/load/GeneratePanel.jsx'
import { LoadReport } from '../components/load/LoadReport.jsx'
import { SampleCard } from '../components/load/SampleCard.jsx'
import { UploadPanel } from '../components/load/UploadPanel.jsx'
import { Empty } from '../components/ui/Notice.jsx'
import { useWorkspace } from '../state/context.js'

/** Three ways to start: a sample network, your own CSV files, or a synthetic town. */
export function LoadPage() {
  const { network, loadSample, upload, generate, busy } = useWorkspace()
  const [samples, setSamples] = useState(null)
  const [opening, setOpening] = useState(null)
  const [showReport, setShowReport] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    listSamples().then(setSamples).catch(() => setSamples([]))
  }, [])

  const afterLoad = (promise) =>
    promise
      .then(() => setShowReport(true))
      .catch(() => {})
      .finally(() => setOpening(null))

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 pt-6">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="text-center">
        <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">Start with a contact network</h1>
        <p className="mx-auto mt-2 max-w-lg text-[15px] text-muted">
          Open a sample, upload your contact tracing files, or generate a synthetic town.
        </p>
      </motion.div>

      {showReport && network && <LoadReport network={network} onOpen={() => navigate('/network')} />}

      <section aria-label="Sample networks">
        <h2 className="mb-2 px-1 text-sm font-semibold text-muted">Samples</h2>
        {samples && samples.length === 0 ? (
          <div className="glass rounded-[22px]">
            <Empty icon={Database} title="No samples found">
              Start the backend from the backend folder so it can find data/sample.
            </Empty>
          </div>
        ) : (
          <div className="scroll-thin -mx-4 flex snap-x gap-4 overflow-x-auto px-4 pt-1 pb-4">
            {(samples ?? []).map((sample, i) => (
              <SampleCard
                key={sample.id}
                sample={sample}
                index={i}
                loading={opening === sample.id}
                onOpen={() => {
                  setOpening(sample.id)
                  afterLoad(loadSample(sample.id))
                }}
              />
            ))}
          </div>
        )}
      </section>

      <h2 className="-mb-3 px-1 text-sm font-semibold text-muted">Your own data</h2>
      <div className="grid gap-4 md:grid-cols-2">
        <UploadPanel delay={0.1} busy={busy === 'load' && !opening} onUpload={(files, name) => afterLoad(upload(files, name))} />
        <GeneratePanel delay={0.15} busy={busy === 'load' && !opening} onGenerate={(settings) => afterLoad(generate(settings))} />
      </div>
    </div>
  )
}
