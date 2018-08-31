package koeln.uni.de.brandbergvision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
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
    private ErrorHandler errorHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set focus on MainActivity
        setContentView(R.layout.activity_main);


        //Setup Watson Visiual Recognition with API Key
        IamOptions options = new IamOptions.Builder()
                .apiKey(getString(R.string.api_key))
                .build();
        vrClient = new VisualRecognition("2018-07-01",options);


        // Initialize camera helper
        helper = new CameraHelper(this);
    }

    /**
     * Takes a picture with on board standard camera and gives it to cropping.
     * @param view Needs view to show in
     */
    public void takePicture(View view) {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }


    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //initialize Error handler
        errorHandler = new ErrorHandler(this);

        // if Image got through cropping
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);


            final Bitmap photo = result.getOriginalBitmap();
            final File photoFile = new File(result.getUri().getPath());
            Log.d("PHOTO",photoFile.getName());
            ImageView preview = findViewById(R.id.preview);
            preview.setImageURI(result.getUri());

            //Store cropped image in gallery
            try {
                MediaStore.Images.Media.insertImage(getContentResolver(), result.getUri().getPath() ,"bbv-" + System.currentTimeMillis() , "cropped Image");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                errorHandler.printError("Something went wrong with your photo. Pls restart the app");
            }

            //Prints a toast to the main act screen
            Toast.makeText(this, "Classifying your image. This may take a few seconds", Toast.LENGTH_LONG).show();

            //run in async task to not fry the app
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        //setup classifier options
                        ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                                .imagesFile(photoFile)
                                .threshold((float) 0.3)
                                .owners(Arrays.asList("me"))
                                .addClassifierId("human_331446926")
                                .build();

                        final ClassifiedImages output = vrClient.classify(classifyOptions).execute();

                        //Some debug prints
                        Log.i("CLASS", output.getImages().get(0).getClassifiers().get(0).getClasses().get(0).getClassName());
                        Log.i("SCORE", output.getImages().get(0).getClassifiers().get(0).getClasses().get(0).getScore().toString());


                        List<ClassResult> classes = output.getImages().get(0).getClassifiers().get(0).getClasses();

                        //iterate over all results and save into String
                       final StringBuffer buffer = new StringBuffer();
                        for(ClassResult result : classes){

                            String name = result.getClassName();
                            String score = Float.toString(result.getScore()*100);
                            Log.i("Class", result.getClassName());
                            Log.i("Score", result.getScore().toString());
                            buffer.append("Class: " + name +  "\n"
                                        + "Score: " + score + "% \n"
                                        + "___________________________________\n");


                        }

                    // Print results in results field
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView detectedObjects =
                                    findViewById(R.id.detected_objects);
                            detectedObjects.setText(buffer.toString());
                        }
                    });



                    } catch (FileNotFoundException e) {
                        errorHandler.printError("File not found. Please try again or restart the App.");
                        e.printStackTrace();
                    } catch (IndexOutOfBoundsException e) {
                        errorHandler.printError("Something went wrong. No Classes found . Please try again or restart the App.");
                        e.printStackTrace();
                    } catch (Exception e) {
                        errorHandler.printError("Something went wrong.Please try again or restart the App.");
                        e.printStackTrace();
                    }
                }
            });
        }
    }



}
