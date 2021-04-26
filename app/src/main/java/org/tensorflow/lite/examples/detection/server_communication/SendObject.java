package org.tensorflow.lite.examples.detection.server_communication;

import com.google.gson.annotations.SerializedName;

public class SendObject {
    @SerializedName("photoUrl")
    private String imageUrl;

    @SerializedName("width")
    private double width;

    @SerializedName("locationX")
    private double locationX;

    @SerializedName("locationY")
    private double locationY;

    @SerializedName("locationDetail")
    private String locationDetail;

    @SerializedName("height")
    private double height;

    @SerializedName("riskLevelInteger")
    private int riskLevelInteger;

    @SerializedName("comment")
    private String comment;

    @SerializedName("structureId")
    private int structureId;
}
