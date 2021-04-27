package org.tensorflow.lite.examples.detection.server_communication;

import com.google.gson.annotations.SerializedName;

public class SendObject {
    public SendObject(String photoUrl, String width, String locationX, String locationY,
                String locationDetail, String height, String riskLevelInteger,
                String comment, String structureId) {
        this.photoUrl = photoUrl;
        this.width = width;
        this.locationX = locationX;
        this.locationY = locationY;
        this.locationDetail = locationDetail;
        this.height = height;
        this.riskLevelInteger = riskLevelInteger;
        this.comment = comment;
        this.structureId = structureId;
    }

    @SerializedName("photoUrl")
    private String photoUrl;

    @SerializedName("width")
    private String width;

    @SerializedName("locationX")
    private String locationX;

    @SerializedName("locationY")
    private String locationY;

    @SerializedName("locationDetail")
    private String locationDetail;

    @SerializedName("height")
    private String height;

    @SerializedName("riskLevelInteger")
    private String riskLevelInteger;

    @SerializedName("comment")
    private String comment;

    @SerializedName("structureId")
    private String structureId;

    @Override
    public String toString() {
        return "Data{" +
                "photoUrl=" + photoUrl + ",\n\t" +
                "width=" + width + ",\n\t" +
                "locationX=" + locationX + ",\n\t" +
                "locationY=" + locationY + ",\n\t" +
                "locationDetail=" + locationDetail + ",\n\t" +
                "height=" + height + ",\n\t" +
                "riskLevelInteger=" + riskLevelInteger + ",\n\t" +
                "comment=" + comment + ",\n\t" +
                "structureId=" + structureId + "\n}";
    }
}
