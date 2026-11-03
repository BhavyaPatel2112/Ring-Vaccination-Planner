import { ArrowLeft } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router'
import { Button } from '../components/ui/Button.jsx'
import { GlassPanel } from '../components/ui/GlassPanel.jsx'
import { TOOLS } from '../lib/tools.js'

/** Stand-in for screens that are not built yet, in the same style as the rest. */
export function PlaceholderPage() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const tool = TOOLS.find((t) => t.path === pathname)
  const Icon = tool?.icon
  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 items-center px-4 pt-10">
      <GlassPanel grabber className="w-full p-8 text-center">
        {Icon && (
          <span className="inset mx-auto grid size-14 place-items-center rounded-full">
            <Icon className="size-6 text-accent" />
          </span>
        )}
        <h1 className="mt-4 text-2xl font-medium tracking-tight">{tool?.title ?? 'Not found'}</h1>
        <p className="mt-2 text-sm text-muted">{tool?.description ?? 'There is no screen at this address.'}</p>
        <p className="mt-4 text-xs text-faint">
          {tool ? 'This screen is coming in the next build. It will use the network and cases you have already loaded.' : ''}
        </p>
        <Button className="mt-6" icon={ArrowLeft} onClick={() => navigate('/network')}>
          Back to the network
        </Button>
      </GlassPanel>
    </div>
  )
}
