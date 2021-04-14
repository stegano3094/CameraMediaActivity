package com.stegano.cameramedia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecordActivity extends AppCompatActivity {
    private Button recordButton;
    private TextureView textureView;

    private Size previewSize;
    private CameraDevice cameraDevice;  // 실행된 카메라를 저장할 변수
    private CaptureRequest.Builder previewBuilder;  // 프리뷰를 생성할 빌더 객체
    private CameraCaptureSession previewSession;  // 캡션 세션을 저장할

    private MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        textureView = (TextureView) findViewById(R.id.preview);
        recordButton = (Button) findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording()) {
                    stopRecording(true);
                } else {
                    startRecording();
                }
            }
        });
    }

    private boolean isRecording() {
        return mediaRecorder != null;
    }

    private void startRecording() {
        recordButton.setText("중지");

        if(mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }

        String recordFilePath = getOutputMediaFile().getAbsolutePath();

        mediaRecorder.setVideoEncodingBitRate(5000000);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(previewSize.getWidth(), previewSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(recordFilePath);
        mediaRecorder.setOrientationHint(90);

        try {
            mediaRecorder.prepare();  // 녹화 준비
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        List<Surface> surfaces = new ArrayList<>();
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        Surface previewSurface = new Surface(surfaceTexture);
        surfaces.add(previewSurface);

        Surface mediaRecorderSurface = mediaRecorder.getSurface();
        surfaces.add(mediaRecorderSurface);

        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            previewBuilder.addTarget(previewSurface);
            previewBuilder.addTarget(mediaRecorderSurface);
            cameraDevice.createCaptureSession(surfaces, captureStateCallback, null);
            mediaRecorder.start();  // 녹화 시작
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording(boolean showPreview) {
        recordButton.setText("녹화");

        stopPreview();  // 프리뷰 정지

        if(mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if(showPreview) {
            startPreview();
        }
    }

    private File getOutputMediaFile() {
        String recordPath = getExternalCacheDir().getAbsolutePath();
        File mediaFile = new File(recordPath + File.separator + "record.mp4");
        return mediaFile;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording(false);
        stopPreview();
    }

    private void startPreview() {
        if(textureView.isAvailable()) {  // textureView의 상태 확인
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    private void stopPreview() {
        if(previewSession != null) {
            previewSession.close();
            previewSession = null;
        }
        if(cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    private void openCamera() {
        // 먼저 권한을 확인한다
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 카메라 서비스 가져오기
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            String targetCameraId = null;

            for(final String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                int cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

                if(cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    targetCameraId = cameraId;
                    break;
                }
            }

            if(targetCameraId == null) {
                return;
            }

            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(targetCameraId);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
            cameraManager.openCamera(targetCameraId, deviceStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(getApplicationContext(), "카메라를 찾는데 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;  // 실행되는 카메라를 전역 변수에 저장
            showPreview();  // 카메라가 준비되면 보여줄 메서드 호출함
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
            stopRecording(false);
            stopPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private void showPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        Surface surface = new Surface(surfaceTexture);

        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), captureStateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            previewSession = session;  // 세션이 만들어지면 세션을 저장함

            updatePreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    private void updatePreview() {
        try {
            previewSession.setRepeatingRequest(previewBuilder.build(), null, null);  // 화면 갱신 시작
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}