package codeprose.facemark;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.HashMap;
import java.util.List;

public class FaceMarker {

    private static SetResultInterface mCallback;

    public static void setInterface(SetResultInterface s) {
        mCallback = s;
    }

    static HashMap<FirebaseVisionFaceLandmark, Tuple> landmarks;

    public interface SetResultInterface {
        void setImage(Bitmap bitmap);
    }

    private static final String TAG = "FaceMarker";
    private static OnSuccessListener faceDetectionSuccessListener;
    private static OnFailureListener faceDetectionFailureListener;
    private static Bitmap resultBitmap;

    public static Bitmap detectFacesAndOverlayLandmarks(Context c, Bitmap bitmap) {
        resultBitmap = bitmap;
        init(c);

        // Build  Face API options
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .build();

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);

        detector.detectInImage(image)
                .addOnSuccessListener(faceDetectionSuccessListener)
                .addOnFailureListener(faceDetectionFailureListener);
        return resultBitmap;
    }

    private static void init(final Context c) {

        faceDetectionSuccessListener = new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> faces) {
                Log.d(TAG, String.format("onSuccess: %d faces detected", faces.size()));

                for (FirebaseVisionFace face : faces) {
                    getLandmarks(face);
                    mCallback.setImage(resultBitmap);
                }
            }
        };

        faceDetectionFailureListener =
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        try {
                            throw e;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                };
    }

    private static void getLandmarks(FirebaseVisionFace face) {

        landmarks = new HashMap<>();

        for (int i = 0; i <= 12; i++) {
            if (i == 2 || i == 8) continue;
            FirebaseVisionFaceLandmark landmark = face.getLandmark(i);
            if (landmark != null) {

                FirebaseVisionPoint pos = landmark.getPosition();
                landmarks.put(landmark, new Tuple(pos.getX(), pos.getY()));
            }
        }

        resultBitmap = drawLandmarks(resultBitmap);

    }

    private static Bitmap drawLandmarks(Bitmap backgroundBitmap) {

        Bitmap markedBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());
        Canvas canvas = new Canvas(markedBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#00bcd7"));

        for (HashMap.Entry<FirebaseVisionFaceLandmark, Tuple> landmark : landmarks.entrySet()) {
            Tuple pos = landmark.getValue();
            float X = pos.X, Y = pos.Y;
            Log.d(TAG, "drawLandmarks: " + pos.toString());
            float radius = 20f;
            canvas.drawCircle(X, Y, radius, paint);
        }

        return markedBitmap;
    }


}
