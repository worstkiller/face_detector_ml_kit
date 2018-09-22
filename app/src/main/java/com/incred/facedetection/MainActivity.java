package com.incred.facedetection;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.androidbuffer.kotlinfilepicker.KotConstants;
import com.androidbuffer.kotlinfilepicker.KotRequest;
import com.androidbuffer.kotlinfilepicker.KotResult;
import com.androidbuffer.kotlinfilepicker.KotUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.incred.facedetection.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by incred on 22/9/18.
 */

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private AppCompatButton btnUpload, btnReplay;
    private FirebaseVisionFaceDetectorOptions options;
    private Handler handler;
    private Uri uri;
    private MediaMetadataRetriever retriever;
    private MediaPlayer mediaPlayerEx;
    private FirebaseVisionFaceDetector detector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        buttonClickListener();
        manageVideoListener();
    }

    private void initViews() {
        FirebaseApp.initializeApp(this);
        videoView = findViewById(R.id.videoView);
        btnUpload = findViewById(R.id.btnUpload);
        btnReplay = findViewById(R.id.btnReplay);
        options = getOptions();
        handler = new Handler();
        retriever = new MediaMetadataRetriever();
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    private void buttonClickListener() {

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnReplay.setEnabled(false);
                mediaPlayerEx.start();
                startBitmapFetcher();
            }
        });
    }

    private void manageVideoListener() {

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayerEx = mediaPlayer;
                btnReplay.setEnabled(true);
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                btnReplay.setEnabled(true);
            }
        });
    }

    private void startBitmapFetcher() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerEx.isPlaying()) {
                    Bitmap frame = retriever.getFrameAtTime((long) mediaPlayerEx.getCurrentPosition() * 10000, MediaMetadataRetriever.OPTION_CLOSEST);
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(frame);
                    detectFramesFromBitmap(image);
                    saveBitmap(frame);
                    handler.postDelayed(this, 200);
                }
                Log.d("TAG ", "video playing " + mediaPlayerEx.getCurrentPosition());
            }
        };
        runnable.run();
    }

    private void detectFramesFromBitmap(FirebaseVisionImage image) {
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                // Task completed successfully
                                Log.d("TAG", "success detected faces in given frame");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                Log.d("TAG", "failed to detect face in given frame");
                                e.printStackTrace();
                            }
                        });
    }

    private void openGallery() {
        new KotRequest.File(this, 101).pick();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (101 == requestCode && resultCode == Activity.RESULT_OK) {
            ArrayList<KotResult> arrayList = data.getParcelableArrayListExtra(KotConstants.EXTRA_FILE_RESULTS);
            uri = arrayList.get(0).getUri();
            videoView.setVideoURI(uri);
            retriever.setDataSource(this, uri);
        }
    }

    private FirebaseVisionFaceDetectorOptions getOptions() {
        return new FirebaseVisionFaceDetectorOptions.Builder()
                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.15f)
                .setTrackingEnabled(true)
                .build();
    }

    private void saveBitmap(final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String fileName = "Image_" + System.currentTimeMillis();
                File file = new File(Environment.getExternalStorageDirectory(), "/facedetector/");
                try {
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    FileOutputStream outputStream = new FileOutputStream(File.createTempFile(fileName, ".jpg", file));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
