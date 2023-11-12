package com.textrecogination;

import static com.textrecogination.R.drawable.rectangle;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.balsikandar.crashreporter.CrashReporter;
import com.canhub.cropper.CropImageView;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView img;
    private TextView textview;
    private Bitmap imageBitmap,  btmap;
    LinearLayout main, im_layout;



    CropImageView cropImageView;
    final int REQUEST_STORAGE = 101;
    final int REQUEST_CAMERA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashReporter.initialize(this);
        img = findViewById(R.id.image);
        textview = findViewById(R.id.text);
        Button snapBtn = findViewById(R.id.snapbtn);
        ScrollView sc = findViewById(R.id.sc);
        Button detectBtn = findViewById(R.id.detectbtn);
        Button storeImage = findViewById(R.id.storeimage);
        Button crop_image_button = findViewById(R.id.croped_image);
        main = findViewById(R.id.main);
        im_layout = findViewById(R.id.im_layout);
        cropImageView = findViewById(R.id.cropImageView);

        im_layout.setVisibility(View.GONE);
        sc.setBackgroundResource(R.drawable.strock_2);
        detectBtn.setBackgroundResource(rectangle);
        storeImage.setBackgroundResource(R.drawable.rectangle);
        snapBtn.setBackgroundResource(R.drawable.rectangle);

        //ON click listeners--------------------------------------

        storeImage.setOnClickListener(v -> {
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(i, "select picture"), REQUEST_STORAGE);

        });

        snapBtn.setOnClickListener(v -> {
            try {
                dispatchTakePictureIntent();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        detectBtn.setOnClickListener(v -> {
            detectTxt();
            cropImageView.clearImage();
        });

        crop_image_button.setOnClickListener(v -> {
            btmap = cropImageView.getCroppedImage();
            img.setImageBitmap(btmap);
            main.setVisibility(View.VISIBLE);
            im_layout.setVisibility(View.INVISIBLE);
        });
    }

    private void dispatchTakePictureIntent() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create a File to save the image
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.textrecogination.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        }
    }

    private  String currentPhotoPath;
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CrashReporter.initialize(this);
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            if (currentPhotoPath != null) {
                imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                cropImageView.setImageBitmap(imageBitmap);
                textview.setText("");
                main.setVisibility(View.GONE);
                im_layout.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "No Image", Toast.LENGTH_SHORT).show();
            }

        }
        if (resultCode == RESULT_OK && requestCode == REQUEST_STORAGE) {
            Uri imageuri = data.getData();
            if (imageuri != null) {
                try {
                    main.setVisibility(View.GONE);
                    im_layout.setVisibility(View.VISIBLE);
                    textview.setText("");
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageuri);
                    cropImageView.setImageBitmap(imageBitmap);

                } catch (Exception e) {

                }
            }else{
                Toast.makeText(this, "No Image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void detectTxt() {
        if (btmap != null) {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(btmap);
            FirebaseVisionTextDetector detector = FirebaseVision.getInstance().getVisionTextDetector();
            detector.detectInImage(image).addOnSuccessListener(this::processTxt).addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Fail to detect the text from image..", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No Image selected", Toast.LENGTH_SHORT).show();
        }

    }

    private void processTxt(FirebaseVisionText text) {
        List<FirebaseVisionText.Block> blocks = text.getBlocks();
        if (blocks.size() == 0) {
            Toast.makeText(MainActivity.this, "No Text ", Toast.LENGTH_LONG).show();
        }
        for (FirebaseVisionText.Block block : text.getBlocks()) {
            String txt = block.getText();
            textview.setText(txt);
        }
    }

}