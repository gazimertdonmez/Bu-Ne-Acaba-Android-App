package com.mobile.gemede;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

public class Classify extends AppCompatActivity {

    // rgb dönüştürme için ön ayarlar
    private static final int RESULTS_TO_SHOW = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    //Mtts adlı bir nesne tanımlıyoruz. Tts api'si için.
    private TextToSpeech mTTS;


    // model interpreter ayarları
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private Interpreter tflite;
    private Interpreter depth_tflite;
    // model için tüm olası etiketleri tutar
    private List<String> labelList;
    // seçilen görüntü verilerini bayt olarak saklıyoruz
    private ByteBuffer imgData = null;
    private ByteBuffer depth_image = null;
    // belirlenmemiş grafikler için her etiketin olasılıklarını tutar
    private float[][] labelProbArray = null;
    // belirlenebilmiş grafikler için her etiketin olasılıklarını tutar
    private byte[][] labelProbArrayB = null;
    // En yüksek olasılıklara sahip etiketleri tutan bir dizi oluşturduk
    private String[] topLables = null;
    // en yüksek olasılıkları tutan dizi
    private String[] topConfidence = null;


    private String classification_model = "inception_float.tflite";
    private String depth_model = "optimized_pydnet++.tflite";

    // Modelimizin tespit etmesi için girdi olarak kullanacağımız fotoğrafın  kesilme işlemi ; (299*299)
    private int DIM_IMG_SIZE_X = 299;
    private int DIM_IMG_SIZE_Y = 299;
    private int DIM_PIXEL_SIZE = 3;

    // Görüntünün verilerini tutan bir dizi oluşturduk.
    private int[] intValues;


    private ImageView selected_image;
    private Button classify_button;
    private Button back_button;
    private Button listen_button;
    private TextView label1;
    private TextView label2;
    private TextView label3;
    private TextView Confidence1;
    private TextView Confidence2;
    private TextView Confidence3;

    // Elimizde ki en iyi sonuç için öncelik sırası ;
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Burada tts api sini başlatıyoruz , bir tts tanımlıyoruz.
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (status == TextToSpeech.SUCCESS) {

                    int result = mTTS.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        classify_button.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }





            }
        });



        // görüntü verilerini tutacak olan dizi
        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

        super.onCreate(savedInstanceState);

        try{
            tflite = new Interpreter(loadModelFile(classification_model), tfliteOptions);
            labelList = loadLabelList();
            depth_tflite = new Interpreter(loadModelFile(depth_model), tfliteOptions);
        } catch (Exception ex){
            ex.printStackTrace();
        }

        imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);

        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];

        setContentView(R.layout.activity_classify);


        // Çıktıyı label1 alanına yazdırıyoruz.
        label1 = (TextView) findViewById(R.id.label1);

        // Çektiğimiz fotoğrafı gösteriyoruz.
        selected_image = (ImageView) findViewById(R.id.selected_image);


        topLables = new String[RESULTS_TO_SHOW];
        // en yüksek olasılıkları tutmak için dizi
        topConfidence = new String[RESULTS_TO_SHOW];

        // Kullanıcının farklı bir fotoğraf çekmesi, seçmesi için önceki aktiviteye dönüş.
        back_button = (Button)findViewById(R.id.back_button);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTTS.stop();//Burada durdurma işlemi yapmazsak tuşa basılmasına rağmen sesli
                //devam ederdi.
                Intent i = new Intent(Classify.this, Start.class);
                startActivity(i);
            }


        });
        listen_button = (Button)findViewById(R.id.listen_button);
        listen_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                label1.setText("Bu bir "+topLables[2]);

                String speech = "Bu bir  ";
                speech = speech + topLables[2];
                speak(speech);


            }


        });


        // Ekranda görüntülenmiş görüntünün sınıflandırılması
        classify_button = (Button)findViewById(R.id.classify_image);
        classify_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // imageView'dan mevcut bitmap'i al
                Bitmap bitmap_orig = ((BitmapDrawable)selected_image.getDrawable()).getBitmap();
                // Görüntüyü merkezden kırp
                Bitmap cropped_bmp = cropBitmap(bitmap_orig);
                // Bitmap i modelde kullanabilmemiz için gereken boyuta kırpıyoruz.
                Bitmap bitmap = getResizedBitmap(cropped_bmp, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);

                // bitmap'i bayt dizisine dönüştür
                convertBitmapToByteBuffer(bitmap);
                // İşlemi yürütüyoruz.
                tflite.run(imgData, labelProbArray);




                get_result();
                //gerekli fonksiyonu çalıştırdık.

            }
        });

        // imageView'da göstermek için önceki aktiviteden görüntüyü alıyoruz.
        Uri uri = (Uri)getIntent().getParcelableExtra("resID_uri");
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            selected_image.setImageBitmap(bitmap);
            // Fotoğrafın doğru eksende gözükmesi için 90 derece döndürüyoruz.
            selected_image.setRotation(selected_image.getRotation() + 90);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // dosyadan tflite graphını içeri aktarıyoruz
    private MappedByteBuffer loadModelFile(String model_name) throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(model_name);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // bitmap'i byte dizisine dönüştürüyoruz döngüde kullanmak için
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // tüm pikseller arasında döngü çalıştırıyoruz
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
    }

    // labels.txt dosyamızda ki etiketleri bir diziye aktarıyoruz.
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(this.getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }


    private void get_result() {
        // tüm sonuçları öncelik sırasına ekle
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i]));

            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }

        // öncelik kuyruğundan en iyi sonuçları alıyoruz.
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            topLables[i] = label.getKey();
            topConfidence[i] = String.format("%.0f%%", label.getValue() * 100);
        }

        // Çıktı İşlemleri
        label1.setText("Bu bir " + topLables[2]);

        String speech = "Bu bir  ";
        speech = speech + topLables[2];
        speak(speech);

    }










    //bitmap crop
    public Bitmap cropBitmap(Bitmap srcBmp){

        Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()){

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        }else{

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }
        return dstBmp;
    }

    // bitmap'i verilen boyutlara yeniden boyutlandırır
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private void speak(String text){
        mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
    }
}
