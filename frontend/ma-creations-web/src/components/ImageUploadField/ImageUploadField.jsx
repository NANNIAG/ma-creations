import { useEffect, useId, useState } from 'react'

/**
 * Styled image upload block for admin CMS forms.
 * Keeps native file input for accessibility; presents a clear drop-area UI.
 */
export default function ImageUploadField({
  label = 'Image',
  required = false,
  value,
  onChange,
  error,
  hint = 'JPEG, PNG, WEBP, or GIF',
  previewUrl = null,
}) {
  const inputId = useId()
  const [localPreview, setLocalPreview] = useState(null)

  useEffect(() => {
    if (!value) {
      setLocalPreview(null)
      return undefined
    }
    const url = URL.createObjectURL(value)
    setLocalPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [value])

  const displayPreview = localPreview || previewUrl

  return (
    <div className="block text-sm">
      <span className="font-medium">
        {label}
        {required ? ' *' : ''}
      </span>

      <label
        htmlFor={inputId}
        className={`mt-2 flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-4 py-6 text-center transition hover:border-ma-primary hover:bg-ma-primary/5 ${
          error ? 'border-red-400 bg-red-50/40' : 'border-ma-border bg-ma-surface'
        }`}
      >
        {displayPreview ? (
          <img
            src={displayPreview}
            alt=""
            className="h-28 w-28 rounded-lg object-cover ring-1 ring-ma-border"
          />
        ) : (
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-ma-bg text-ma-muted ring-1 ring-ma-border">
            <UploadIcon />
          </div>
        )}

        <div>
          <p className="font-semibold text-ma-text">
            {value ? 'Change image' : 'Click to upload image'}
          </p>
          <p className="mt-1 text-xs text-ma-muted">{hint}</p>
          {value?.name && (
            <p className="mt-1 max-w-xs truncate text-xs text-ma-primary">{value.name}</p>
          )}
        </div>

        <input
          id={inputId}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          className="sr-only"
          aria-label={label}
          onChange={(e) => onChange?.(e.target.files?.[0] || null)}
        />
      </label>

      {error && (
        <span className="mt-1 block text-xs text-red-700" role="alert">
          {error}
        </span>
      )}
    </div>
  )
}

function UploadIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 16V4m0 0 4 4m-4-4-4 4"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M20 16.5V18a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-1.5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  )
}
