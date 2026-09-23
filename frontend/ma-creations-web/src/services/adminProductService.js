import { apiRequest, resolveMediaUrl } from './apiClient'

function mapDetail(data) {
  if (!data) {
    return data
  }
  return {
    ...data,
    primaryImageUrl: resolveMediaUrl(data.primaryImageUrl),
    images: (data.images || []).map((image) => ({
      ...image,
      url: resolveMediaUrl(image.url),
    })),
  }
}

function mapListItem(item) {
  return {
    ...item,
    primaryImageUrl: resolveMediaUrl(item.primaryImageUrl),
  }
}

export async function createAdminProduct({ title, categoryId, sellingPrice, mrp, imageFile }) {
  const formData = new FormData()
  formData.append('title', title)
  formData.append('categoryId', String(categoryId))
  formData.append('sellingPrice', String(sellingPrice))
  formData.append('mrp', String(mrp))
  formData.append('image', imageFile)

  const data = await apiRequest('/api/admin/products', {
    method: 'POST',
    body: formData,
  })

  return mapDetail(data)
}

export async function fetchAdminProducts() {
  const data = await apiRequest('/api/admin/products')
  return (data || []).map(mapListItem)
}

export async function fetchAdminProduct(id) {
  const data = await apiRequest(`/api/admin/products/${id}`)
  return mapDetail(data)
}

export async function updateAdminProduct({ id, title, categoryId, sellingPrice, mrp, imageFile }) {
  const formData = new FormData()
  formData.append('title', title)
  formData.append('categoryId', String(categoryId))
  formData.append('sellingPrice', String(sellingPrice))
  formData.append('mrp', String(mrp))
  if (imageFile) {
    formData.append('image', imageFile)
  }

  const data = await apiRequest(`/api/admin/products/${id}`, {
    method: 'PUT',
    body: formData,
  })

  return mapDetail(data)
}

/**
 * Hide (published=false) or Publish (published=true) a product.
 */
export async function updateProductStatus(productId, published) {
  const data = await apiRequest(`/api/admin/products/${productId}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ published: Boolean(published) }),
  })
  return mapListItem(data)
}
