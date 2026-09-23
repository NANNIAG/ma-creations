package com.macreations.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.macreations.dto.AdminProductListItemResponse;
import com.macreations.dto.CreateProductRequest;
import com.macreations.dto.ProductDetailResponse;
import com.macreations.dto.ProductListItemResponse;
import com.macreations.dto.UpdateProductRequest;
import com.macreations.dto.UpdateProductStatusRequest;
import com.macreations.entity.Category;
import com.macreations.entity.Product;
import com.macreations.entity.ProductImage;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.mapper.ProductMapper;
import com.macreations.repository.ProductRepository;
import com.macreations.service.storage.ProductImageStorage;
import com.macreations.util.CatalogUtils;

@Service
public class ProductService {

    public static final int MAX_SEARCH_LENGTH = 100;

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final ProductImageStorage productImageStorage;

    public ProductService(
            ProductRepository productRepository,
            CategoryService categoryService,
            ProductMapper productMapper,
            ProductImageStorage productImageStorage) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.productMapper = productMapper;
        this.productImageStorage = productImageStorage;
    }

    @Transactional(readOnly = true)
    public List<ProductListItemResponse> listProducts(Long categoryId, String sort) {
        return listProducts(categoryId, sort, null);
    }

    @Transactional(readOnly = true)
    public List<ProductListItemResponse> listProducts(Long categoryId, String sort, String search) {
        if (categoryId != null) {
            categoryService.requireCategory(categoryId);
        }

        Sort springSort = resolveSort(sort);
        String normalizedSearch = normalizeSearch(search);

        List<Product> products;
        if (normalizedSearch == null) {
            products = categoryId == null
                    ? productRepository.findByPublishedTrue(springSort)
                    : productRepository.findByCategoryIdAndPublishedTrue(categoryId, springSort);
        } else {
            String pattern = CatalogUtils.toContainsLikePattern(normalizedSearch);
            products = categoryId == null
                    ? productRepository.search(pattern, springSort)
                    : productRepository.searchByCategoryId(categoryId, pattern, springSort);
        }

        return products.stream().map(productMapper::toListItem).toList();
    }

    /**
     * Admin CMS list — reuses the same product repository/sort, adds category + createdAt.
     * Includes both published and hidden products.
     */
    @Transactional(readOnly = true)
    public List<AdminProductListItemResponse> listAdminProducts() {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return products.stream()
                .peek(product -> {
                    product.getCategory().getName();
                    product.getImages().size();
                })
                .map(productMapper::toAdminListItem)
                .toList();
    }

    /**
     * Public storefront detail — published products only. Hidden → PRODUCT_NOT_FOUND.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicProduct(Long id) {
        Product product = productRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + id));
        product.getImages().size();
        product.getCategory().getName();
        return productMapper.toDetail(product);
    }

    /**
     * Admin detail — published and hidden.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getAdminProduct(Long id) {
        Product product = requireProduct(id);
        product.getImages().size();
        product.getCategory().getName();
        return productMapper.toDetail(product);
    }

    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request, MultipartFile image) {
        Category category = categoryService.requireCategory(request.getCategoryId());

        Product product = new Product();
        product.setTitle(request.getTitle().trim());
        product.setCategory(category);
        product.setSellingPrice(request.getSellingPrice());
        product.setMrp(request.getMrp());
        product.setSlug(generateUniqueSlug(request.getTitle(), null));
        product.setPublished(true);

        String storagePath = productImageStorage.store(image);
        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setStoragePath(storagePath);
        productImage.setSortOrder(0);
        product.getImages().add(productImage);

        Product saved = productRepository.save(product);
        return productMapper.toDetail(saved);
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest request, MultipartFile image) {
        Product product = requireProduct(id);
        product.getImages().size();

        Category category = categoryService.requireCategory(request.getCategoryId());
        String newTitle = request.getTitle().trim();
        boolean titleChanged = !newTitle.equals(product.getTitle());

        product.setTitle(newTitle);
        product.setCategory(category);
        product.setSellingPrice(request.getSellingPrice());
        product.setMrp(request.getMrp());
        if (titleChanged) {
            product.setSlug(generateUniqueSlug(newTitle, product.getId()));
        }

        if (image != null && !image.isEmpty()) {
            List<String> previousPaths = product.getImages().stream()
                    .map(ProductImage::getStoragePath)
                    .toList();
            product.getImages().clear();

            String storagePath = productImageStorage.store(image);
            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            productImage.setStoragePath(storagePath);
            productImage.setSortOrder(0);
            product.getImages().add(productImage);

            previousPaths.forEach(productImageStorage::delete);
        }

        Product saved = productRepository.save(product);
        saved.getImages().size();
        saved.getCategory().getName();
        return productMapper.toDetail(saved);
    }

    @Transactional
    public AdminProductListItemResponse updateProductStatus(Long id, UpdateProductStatusRequest request) {
        Product product = requireProduct(id);
        product.getImages().size();
        product.getCategory().getName();
        product.setPublished(Boolean.TRUE.equals(request.getPublished()));
        Product saved = productRepository.save(product);
        saved.getImages().size();
        saved.getCategory().getName();
        return productMapper.toAdminListItem(saved);
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + id));
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank() || "newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String key = sort.toLowerCase();
        if ("price_asc".equals(key)) {
            return Sort.by(Sort.Direction.ASC, "sellingPrice");
        }
        if ("price_desc".equals(key)) {
            return Sort.by(Sort.Direction.DESC, "sellingPrice");
        }
        if ("newest".equals(key)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        throw new BadRequestException(
                "INVALID_SORT",
                "sort must be one of: price_asc, price_desc, newest");
    }

    /**
     * @return trimmed search text, or null when search should not be applied
     */
    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_SEARCH_LENGTH) {
            throw new BadRequestException(
                    "INVALID_SEARCH",
                    "search must be at most " + MAX_SEARCH_LENGTH + " characters");
        }
        return trimmed;
    }

    private String generateUniqueSlug(String title, Long excludeProductId) {
        String base = CatalogUtils.slugify(title);
        String candidate = base;
        int suffix = 2;
        while (slugTaken(candidate, excludeProductId)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean slugTaken(String candidate, Long excludeProductId) {
        if (excludeProductId == null) {
            return productRepository.existsBySlug(candidate);
        }
        return productRepository.existsBySlugAndIdNot(candidate, excludeProductId);
    }
}
