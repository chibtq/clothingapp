package com.example.foodappfinal.Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DiscountCode {
    private String code_id;
    private String code;
    private double discount_value;
    private String valid_from;
    private String valid_until;
    private boolean is_active;

    // Getters and setters
    public String getCodeId() {
        return code_id;
    }

    public void setCodeId(String code_id) {
        this.code_id = code_id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscountValue() {
        return discount_value;
    }

    public void setDiscountValue(double discount_value) {
        this.discount_value = discount_value;
    }

    public String getValidFrom() {
        return valid_from;
    }

    public void setValidFrom(String valid_from) {
        this.valid_from = valid_from;
    }

    public String getValidUntil() {
        return valid_until;
    }

    public void setValidUntil(String valid_until) {
        this.valid_until = valid_until;
    }

    public boolean isActive() {
        return is_active;
    }

    public void setActive(boolean is_active) {
        this.is_active = is_active;
    }

    public Date getValidUntilDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // Thiết lập múi giờ
            return sdf.parse(valid_until);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Date getValidFromDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            return sdf.parse(valid_from);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}