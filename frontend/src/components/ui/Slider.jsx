import * as RadixSlider from '@radix-ui/react-slider'

/** Labelled slider with the current value shown on the right. */
export function Slider({ label, value, onChange, min, max, step = 1, format = (v) => v }) {
  return (
    <label className="block">
      <span className="flex items-baseline justify-between text-xs">
        <span className="font-medium text-muted">{label}</span>
        <span className="font-semibold">{format(value)}</span>
      </span>
      <RadixSlider.Root
        className="relative mt-2 flex h-5 touch-none items-center select-none"
        value={[value]}
        min={min}
        max={max}
        step={step}
        onValueChange={([v]) => onChange(v)}
      >
        <RadixSlider.Track className="relative h-1.5 grow rounded-full bg-inset">
          <RadixSlider.Range className="absolute h-full rounded-full bg-accent" />
        </RadixSlider.Track>
        <RadixSlider.Thumb
          aria-label={label}
          className="block size-4 rounded-full border-2 border-accent bg-white shadow-md transition hover:scale-110"
        />
      </RadixSlider.Root>
    </label>
  )
}
