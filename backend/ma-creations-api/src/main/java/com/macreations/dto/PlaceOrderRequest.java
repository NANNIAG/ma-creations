package com.macreations.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.macreations.entity.PaymentMethod;

/**
 * Place-order request. Never accept money totals or customerId from the client.
 */
public class PlaceOrderRequest {

    @NotBlank(message = "previewHash is required")
    private String previewHash;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "idempotencyKey is required")
    @Size(max = 64)
    private String idempotencyKey;

    @NotBlank(message = "contactName is required")
    @Size(max = 150)
    private String contactName;

    @NotBlank(message = "contactMobile is required")
    @Size(max = 20)
    private String contactMobile;

    @Email(message = "contactEmail must be a valid email")
    @Size(max = 255)
    private String contactEmail;

    @Valid
    @NotNull(message = "shippingAddress is required")
    private ShippingAddressRequest shippingAddress;

    public String getPreviewHash() {
        return previewHash;
    }

    public void setPreviewHash(String previewHash) {
        this.previewHash = previewHash;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactMobile() {
        return contactMobile;
    }

    public void setContactMobile(String contactMobile) {
        this.contactMobile = contactMobile;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public ShippingAddressRequest getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddressRequest shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public static class ShippingAddressRequest {
        @NotBlank(message = "fullName is required")
        @Size(max = 150)
        private String fullName;

        @NotBlank(message = "mobile is required")
        @Size(max = 20)
        private String mobile;

        @Email
        @Size(max = 255)
        private String email;

        @NotBlank(message = "line1 is required")
        @Size(max = 255)
        private String line1;

        @Size(max = 255)
        private String line2;

        @Size(max = 255)
        private String landmark;

        @NotBlank(message = "city is required")
        @Size(max = 100)
        private String city;

        @NotBlank(message = "state is required")
        @Size(max = 100)
        private String state;

        @NotBlank(message = "postalCode is required")
        @Size(max = 20)
        private String postalCode;

        @Size(max = 100)
        private String country = "India";

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getLine1() {
            return line1;
        }

        public void setLine1(String line1) {
            this.line1 = line1;
        }

        public String getLine2() {
            return line2;
        }

        public void setLine2(String line2) {
            this.line2 = line2;
        }

        public String getLandmark() {
            return landmark;
        }

        public void setLandmark(String landmark) {
            this.landmark = landmark;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }
}
