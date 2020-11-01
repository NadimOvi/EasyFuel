package com.gtr.easyfuel.Class;

public class SaveData {
 private String octaneImage;
 private String octaneText;
 private String amountImage;
 private String amountText;
 private String slipImage;

    public SaveData() {
    }

    public SaveData(String octaneImage, String octaneText, String amountImage, String amountText, String slipImage) {
        this.octaneImage = octaneImage;
        this.octaneText = octaneText;
        this.amountImage = amountImage;
        this.amountText = amountText;
        this.slipImage = slipImage;
    }

    public String getOctaneImage() {
        return octaneImage;
    }

    public void setOctaneImage(String octaneImage) {
        this.octaneImage = octaneImage;
    }

    public String getOctaneText() {
        return octaneText;
    }

    public void setOctaneText(String octaneText) {
        this.octaneText = octaneText;
    }

    public String getAmountImage() {
        return amountImage;
    }

    public void setAmountImage(String amountImage) {
        this.amountImage = amountImage;
    }

    public String getAmountText() {
        return amountText;
    }

    public void setAmountText(String amountText) {
        this.amountText = amountText;
    }

    public String getSlipImage() {
        return slipImage;
    }

    public void setSlipImage(String slipImage) {
        this.slipImage = slipImage;
    }
}
