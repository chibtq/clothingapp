package com.example.foodappfinal.Model;

import com.google.gson.annotations.SerializedName;

public class Address {
    @SerializedName("address_id")
    private String addressId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("address_name")
    private String addressName; // New field for address name (e.g., "Home", "Work")

    @SerializedName("address_line")
    private String addressLine;

    private String city;
    private String country;

    @SerializedName("postal_code")
    private String postalCode;

    @SerializedName("is_default")
    private boolean isDefault;

    public Address() {
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    // Method to get full address as a string for display
    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        if (addressName != null && !addressName.isEmpty()) {
            fullAddress.append(addressName).append(": ");
        }
        fullAddress.append(addressLine);
        if (city != null && !city.isEmpty()) {
            fullAddress.append(", ").append(city);
        }
        if (country != null && !country.isEmpty()) {
            fullAddress.append(", ").append(country);
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            fullAddress.append(" ").append(postalCode);
        }
        return fullAddress.toString();
    }
}