import { Navigate, Route, Routes } from 'react-router'
import { AppShell } from './components/shell/AppShell.jsx'
import { TOOLS } from './lib/tools.js'
import { ConsolePage } from './pages/ConsolePage.jsx'
import { LoadPage } from './pages/LoadPage.jsx'
import { PlaceholderPage } from './pages/PlaceholderPage.jsx'
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
        {TOOLS.filter((t) => !t.ready).map((t) => (
          <Route key={t.path} path={t.path.slice(1)} element={<PlaceholderPage />} />
        ))}
        <Route path="*" element={<PlaceholderPage />} />
      </Route>
    </Routes>
  )
}
