import { Navigate, Route, Routes } from 'react-router'
import { AppShell } from './components/shell/AppShell.jsx'
import { TOOLS } from './lib/tools.js'
import { BenchmarksPage } from './pages/BenchmarksPage.jsx'
import { BloomPage } from './pages/BloomPage.jsx'
import { ClustersPage } from './pages/ClustersPage.jsx'
import { ComparePage } from './pages/ComparePage.jsx'
import { ConsolePage } from './pages/ConsolePage.jsx'
import { ExposurePage } from './pages/ExposurePage.jsx'
import { FollowUpPage } from './pages/FollowUpPage.jsx'
import { HowItWorksPage } from './pages/HowItWorksPage.jsx'
import { LoadPage } from './pages/LoadPage.jsx'
import { PlaceholderPage } from './pages/PlaceholderPage.jsx'
import { PlanDetailsPage } from './pages/PlanDetailsPage.jsx'
import { SimulationPage } from './pages/SimulationPage.jsx'
import { useWorkspace } from './state/context.js'

function Home() {
  const { network, restoring } = useWorkspace()
  if (restoring) return null
  return <Navigate to={network ? '/network' : '/load'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Home />} />
        <Route path="load" element={<LoadPage />} />
        <Route path="network" element={<ConsolePage />} />
        <Route path="plan" element={<PlanDetailsPage />} />
        <Route path="compare" element={<ComparePage />} />
        <Route path="clusters" element={<ClustersPage />} />
        <Route path="follow-up" element={<FollowUpPage />} />
        <Route path="exposure" element={<ExposurePage />} />
        <Route path="bloom" element={<BloomPage />} />
        <Route path="simulation" element={<SimulationPage />} />
        <Route path="how-it-works" element={<HowItWorksPage />} />
        <Route path="benchmarks" element={<BenchmarksPage />} />
        {TOOLS.filter((t) => !t.ready).map((t) => (
          <Route key={t.path} path={t.path.slice(1)} element={<PlaceholderPage />} />
        ))}
        <Route path="*" element={<PlaceholderPage />} />
      </Route>
    </Routes>
  )
}
