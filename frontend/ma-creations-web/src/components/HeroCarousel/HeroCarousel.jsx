import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../Button/Button'
import {
  getHeroSlides,
  getShopNowTarget,
  isHashTarget,
  isPathTarget,
} from '../../config/hero'

/**
 * Asset-driven hero. Default images: /heroes/hero-1.png … hero-3.png.
 * Missing files fall back to gradient placeholders; carousel keeps working.
 * fullBleed slides show the designed banner uncropped (CTA in the art stays visible).
 */
export default function HeroCarousel() {
  const navigate = useNavigate()
  const slides = getHeroSlides()
  const [index, setIndex] = useState(0)
  const [loadedImages, setLoadedImages] = useState({})
  const [failedImages, setFailedImages] = useState({})
  const shopNowTarget = getShopNowTarget()

  useEffect(() => {
    if (slides.length <= 1) {
      return undefined
    }
    const timer = setInterval(() => {
      setIndex((current) => (current + 1) % slides.length)
    }, 4500)
    return () => clearInterval(timer)
  }, [slides.length])

  const slide = slides[index] || slides[0]
  const activeTarget = slide?.href || shopNowTarget
  const slideId = slide?.id
  const imageSrc = slide?.image && !failedImages[slideId] ? slide.image : null
  const hasRealImage = Boolean(imageSrc && loadedImages[slideId])

  function handleShopNow() {
    if (isHashTarget(activeTarget)) {
      document.getElementById(activeTarget.slice(1))?.scrollIntoView({ behavior: 'smooth' })
      return
    }
    if (isPathTarget(activeTarget)) {
      navigate(activeTarget)
    }
  }

  const fullBleed = Boolean(slide?.fullBleed && hasRealImage)
  const showText = !fullBleed && Boolean(slide?.title)
  const ctaLabel = slide?.ariaLabel || 'Shop Now'

  const dots = slides.length > 1 && (
    <div
      className={`z-10 flex gap-2 ${
        fullBleed
          ? 'mt-3 justify-center'
          : 'absolute bottom-4 left-1/2 -translate-x-1/2'
      }`}
    >
      {slides.map((item, i) => (
        <button
          key={item.id}
          type="button"
          aria-label={`Go to slide ${i + 1}`}
          aria-current={i === index ? 'true' : undefined}
          className={`h-2.5 w-2.5 rounded-full focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary ${
            i === index ? 'bg-ma-primary' : 'bg-ma-primary/35'
          } ${!fullBleed ? (i === index ? '!bg-white' : '!bg-white/50') : ''}`}
          onClick={() => setIndex(i)}
        />
      ))}
    </div>
  )

  const preloadImg =
    imageSrc && !hasRealImage ? (
      <img
        src={imageSrc}
        alt=""
        aria-hidden="true"
        className="pointer-events-none absolute h-0 w-0 opacity-0"
        onLoad={() => setLoadedImages((prev) => ({ ...prev, [slideId]: true }))}
        onError={() => setFailedImages((prev) => ({ ...prev, [slideId]: true }))}
      />
    ) : null

  return (
    <section className="relative" aria-roledescription="carousel" aria-label="Featured">
      {preloadImg}
      {fullBleed ? (
        <div className="relative w-full bg-ma-bg">
          <img
            src={imageSrc}
            alt={slide.title || 'MA CREATIONS'}
            className="block h-auto w-full"
            loading={index === 0 ? 'eager' : 'lazy'}
          />
          <button
            type="button"
            onClick={handleShopNow}
            className="absolute inset-0 z-10 cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-4px] focus-visible:outline-ma-primary"
            aria-label={ctaLabel}
          />
        </div>
      ) : (
        <div
          className={`relative flex min-h-[56vw] max-h-[30rem] items-end overflow-hidden px-4 py-10 md:min-h-[24rem] md:px-8 ${
            hasRealImage ? 'bg-ma-text' : `bg-gradient-to-br ${slide.tone}`
          }`}
        >
          {hasRealImage && (
            <img
              src={imageSrc}
              alt={slide.title || 'MA CREATIONS'}
              className="absolute inset-0 h-full w-full object-cover"
              loading={index === 0 ? 'eager' : 'lazy'}
            />
          )}
          <div className="absolute inset-0 bg-gradient-to-t from-[#2c2c2c]/70 via-[#2c2c2c]/25 to-transparent" />
          <div className="relative z-10 mx-auto w-full max-w-6xl">
            {!hasRealImage && (
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/80">
                Hero assets pending
              </p>
            )}
            {showText && (
              <h1 className="mt-2 max-w-xl text-3xl font-bold text-white md:text-5xl">{slide.title}</h1>
            )}
            {slide.subtitle && (
              <p className="mt-2 max-w-md text-sm text-white/90 md:text-base">{slide.subtitle}</p>
            )}
            <Button className="mt-5 !bg-ma-primary" onClick={handleShopNow}>
              {ctaLabel}
            </Button>
          </div>
          {dots}
        </div>
      )}

      {fullBleed && dots}
    </section>
  )
}
