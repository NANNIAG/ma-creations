import { apiRequest } from './apiClient'

export async function fetchCategories() {
  return apiRequest('/api/categories', { auth: 'none' })
}
