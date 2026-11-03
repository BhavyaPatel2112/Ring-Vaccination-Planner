import '@fontsource-variable/manrope'
import { MotionConfig } from 'motion/react'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { Toaster } from 'sonner'
import App from './App.jsx'
import './index.css'
import { WorkspaceProvider } from './state/workspace.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <MotionConfig reducedMotion="user">
        <WorkspaceProvider>
          <App />
          <Toaster
            position="bottom-right"
            offset={{ bottom: 88, right: 20 }}
            toastOptions={{
              unstyled: true,
              classNames: {
                toast: 'glass glass-strong flex w-[340px] items-start gap-3 rounded-2xl p-4 font-sans text-sm text-text',
                title: 'font-semibold',
                description: 'mt-0.5 text-xs leading-relaxed text-muted',
                icon: 'mt-0.5',
                success: '[&_[data-icon]]:text-vaccinated',
                error: '[&_[data-icon]]:text-danger',
              },
            }}
          />
        </WorkspaceProvider>
      </MotionConfig>
    </BrowserRouter>
  </StrictMode>,
)
