package com.mobile.gemede;


import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Locale;





import com.soundcloud.android.crop.Crop;

import java.io.File;



public class Start extends AppCompatActivity {

    //TextToSpeech Api için bir nesne tanımladık. mTTS isminde.
    private TextToSpeech mTTS ;
    // Butonları tanımladık
    private Button inceptionFloat;
    private Button yonerge_button;


    public static final int REQUEST_PERMISSION = 300;


    public static final int REQUEST_IMAGE = 100;

    // Kameradan alınan uri dosyasını tutmak için bir değişken oluşturduk.
    private Uri imageUri;

    @Override//uygulama oluşturulurken gerçekleştirilecek komutlar.
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);



        // Kameranın kullanımı için kullanıcıdan izin istiyoruz.
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.CAMERA}, REQUEST_PERMISSION);
        }

        // Kullanıcının cihazına veri yazabilmek için kullanıcıdan izin istiyoruz.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        // Kullanıcının cihazından veri okumak için izin istiyoruz.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (status == TextToSpeech.SUCCESS) {

                    int result = mTTS.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {

                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }

            }
        });

        yonerge_button = (Button)findViewById(R.id.yonerge_button);
        yonerge_button.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View view) {

                                                  speak("Devam etmek için ekranın alt yarısında bulunan başlat tuşuna basmalısınız." +
                                                          "ardından kamera arayüzünüze yönlendirileceksiniz.Fotoğrafı çekip onayladıktan " +
                                                          "sonra ekranın sağ ortasında bulunan " +
                                                          "tuşa basarak cismi belirleyebilirsiniz.");
                                                  String speech2 = " Cisim tanımlaması başarı ile sonuçlandıktan sonra başka bir fotoğraf çekmek için ekranın" +
                                                          " solunda bulunan geri butonuna ," +
                                                          "tekrar dinlemek için en altta bulunan tekrar dinleme butonuna basabilirsiniz.";
                                                  speak(speech2);

                                              }
                                          });

        // on click for inception float model
        inceptionFloat = (Button)findViewById(R.id.inception_float);
        inceptionFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTTS.stop();
                openCameraIntent();



            }
        });
    }

    // Kamera arayüzü açılır.
    private void openCameraIntent(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Yeni Fotoğraf");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Bu Ne Acaba APP");
        // Çekilen fotoğrafın kayıt edileceği yer.
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    // Kullanıcının kamera ve verileri okuma yazma izni verip vermediği kontrol edilir.Vermedi ise bir bildirimle uygulama kapatılır.
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(getApplicationContext(),"This application needs read, write, and camera permissions to run. Application now closing.",Toast.LENGTH_LONG);
                System.exit(0);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        // Fotoğrafı çektiysek ,alınan uri kare olacak şekilde kesilir (299*299), Ardından "Classify" sınıfına yollanır.
        if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            try {
                Intent i = new Intent(Start.this, Classify.class);
                i.putExtra("resID_uri", imageUri);
                startActivity(i);
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }
    private void speak(String text){
        mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
    }
}
