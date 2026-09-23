import { useEffect, useRef } from 'react'
import Button from '../Button/Button'

/**
 * In-app confirmation dialog (replaces window.confirm for admin actions).
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  busy = false,
  onConfirm,
  onCancel,
}) {
  const confirmRef = useRef(null)

  useEffect(() => {
    if (!open) {
      return undefined
    }
    confirmRef.current?.focus()
    function onKeyDown(event) {
      if (event.key === 'Escape' && !busy) {
        onCancel?.()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, busy, onCancel])

  if (!open) {
    return null
  }

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-[#2c2c2c]/45 p-4"
      role="presentation"
      onClick={() => {
        if (!busy) {
          onCancel?.()
        }
      }}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-message"
        className="w-full max-w-md rounded-xl bg-ma-surface p-5 shadow-lg ring-1 ring-ma-border"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-dialog-title" className="text-lg font-bold text-ma-text">
          {title}
        </h2>
        <p id="confirm-dialog-message" className="mt-2 text-sm text-ma-muted">
          {message}
        </p>
        <div className="mt-5 flex flex-wrap justify-end gap-2">
          <Button variant="secondary" size="sm" disabled={busy} onClick={onCancel}>
            {cancelLabel}
          </Button>
          <Button ref={confirmRef} size="sm" disabled={busy} onClick={onConfirm}>
            {busy ? 'Working…' : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
