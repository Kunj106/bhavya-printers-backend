package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyGstDto {
    private int month;
    private int year;
    private double taxableAmount;
    private double gst12Amount;
    private double gst18Amount;
    private double totalGst;
    private int orderCount;
}
