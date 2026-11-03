import { motion } from 'motion/react'

/**
 * The deep "room" behind every screen: soft warm and cool light, a perspective
 * floor grid and a slow light sweep. Purely decorative.
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
      <motion.div
        className="absolute -top-1/2 left-0 h-[200%] w-1/3 -skew-x-12 opacity-[0.06]"
        style={{ background: 'linear-gradient(90deg, transparent, white, transparent)' }}
        animate={{ x: ['-60vw', '160vw'] }}
        transition={{ duration: 18, repeat: Infinity, ease: 'linear', repeatDelay: 6 }}
      />
      <div className="scene-floor" />
    </div>
  )
}
