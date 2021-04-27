/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.server_communication.RetrofitService;
import org.tensorflow.lite.examples.detection.server_communication.S3Object;
import org.tensorflow.lite.examples.detection.server_communication.SendObject;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = false;
  private static final String TF_OD_API_MODEL_FILE = "crack.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/crack.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.7f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

//  Capstone...주형
  private S3Object s3Object = new S3Object();
  private Retrofit retrofit = null;
  private RetrofitService retrofitService = null;
  private int file_cnt=0;
  private String file_name;
  private File file_send;
  private String uploadedFileUrl;
  private Bitmap cropCopyPrevBitmap = null;
  private String latitude;
  private String longitude;
  private String altitude;
  private String height;


  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              getAssets(),
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing classifier!");
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
        new Runnable() {
          @Override
          public void run() {

            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            }

            final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

            for (final Classifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                2021.04.07 김주형 개발 시작
                // 레트로핏 서비스 객체 생성

                // 타임아웃 설정???
//                OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                        .connectTimeout(1, TimeUnit.MINUTES)
//                        .readTimeout(30, TimeUnit.SECONDS)
//                        .writeTimeout(15, TimeUnit.SECONDS)
//                        .build();

                if (retrofit==null) {
//                  Log.d("RETROFIT", "retrofit생성");
                  retrofit = new Retrofit.Builder()
                          .baseUrl(RetrofitService.URL)
                          .addConverterFactory(GsonConverterFactory.create())
//                          .client(okHttpClient) //타임아웃 설정?
                          .build();
                  if (retrofitService == null) {
//                    Log.d("RETROFIT", "retrofitService 생성");
                    retrofitService = retrofit.create(RetrofitService.class);
                  }
//                  Log.d("RETROFIT", "locationService 시작");
                  startLocationService();
                }


                // 탐지된 이미지가 가장 처음 탐지된 이미지거나, 이전에 탐지된 이미지와 다르다면 S3서버 업로드
                // 이분은 나중에 수정 => 이미지를 시간간격으로 올릴지?
                if (cropCopyPrevBitmap == null || (comparePrevBitmap(cropCopyBitmap, cropCopyPrevBitmap) == false)) {
//                  HashMap<String, Object> sendData = new HashMap<>();
                  file_name = "file_" + file_cnt;
                  convertBitmapToFile(cropCopyBitmap, file_name);
                  file_send = new File(getFilesDir() + "/" + file_name + ".jpg");
                  uploadedFileUrl = s3Object.uploadWithTransferUtility(getApplicationContext(), file_name, file_send);
                  file_cnt++;
                  Toast toast =
                          Toast.makeText(
                                  //                        getApplicationContext(), "균열 발견됨!", Toast.LENGTH_SHORT);
                                  getApplicationContext(), uploadedFileUrl, Toast.LENGTH_SHORT);
                  toast.show();
                  SendObject sendData = new SendObject(uploadedFileUrl, "3.14592", latitude, longitude,
                          "rkq", altitude, "1", "comment", "1");
                  Log.d("RETROFIT", "POST Data: " + sendData.toString());
                  retrofitService.postData(sendData).enqueue(new Callback<Integer>() {
                    @Override
                    public void onResponse(Call<Integer> call, Response<Integer> response) {
                      if (response.isSuccessful()) {
                        Log.d("RETROFIT", response.toString());
                        Integer body = response.body();
                        if (body != null)
                          Log.d("RETROFIT", "onResponse: 성공, 결과\n" + body.toString());
                      }
                    }
                    @Override
                    public void onFailure(Call<Integer> call, Throwable t) {
                      Log.d("RETROFIT", "onFailure: " + t.getMessage());
                    }
                  });
//                  retrofitService.postData(sendData).enqueue(new Callback<Void>() {
//                    @Override
//                    public void onResponse(Call<Void> call, Response<Void> response) {
//                      if (response.isSuccessful()) {
//                        Log.d("RETROFIT", response.toString());
//                        Void body = response.body();
//                        if (body != null)
//                          Log.d("RETROFIT", "onResponse: 성공, 결과\n" + body.toString());
//                      }
//                    }
//                    @Override
//                    public void onFailure(Call<Void> call, Throwable t) {
//                      Log.d("RETROFIT", "onFailure: " + t.getMessage());
//                    }
//                  });


                }
                cropCopyPrevBitmap = cropCopyBitmap;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
              }
            }

            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

            runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                  }
                });
          }
        });
  }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public void convertBitmapToFile(Bitmap bitmap_ori, String name) {
    File storage = getFilesDir();
    String fileName = name + ".jpg";
    File imageFile = new File(storage, fileName);
    try {
      imageFile.createNewFile();
      FileOutputStream outputStream = new FileOutputStream(imageFile);
      bitmap_ori.compress(Bitmap.CompressFormat.JPEG, 10, outputStream);
      outputStream.close();
    } catch(Exception e) {}
//        Log.d("AWS", imageFile.getAbsolutePath());
    Bitmap bitmap = BitmapFactory.decodeFile(getFilesDir().getAbsolutePath() + "/" + fileName);
//    bitmap = resizeImage(bitmap);
  }
  private boolean comparePrevBitmap(Bitmap bitmap1, Bitmap bitmap2) {
    ByteBuffer buffer1 = ByteBuffer.allocate(bitmap1.getHeight() * bitmap1.getRowBytes());
    bitmap1.copyPixelsToBuffer(buffer1);
    ByteBuffer buffer2 = ByteBuffer.allocate(bitmap2.getHeight() * bitmap2.getRowBytes());
    bitmap2.copyPixelsToBuffer(buffer2);
    return Arrays.equals(buffer1.array(), buffer2.array());
  }

  // 현재 위치를 알기위한 메소드
  public void startLocationService() {
    LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    try {
      Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (location != null) {
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());
        altitude = String.valueOf(location.getAltitude());
      }
      GPSListener gpsListener = new GPSListener();
      long minTime = 10000;
      float minDistance = 0;
      manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);
//            Toast.makeText(getApplicationContext(), "Checking Location Request complete", Toast.LENGTH_LONG).show();
    } catch(SecurityException e) {}
//    Log.d("RETROFIT", "latitude: " + latitude + "\nlongitude: " + longitude + "\naltitude: " + altitude);
  }
  // GPSListener 내부 클래스 정의
  class GPSListener implements LocationListener {
    @Override
    public void onLocationChanged(@NonNull Location location) {
      latitude = String.valueOf(location.getLatitude());
      longitude = String.valueOf(location.getLongitude());
      altitude = String.valueOf(location.getAltitude());
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(@NonNull String provider) {}
    @Override
    public void onProviderDisabled(@NonNull String provider) {}
  }
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(() -> detector.setUseNNAPI(isChecked));
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }
}
