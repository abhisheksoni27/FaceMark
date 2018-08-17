package com.example.android.emojify;

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

public class Emojifier {

    private static SetResultInterface mCallback;

    public static void setInterface(SetResultInterface s) {
        mCallback = s;
    }

    public interface SetResultInterface {
        void setImage(Bitmap bitmap);
    }

    private static final String TAG = "Emojifier";
    private static OnSuccessListener faceDetectionSuccessListener;
    private static OnFailureListener faceDetectionFailureListener;
    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;
    private static final float EMOJI_SCALE_FACTOR = .9f;
    private static Bitmap resultBitmap;

    private enum Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }

    public static Bitmap detectFacesAndOverlayEmoji(Context c, Bitmap bitmap) {
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
                    Emoji emoji = whichEmoji(face);

                    Bitmap emojiBitmap;
                    switch (emoji) {
                        case SMILE:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.smile);
                            break;
                        case FROWN:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.frown);
                            break;
                        case LEFT_WINK:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.leftwink);
                            break;
                        case RIGHT_WINK:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.rightwink);
                            break;
                        case LEFT_WINK_FROWN:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.leftwinkfrown);
                            break;
                        case RIGHT_WINK_FROWN:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.rightwinkfrown);
                            break;
                        case CLOSED_EYE_SMILE:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.closed_smile);
                            break;
                        case CLOSED_EYE_FROWN:
                            emojiBitmap = BitmapFactory.decodeResource(c.getResources(),
                                    R.drawable.closed_frown);
                            break;
                        default:
                            emojiBitmap = null;
                            Toast.makeText(c, R.string.no_emoji, Toast.LENGTH_SHORT).show();
                    }

                    resultBitmap = addBitmapToFace(resultBitmap, emojiBitmap, face);
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

    private static Emoji whichEmoji(FirebaseVisionFace face) {
        float smileProb = 0f, rightEyeOpenProb = 0f, leftEyeOpenProb = 0f;

        if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
            smileProb = face.getSmilingProbability();
        }
        if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
            rightEyeOpenProb = face.getRightEyeOpenProbability();
        }

        if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
            leftEyeOpenProb = face.getLeftEyeOpenProbability();
        }

        boolean leftEyeClosed = leftEyeOpenProb < EYE_OPEN_PROB_THRESHOLD;
        boolean rightEyeClosed = rightEyeOpenProb < EYE_OPEN_PROB_THRESHOLD;
        boolean smiling = smileProb > SMILING_PROB_THRESHOLD;

        Emoji emoji;
        if (smiling) {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.RIGHT_WINK;
            } else if (rightEyeClosed && !leftEyeClosed) {
                emoji = Emoji.LEFT_WINK;
            } else if (leftEyeClosed && rightEyeClosed) {
                emoji = Emoji.CLOSED_EYE_SMILE;
            } else {
                emoji = Emoji.SMILE;
            }
        } else {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.RIGHT_WINK_FROWN;
            } else if (rightEyeClosed && !leftEyeClosed) {
                emoji = Emoji.LEFT_WINK_FROWN;
            } else if (leftEyeClosed && rightEyeClosed) {
                emoji = Emoji.CLOSED_EYE_FROWN;
            } else {
                emoji = Emoji.FROWN;
            }
        }

        return emoji;
    }

    /**
     * Credits: Udacity.
     * <p>
     * Combines the original picture with the emoji bitmaps
     *
     * @param backgroundBitmap The original picture
     * @param emojiBitmap      The chosen emoji
     * @param face             The detected face
     * @return The final bitmap, including the emojis over the faces
     */
    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, FirebaseVisionFace face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        Rect boundingBox = face.getBoundingBox();
        int newEmojiWidth = (int) (boundingBox.width() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (boundingBox.left + boundingBox.width() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (boundingBox.top + boundingBox.height() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }
}
