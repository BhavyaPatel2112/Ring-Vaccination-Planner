import { AnimatePresence, motion } from 'motion/react'
import { ServerOff } from 'lucide-react'
import { Outlet, useLocation } from 'react-router'
import { useWorkspace } from '../../state/context.js'
import { Header } from './Header.jsx'
import { SceneBackdrop } from './SceneBackdrop.jsx'

export function AppShell() {
  const location = useLocation()
  const { backendDown } = useWorkspace()
  return (
    <div className="relative flex min-h-full flex-col pb-6">
      <SceneBackdrop />
      <Header />
      {backendDown && (
        <div
          className="mx-auto mt-3 flex max-w-xl items-center gap-3 rounded-full border border-danger/40 bg-danger/10 px-4 py-2 text-sm"
          role="alert"
        >
          <ServerOff className="size-4 shrink-0 text-danger" />
          <span>
            The backend is not running. Start it with <code className="font-semibold">./mvnw spring-boot:run</code> in
            the backend folder, then refresh.
          </span>
        </div>
      )}
      <main className="relative z-10 flex flex-1 flex-col">
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={location.pathname}
            className="flex flex-1 flex-col"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </main>
    </div>
  )
}
