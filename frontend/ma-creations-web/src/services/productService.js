import { apiRequest, resolveMediaUrl } from './apiClient'

function mapListItem(product) {
  return {
    ...product,
    primaryImageUrl: resolveMediaUrl(product.primaryImageUrl),
  }
}

function mapDetail(product) {
  return {
    ...product,
    images: (product.images || []).map((image) => ({
      ...image,
      url: resolveMediaUrl(image.url),
    })),
  }
}

export async function fetchProducts({ categoryId, sort = 'newest', search } = {}) {
  const params = new URLSearchParams()
  if (categoryId != null && categoryId !== '') {
    params.set('categoryId', String(categoryId))
  }
  if (sort) {
    params.set('sort', sort)
  }
  if (search != null && String(search).trim() !== '') {
    params.set('search', String(search).trim())
  }
  const query = params.toString()
  const data = await apiRequest(`/api/products${query ? `?${query}` : ''}`, { auth: 'none' })
  return (data || []).map(mapListItem)
}

export async function fetchProductById(id) {
  const data = await apiRequest(`/api/products/${id}`, { auth: 'none' })
  return mapDetail(data)
}
