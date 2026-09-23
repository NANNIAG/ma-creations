import Button from '../Button/Button'

export default function ErrorState({
  title = 'Something went wrong',
  message = 'Please try again.',
  onRetry,
}) {
  return (
    <div className="mx-auto max-w-md rounded-xl bg-ma-surface px-6 py-10 text-center">
      <h2 className="text-lg font-bold text-ma-text">{title}</h2>
      <p className="mt-2 text-sm text-ma-muted">{message}</p>
      {onRetry && (
        <Button className="mt-5" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
