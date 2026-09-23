export default function LoadingState({ label = 'Loading…' }) {
  return (
    <div className="flex min-h-40 flex-col items-center justify-center gap-3 py-12 text-ma-muted">
      <div
        className="h-8 w-8 animate-spin rounded-full border-2 border-ma-border border-t-ma-primary"
        aria-hidden
      />
      <p className="text-sm">{label}</p>
    </div>
  )
}
