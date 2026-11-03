import { AnimatePresence, motion } from 'motion/react'
import { ServerOff } from 'lucide-react'
import { Outlet, useLocation } from 'react-router'
import { useWorkspace } from '../../state/context.js'
import { SceneBackdrop } from './SceneBackdrop.jsx'
import { ThemeToggle } from './ThemeToggle.jsx'
import { TopBar } from './TopBar.jsx'
import { ViewSwitch } from './ViewSwitch.jsx'
import { WorkflowRail } from './WorkflowRail.jsx'

export function AppShell() {
  const location = useLocation()
  const { backendDown } = useWorkspace()
  return (
    <div className="relative flex min-h-full flex-col pb-24">
      <SceneBackdrop />
      <TopBar />
      <ViewSwitch />
      <AnimatePresence>
        {backendDown && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="mx-auto mt-3 flex max-w-xl items-center gap-3 rounded-full border border-danger/40 bg-danger/10 px-4 py-2 text-xs"
            role="alert"
          >
            <ServerOff className="size-4 text-danger" />
            <span>
              The backend is not running. Start it with <code className="font-semibold">./mvnw spring-boot:run</code> in
              the backend folder, then refresh.
            </span>
          </motion.div>
        )}
      </AnimatePresence>
      <main className="relative z-10 flex flex-1 flex-col">
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            className="flex flex-1 flex-col"
            initial={{ opacity: 0, scale: 0.985 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 1.01 }}
            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </main>
      <WorkflowRail />
      <ThemeToggle />
    </div>
  )
}
