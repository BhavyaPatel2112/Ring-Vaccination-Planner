/** True for networks recorded with wearable sensors (the SocioPatterns samples). */
export function isSensorData(summary) {
  return Boolean(summary?.name?.includes('SocioPatterns'))
}
