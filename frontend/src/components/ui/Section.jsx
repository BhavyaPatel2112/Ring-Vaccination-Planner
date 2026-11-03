import * as Accordion from '@radix-ui/react-accordion'
import { ChevronDown } from 'lucide-react'
import { cn } from '../../lib/cn.js'

/** A group of expandable sections, each with a title and a count. */
export function SectionGroup({ children, defaultOpen, className }) {
  return (
    <Accordion.Root type="multiple" defaultValue={defaultOpen} className={cn('flex flex-col gap-2', className)}>
      {children}
    </Accordion.Root>
  )
}

export function Section({ value, title, count, dot, children, action }) {
  return (
    <Accordion.Item value={value} className="glass overflow-hidden rounded-[18px]">
      <Accordion.Header className="flex items-center">
        <Accordion.Trigger className="group flex flex-1 items-center gap-2 px-4 py-3 text-left text-[13px] font-semibold">
          {dot && <span className="size-2 rounded-full" style={{ background: dot }} />}
          <span>{title}</span>
          {count != null && <span className="font-semibold text-accent">({count})</span>}
          <ChevronDown className="ml-auto size-4 text-muted transition-transform duration-300 group-data-[state=open]:rotate-180" />
        </Accordion.Trigger>
        {action && <div className="pr-3">{action}</div>}
      </Accordion.Header>
      <Accordion.Content className="overflow-hidden px-3 pb-3 data-[state=closed]:animate-none">{children}</Accordion.Content>
    </Accordion.Item>
  )
}
