import { forwardRef } from 'react'

const variants = {
  primary:
    'bg-ma-primary text-white hover:brightness-95 disabled:opacity-50 disabled:cursor-not-allowed',
  secondary:
    'border border-ma-primary text-ma-text bg-transparent hover:bg-ma-primary/10 disabled:opacity-50 disabled:cursor-not-allowed',
  ghost: 'text-ma-text hover:bg-ma-border/60 disabled:opacity-50 disabled:cursor-not-allowed',
}

const sizes = {
  sm: 'px-3 py-2 text-sm',
  md: 'px-4 py-2.5 text-sm md:text-base',
  lg: 'px-5 py-3 text-base',
}

const Button = forwardRef(function Button(
  {
    children,
    variant = 'primary',
    size = 'md',
    className = '',
    type = 'button',
    fullWidth = false,
    ...props
  },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      className={[
        'inline-flex items-center justify-center rounded-lg font-semibold transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary',
        variants[variant] || variants.primary,
        sizes[size] || sizes.md,
        fullWidth ? 'w-full' : '',
        className,
      ].join(' ')}
      {...props}
    >
      {children}
    </button>
  )
})

export default Button
