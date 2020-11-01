package com.gtr.easyfuel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gtr.easyfuel.Class.SaveData;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    private EditText meterText,meterAmountText;
    private ImageView meterImage,meterAmountImage,slipImage;
    private Button saveButton;
    private ProgressDialog progressDialog;

    private  static final int CAMERA_REQUEST_CODE = 200;
    private  static final int STORAGE_REQUEST_CODE = 400;
    private  static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE= 1001;
    private static final int AMOUNT_PICK_CAMERA_CODE= 1002;
    private static final int SLIP_PICK_CAMERA_CODE= 1010;

    private String cameraPermission[];
    private String storagePermission[];


    int imageNo;
    private String currentPhotoPath;
    Bitmap bitmap;

    private AdView mAdView;

    //Uri
    Uri resultUri,resultUri2;

    //Firebase
    StorageReference mStorageRef;
    DatabaseReference dbRef;
    SaveData saveData;
    byte[] data1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        progressDialog = new ProgressDialog(ProfileActivity.this);
        progressDialog.setMessage("অপেক্ষা করুন...");
        progressDialog.setCancelable(false);

        meterText= findViewById(R.id.meterText);
        meterImage = findViewById(R.id.meterImage);

        meterAmountImage = findViewById(R.id.meterAmountImage);
        meterAmountText= findViewById(R.id.meterAmountText);
        slipImage = findViewById(R.id.slipImage);

        saveButton = findViewById(R.id.saveButton);

        //Firebase initialise
        saveData=new SaveData();
        mStorageRef= FirebaseStorage.getInstance().getReference("Images");
        dbRef= FirebaseDatabase.getInstance().getReference().child("SaveData");

        //AddView
        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-6949704799119881/2209175499");

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        cameraPermission= new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};

        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //meterImage show
        meterImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageImportDialog();
            }
        });

        meterAmountImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                meterTotalShowImageDialog();
            }
        });

        slipImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName="photo";
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File imageFile= null;
                try {
                    imageFile = File.createTempFile(fileName,".jpg",storageDirectory);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                currentPhotoPath = imageFile.getAbsolutePath();

                Uri imageUri= FileProvider.getUriForFile(ProfileActivity.this,
                        "com.gtr.easyfuel.fileprovider",imageFile);

                Intent intent= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,SLIP_PICK_CAMERA_CODE);

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                checked();
                /*firebaseUpload();*/
            }
        });
    }

    private void firebaseUpload() {
        String octaneUploadImage,amountUploadImage,slipUploadImage;
        octaneUploadImage= System.currentTimeMillis()+"."+resultUri;
        amountUploadImage= System.currentTimeMillis()+"."+resultUri2;
        slipUploadImage= System.currentTimeMillis()+"."+data1;

        saveData.setOctaneText(meterText.getText().toString().trim());
        saveData.setAmountText(meterAmountText.getText().toString().trim());
        saveData.setOctaneImage(octaneUploadImage);
        saveData.setAmountImage(amountUploadImage);
        saveData.setSlipImage(slipUploadImage);
        dbRef.push().setValue(saveData);

        StorageReference meterOctaneImageRef=mStorageRef.child("octaneUploadImage");
        meterOctaneImageRef.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ProfileActivity.this, "Upload Done", Toast.LENGTH_SHORT).show();
            }
        });
        StorageReference meterAmountImageRef=mStorageRef.child("amountUploadImage");
        meterAmountImageRef.putFile(resultUri2).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ProfileActivity.this, "Upload Done", Toast.LENGTH_SHORT).show();
            }
        });
        StorageReference mountainImagesRef = mStorageRef.child("slipUploadImage");
        UploadTask uploadTask = mountainImagesRef.putBytes(data1);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...

                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Upload Done", Toast.LENGTH_SHORT).show();

                meterImage.setImageBitmap(null);
                meterAmountImage.setImageBitmap(null);
                slipImage.setImageBitmap(null);

                meterImage.setBackgroundResource(R.drawable.litres_image);
                meterAmountImage.setBackgroundResource(R.drawable.taka_image);
                slipImage.setBackgroundResource(R.drawable.payslip);


                meterText.setText("");
                meterAmountText.setText("");
            }
        });
    }

    private void checked() {
        String meterImageText = meterText.getText().toString().trim();
        String meterImageAmountText = meterAmountText.getText().toString().trim();
        if (meterImageText.isEmpty()) {
            meterText.setError("খালি প্রযোজ্ নয়");
            meterText.requestFocus();
            return;

        }else if (meterImageAmountText.isEmpty()) {
            meterAmountText.setError("খালি প্রযোজ্ নয়");
            meterAmountText.requestFocus();
            return;
        }else{
            firebaseUpload();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        /*if(id==R.id.addImage){
            *//*showImageImportDialog();*//*
        }*/
        if (id==R.id.settings){
            Toast.makeText(this, "Setting", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageImportDialog() {
        if (!checkCameraPermission()){

            requestCameraPermission();

        }else{
            pickCamera();
        }
    }

    private void meterTotalShowImageDialog() {
        if (!checkCameraPermission()){

            requestCameraPermission();

        }else{
            totalAmountPick();
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }


    private void pickCamera() {

        imageNo=0;
        getImageClick();



    }
    private void totalAmountPick() {

        imageNo=1;
        getImageClick2();
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean cameraAccepted= grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean  writeStorageAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }else{
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    //select image form camera or gallery
    public void getImageClick() {
        CropImage.startPickImageActivity(this);


        /*Intent cameraIntent= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri2);
        startActivityForResult(cameraIntent,0);*/

    }
    public void getImageClick2() {
        CropImage.startPickImageActivity(this);


        /*Intent cameraIntent= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri2);
        startActivityForResult(cameraIntent,0);*/

    }

    //this for Crope Image
    private void startCropImageActivity(Uri imageUri) {
        CropImage.activity(imageUri)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            //start Crope image
            startCropImageActivity(imageUri);
        }

        // handle result of CropImageActivity
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            resultUri = result.getUri();
            resultUri2 = result.getUri();
            switch (imageNo){
                case (0):
                    //here you have resultUri for save image or preview as image1
                    meterImage.setImageURI(resultUri);

                    BitmapDrawable bitmapDrawable = (BitmapDrawable)meterImage.getDrawable();
                    Bitmap bitmap = bitmapDrawable.getBitmap();

                    TextRecognizer recognizer= new TextRecognizer.Builder(getApplicationContext()).build();

                    if (!recognizer.isOperational()){
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }else{
                        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                        SparseArray<TextBlock> items = recognizer.detect(frame);
                        StringBuilder sb= new StringBuilder();
                        for (int i=0;i<items.size();i++){
                            TextBlock myItem = items.valueAt(i);
                            sb.append(myItem.getValue());
                            sb.append("\n");
                        }
                        meterText.setText(sb.toString());
                    }
                    break;
                case (1):
                    //here you have resultUri for save image or preview as image1
                    meterAmountImage.setImageURI(resultUri2);

                    BitmapDrawable bitmapDrawable1 = (BitmapDrawable)meterAmountImage.getDrawable();
                    Bitmap bitmap1 = bitmapDrawable1.getBitmap();

                    TextRecognizer recognizer1= new TextRecognizer.Builder(getApplicationContext()).build();

                    if (!recognizer1.isOperational()){
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }else{
                        Frame frame = new Frame.Builder().setBitmap(bitmap1).build();
                        SparseArray<TextBlock> items = recognizer1.detect(frame);
                        StringBuilder sb= new StringBuilder();
                        for (int i=0;i<items.size();i++){
                            TextBlock myItem = items.valueAt(i);
                            sb.append(myItem.getValue());
                            sb.append("\n");
                        }
                        meterAmountText.setText(sb.toString());
                    }
                    break;
                case (3):
                    //here you have resultUri for save image or preview as image1
            }
        }
        if (resultCode==RESULT_OK){
            if (requestCode==SLIP_PICK_CAMERA_CODE){
                /*Uri filePath= data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                    slipImage.setImageBitmap(bitmap);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }*/


                bitmap= BitmapFactory.decodeFile(currentPhotoPath);

                Bitmap bOutput;
                float degrees = 90;//rotation degree
                Matrix matrix = new Matrix();
                matrix.setRotate(degrees);
                final float densityMultiplier = getResources().getDisplayMetrics().density;
                int h = (int) (250 * densityMultiplier);
                int w = (int) (h * bitmap.getWidth() / ((double) bitmap.getHeight()));

                bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
                bOutput = Bitmap.createBitmap(bitmap,0,0,w,h, matrix, true);

                slipImage.setImageBitmap(bOutput);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bOutput.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                data1 = baos.toByteArray();
            }
        }

    }
}