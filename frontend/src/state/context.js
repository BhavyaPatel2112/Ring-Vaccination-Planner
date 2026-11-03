import { createContext, useContext } from 'react'

export const WorkspaceContext = createContext(null)

/** Current network, cases, rings and plan, plus the actions that change them. */
export function useWorkspace() {
  const value = useContext(WorkspaceContext)
  if (!value) throw new Error('useWorkspace must be used inside WorkspaceProvider')
  return value
}
