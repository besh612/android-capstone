package org.tensorflow.lite.examples.detection.server_communication;

import com.google.gson.annotations.SerializedName;

public class SendObject {
    @SerializedName("imageUrl")
    private int imageUrl;

    @SerializedName("latitude")
    private int latitude;

    @SerializedName("longitude")
    private int longitude;

    @SerializedName("height")
    private int height;

}
