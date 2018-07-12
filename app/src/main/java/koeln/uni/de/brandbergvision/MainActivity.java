package koeln.uni.de.brandbergvision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private VisualRecognition vrClient;
    private CameraHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        IamOptions options = new IamOptions.Builder()
                .apiKey(getString(R.string.api_key))
                .build();

        vrClient = new VisualRecognition("2018-07-01",options);


        // Initialize camera helper
        helper = new CameraHelper(this);
    }

    public void takePicture(View view) {
//        helper.dispatchTakePictureIntent();
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }


    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

//        if(requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE) {
//            final Bitmap photo = helper.getBitmap(resultCode);
//            final File photoFile = helper.getFile(resultCode);

            final Bitmap photo = result.getOriginalBitmap();
            final File photoFile = new File(result.getUri().getPath());
            Log.d("PHOTO",photoFile.getName());
            ImageView preview = findViewById(R.id.preview);
//            preview.setImageBitmap(photo);
            preview.setImageURI(result.getUri());

            try {
                MediaStore.Images.Media.insertImage(getContentResolver(), result.getUri().getPath().toString() ,"bbv-" + System.currentTimeMillis() , "cropped Image");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "Classifying your image. This may take a few seconds", Toast.LENGTH_LONG).show();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {


                    try {
                        ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                                .imagesFile(photoFile)
                                .threshold((float) 0.3)
                                .owners(Arrays.asList("me"))
                                .addClassifierId("humanclassifier_2106950051")
                                .build();

                        final ClassifiedImages output = vrClient.classify(classifyOptions).execute();
                        Log.i("KLASSE", output.getImages().get(0).getClassifiers().get(0).getClasses().get(0).getClassName());
                        Log.i("SCORE", output.getImages().get(0).getClassifiers().get(0).getClasses().get(0).getScore().toString());

                        List<ClassResult> classes = output.getImages().get(0).getClassifiers().get(0).getClasses();


                       final StringBuffer buffer = new StringBuffer();
                        for(ClassResult result : classes){

                            String name = result.getClassName();
                            String score = Float.toString(result.getScore()*100);
                            Log.i("Class", result.getClassName());
                            Log.i("Score", result.getScore().toString());
                            buffer.append("Class: " + name +  "\n"
                                        + "Score: " + score + "% \n"
                                        + "__________________________\n");


                        }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView detectedObjects =
                                    findViewById(R.id.detected_objects);
                            detectedObjects.setText(buffer.toString());
                        }
                    });



                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
