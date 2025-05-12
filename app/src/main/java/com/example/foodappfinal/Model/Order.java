package com.example.foodappfinal.Model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Order {
    private String order_id;
    private String user_id;
    private String address_id;
    private double total_amount;
    private double subtotal;
    private double delivery_fee;
    private double tax;
    private String discount_code;
    private double discount_amount;
    private String status;
    private String created_at;

    public String getOrderId() {
        return order_id;
    }

    public void setOrderId(String order_id) {
        this.order_id = order_id;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String user_id) {
        this.user_id = user_id;
    }

    public String getAddressId() {
        return address_id;
    }

    public void setAddressId(String address_id) {
        this.address_id = address_id;
    }

    public double getTotalAmount() {
        return total_amount;
    }

    public void setTotalAmount(double total_amount) {
        this.total_amount = total_amount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDeliveryFee() {
        return delivery_fee;
    }

    public void setDeliveryFee(double delivery_fee) {
        this.delivery_fee = delivery_fee;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public String getDiscountCode() {
        return discount_code;
    }

    public void setDiscountCode(String discount_code) {
        this.discount_code = discount_code;
    }

    public double getDiscountAmount() {
        return discount_amount;
    }

    public void setDiscountAmount(double discount_amount) {
        this.discount_amount = discount_amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(String created_at) {
        this.created_at = created_at;
    }

    public Date getCreatedAtAsDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            return sdf.parse(created_at);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date(); // Fallback to current date if parsing fails
        }
    }
}