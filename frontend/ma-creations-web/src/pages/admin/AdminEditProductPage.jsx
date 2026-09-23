import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Button from '../../components/Button/Button'
import ErrorState from '../../components/ErrorState/ErrorState'
import ImageUploadField from '../../components/ImageUploadField/ImageUploadField'
import LoadingState from '../../components/Loading/LoadingState'
import { ApiError } from '../../services/apiClient'
import { fetchAdminProduct, updateAdminProduct } from '../../services/adminProductService'
import { fetchCategories } from '../../services/categoryService'

export default function AdminEditProductPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [title, setTitle] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [sellingPrice, setSellingPrice] = useState('')
  const [mrp, setMrp] = useState('')
  const [currentImageUrl, setCurrentImageUrl] = useState(null)
  const [imageFile, setImageFile] = useState(null)

  const [fieldErrors, setFieldErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setLoadError('')
      try {
        const [product, cats] = await Promise.all([fetchAdminProduct(id), fetchCategories()])
        if (cancelled) {
          return
        }
        setCategories(cats || [])
        setTitle(product.title || '')
        setCategoryId(product.category?.id ? String(product.category.id) : '')
        setSellingPrice(product.sellingPrice != null ? String(product.sellingPrice) : '')
        setMrp(product.mrp != null ? String(product.mrp) : '')
        const primary =
          product.images?.[0]?.url ||
          product.primaryImageUrl ||
          null
        setCurrentImageUrl(primary)
      } catch (err) {
        if (!cancelled) {
          setLoadError(err.message || 'Failed to load product')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [id])

  function validate() {
    const next = {}
    if (!title.trim()) next.title = 'Product title is required'
    if (!categoryId) next.categoryId = 'Category is required'
    if (sellingPrice === '' || Number.isNaN(Number(sellingPrice)) || Number(sellingPrice) < 0) {
      next.sellingPrice = 'Enter a valid selling price (0 or more)'
    }
    if (mrp === '' || Number.isNaN(Number(mrp)) || Number(mrp) < 0) {
      next.mrp = 'Enter a valid MRP (0 or more)'
    }
    if (
      !next.sellingPrice &&
      !next.mrp &&
      Number(sellingPrice) > Number(mrp)
    ) {
      next.sellingPrice = 'Selling price should not be greater than MRP'
    }
    if (imageFile && imageFile.type && !imageFile.type.startsWith('image/')) {
      next.image = 'File must be an image (JPEG, PNG, WEBP, or GIF)'
    }
    setFieldErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError('')
    setSuccess('')
    if (!validate()) {
      return
    }

    setSubmitting(true)
    try {
      const updated = await updateAdminProduct({
        id,
        title: title.trim(),
        categoryId,
        sellingPrice,
        mrp,
        imageFile,
      })
      setSuccess(`Product “${updated.title}” updated successfully.`)
      if (updated.images?.[0]?.url) {
        setCurrentImageUrl(updated.images[0].url)
      }
      setImageFile(null)
      window.setTimeout(() => navigate('/admin/products'), 1200)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setFormError('Session expired. Please log in again.')
      } else if (err instanceof ApiError && err.status === 404) {
        setFormError('Product not found.')
      } else {
        setFormError(err.message || 'Could not update product')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <LoadingState label="Loading product…" />
  }

  if (loadError) {
    return <ErrorState title="Could not load product" message={loadError} />
  }

  return (
    <div className="max-w-xl">
      <Link to="/admin/products" className="text-sm text-ma-muted hover:text-ma-primary">
        ← Products
      </Link>
      <h1 className="mt-3 text-2xl font-bold">Edit Product</h1>
      <p className="mt-1 text-sm text-ma-muted">
        Update confirmed fields only. Leave image blank to keep the current photo.
      </p>

      <form className="mt-6 space-y-4" onSubmit={handleSubmit} noValidate>
        <label className="block text-sm">
          <span className="font-medium">Product Title</span>
          <input
            className="mt-1 w-full rounded-lg border border-ma-border bg-ma-surface px-3 py-2.5"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
          {fieldErrors.title && <span className="mt-1 block text-xs text-red-700">{fieldErrors.title}</span>}
        </label>

        <label className="block text-sm">
          <span className="font-medium">Category</span>
          <select
            className="mt-1 w-full rounded-lg border border-ma-border bg-ma-surface px-3 py-2.5"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          >
            <option value="">Select a category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          {fieldErrors.categoryId && (
            <span className="mt-1 block text-xs text-red-700">{fieldErrors.categoryId}</span>
          )}
        </label>

        <ImageUploadField
          label="Replace image (optional)"
          value={imageFile}
          onChange={setImageFile}
          error={fieldErrors.image}
          previewUrl={imageFile ? null : currentImageUrl}
          hint="Leave empty to keep the current photo. JPEG, PNG, WEBP, or GIF."
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm">
            <span className="font-medium">Selling Price (₹)</span>
            <input
              type="number"
              min="0"
              step="0.01"
              className="mt-1 w-full rounded-lg border border-ma-border bg-ma-surface px-3 py-2.5"
              value={sellingPrice}
              onChange={(e) => setSellingPrice(e.target.value)}
            />
            {fieldErrors.sellingPrice && (
              <span className="mt-1 block text-xs text-red-700">{fieldErrors.sellingPrice}</span>
            )}
          </label>
          <label className="block text-sm">
            <span className="font-medium">MRP (₹)</span>
            <input
              type="number"
              min="0"
              step="0.01"
              className="mt-1 w-full rounded-lg border border-ma-border bg-ma-surface px-3 py-2.5"
              value={mrp}
              onChange={(e) => setMrp(e.target.value)}
            />
            {fieldErrors.mrp && <span className="mt-1 block text-xs text-red-700">{fieldErrors.mrp}</span>}
          </label>
        </div>

        {formError && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
            {formError}
          </p>
        )}
        {success && (
          <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-800" role="status">
            {success}
          </p>
        )}

        <Button type="submit" disabled={submitting}>
          {submitting ? 'Saving…' : 'Save changes'}
        </Button>
      </form>
    </div>
  )
}
