/**
 * The calm "room" behind every screen: soft warm and cool light and a faint
 * perspective floor. Purely decorative and static.
 */
export function SceneBackdrop() {
  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <div
        className="absolute inset-0"
        style={{ background: 'linear-gradient(180deg, var(--scene-top) 0%, var(--scene-bottom) 100%)' }}
      />
      <div
        className="absolute -left-[20%] -top-[30%] h-[80vh] w-[80vw] rounded-full blur-3xl"
        style={{ background: 'radial-gradient(closest-side, var(--scene-glow-a), transparent)' }}
      />
      <div
        className="absolute -right-[15%] top-[10%] h-[70vh] w-[60vw] rounded-full blur-3xl"
        style={{ background: 'radial-gradient(closest-side, var(--scene-glow-b), transparent)' }}
      />
      <div className="scene-floor" />
    </div>
  )
}
