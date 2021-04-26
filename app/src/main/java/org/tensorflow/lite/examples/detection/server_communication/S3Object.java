package org.tensorflow.lite.examples.detection.server_communication;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;

import java.io.File;

public class S3Object {

    public void downloadWithTransferUtility(Context mainContext, File filesDir) {
        // Cognito 샘플 코드, CredentialProvider 객체 생성
        // Amazon Cognito 인증 공급자를 초기화합니다
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                mainContext,
                "ap-northeast-2:1053a8b7-33a3-47e3-8299-62fecb3677cb", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        );
        TransferNetworkLossHandler.getInstance(mainContext);

        // TransferUtility 객체 생성
        TransferUtility transferUtility = TransferUtility.builder()
                .context(mainContext)
                .defaultBucket("kgu.capstone.bucket.v1")
                .s3Client(new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                .build();

        TransferObserver downloadObserver = transferUtility.download(
                "kgu.capstone.bucket.v1",
                "image01.jpg",
                new File(filesDir.getAbsolutePath() + "/image01.jpg"));

        downloadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED)
                    Log.d("AWS", "DOWNLOAD Completed");
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                try {
                    int done = Integer.valueOf((int) (Double.valueOf(bytesCurrent / bytesTotal) * 100.0));
                    Log.d("AWS", "DOWNLOAD-- id: " + id + ", percent done: " + done);
                } catch (Exception e) {
                    Log.d("AWS", "Trouble calculating progress percent" + e.toString());
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d("AWS", "DOWNLOAD Error-- id: " + id + ", EX: " + ex.getMessage().toString());
            }
        });
    }

    public String uploadWithTransferUtility(Context mainContext, String filename, File file) {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                mainContext,
                "ap-northeast-2:1053a8b7-33a3-47e3-8299-62fecb3677cb", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        );
        AmazonS3Client client = new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2));
        TransferNetworkLossHandler.getInstance(mainContext);
        TransferUtility transferUtility = TransferUtility.builder()
                .context(mainContext)
                .defaultBucket("kgu.capstone.bucket.v1")
                .s3Client(client)
                .build();

        TransferObserver uploadObserver = transferUtility.upload(filename,
                file,
                CannedAccessControlList.PublicRead);

        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED)
                    Log.d("AWS", "UPLOAD Completed");
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                try {
                    int done = Integer.valueOf((int) (Double.valueOf(bytesCurrent / bytesTotal) * 100.0));
                    Log.d("AWS", "UPLOAD-- id: " + id + ", percent done: " + done);
                } catch (Exception e) {
                    Log.d("AWS", "Trouble calculating progress percent" + e.toString());
                }
            }
            @Override
            public void onError(int id, Exception ex) {
                Log.d("AWS", "UPLOAD Error-- id: " + id + ", EX: " + ex.getMessage().toString());
            }
        });
        String file_url = client.getResourceUrl("kgu.capstone.bucket.v1", filename);
        // 전송된 사진의 url
        System.out.println("Resource URL: " +
                client.getResourceUrl("kgu.capstone.bucket.v1", filename));
        return file_url;
    }
}
