package codeprose.facemark;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;

public class FaceMarker {

    private static SetResultInterface mCallback;

    public static void setInterface(SetResultInterface s) {
        mCallback = s;
    }

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

        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                        .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
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

}
