package com.macreations.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Admin Hide / Publish toggle. published=true → storefront-visible; false → hidden.
 */
public class UpdateProductStatusRequest {

    @NotNull(message = "published is required")
    private Boolean published;

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }
}
