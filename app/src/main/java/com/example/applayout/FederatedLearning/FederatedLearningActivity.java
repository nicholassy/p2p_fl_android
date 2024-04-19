package com.example.applayout.FederatedLearning;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applayout.MainActivity;
import com.example.applayout.R;
import com.example.applayout.Report.ReportActivity;

import org.tensorflow.lite.Interpreter;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FederatedLearningActivity extends AppCompatActivity{
    Context context = this;
    ProgressBar progressBar;
    TextView status, text;
    Button btConfirm;

    private class TrainModelTask extends AsyncTask<Void,Integer,Void>{
        protected void onPreExecute(){
            super.onPreExecute();
            progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
        }

        protected Void doInBackground(Void... voids){
            try (Interpreter anotherInterpreter = new Interpreter(loadModelFile(context.getAssets(),"model.tflite"))) {
                int NUM_EPOCHS = 100;
                int BATCH_SIZE = 100;
                int IMG_HEIGHT = 28;
                int IMG_WIDTH = 28;
                int NUM_TRAININGS = 60000;
                int NUM_BATCHES = NUM_TRAININGS / BATCH_SIZE;

                List<FloatBuffer> trainImageBatches = new ArrayList<>(NUM_BATCHES);
                List<FloatBuffer> trainLabelBatches = new ArrayList<>(NUM_BATCHES);

                // Prepare training batches.
                for (int i = 0; i < NUM_BATCHES; ++i) {
                    ByteBuffer trainImageBuffer = ByteBuffer.allocateDirect(4 * IMG_HEIGHT * IMG_WIDTH).order(ByteOrder.nativeOrder());
                    FloatBuffer trainImages = trainImageBuffer.asFloatBuffer();

                    ByteBuffer trainLabelsBuffer = ByteBuffer.allocateDirect(4 * 10).order(ByteOrder.nativeOrder());
                    FloatBuffer trainLabels = trainLabelsBuffer.asFloatBuffer();

                    // Fill the data values...
                    trainImageBatches.add((FloatBuffer) trainImages.rewind());
                    trainLabelBatches.add((FloatBuffer) trainLabels.rewind());
                }

                // Run training for a few steps.
                float[] losses = new float[NUM_EPOCHS];
                for (int epoch = 0; epoch < NUM_EPOCHS; ++epoch) {
                    for (int batchIdx = 0; batchIdx < NUM_BATCHES; ++batchIdx) {
                        Map<String, Object> inputs = new HashMap<>();
                        inputs.put("x", trainImageBatches.get(batchIdx));
                        inputs.put("y", trainLabelBatches.get(batchIdx));

                        Map<String, Object> outputs = new HashMap<>();
                        FloatBuffer lossBuffer = FloatBuffer.allocate(1);
                        outputs.put("loss", lossBuffer);

                        anotherInterpreter.runSignature(inputs, outputs, "train");
                        final int progressPercentage = (epoch * 100) / NUM_EPOCHS;
                        progressBar.setProgress(progressPercentage);
                        //float lossValue = lossBuffer.get(0);

                        // Record the last loss.
                        if (batchIdx == NUM_BATCHES - 1) losses[epoch] = lossBuffer.get(0);
                    }

                    // Print the loss output for every 10 epochs.
                    if ((epoch + 1) % 10 == 0) {
                        System.out.println(
                                "Finished " + (epoch + 1) + " epochs, current loss: " + losses[epoch]);
                    }
                }
            } catch (IOException e){
                Log.e("ReportActivity","Error",e);
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress){
            super.onProgressUpdate(progress);
            progressBar.setProgress(progress[0]);
        }

        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            progressBar.setVisibility(View.GONE);
            text.setText("Training Completed");
            btConfirm.setVisibility(View.VISIBLE);
        }
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //OnDeviceTraining.onDeviceTraining();
        setContentView(R.layout.activity_federatedlearning);
        text = findViewById(R.id.federated_learning_text);
        status = findViewById(R.id.federated_learning_status);
        btConfirm = findViewById(R.id.btConfirmFederatedLearning);
        status.setText("Learning From: OPPO R11\nML Objective: "+ MainActivity.string +"\nML Model Size: 0.82MB");
        btConfirm.setVisibility(View.GONE);

        btConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ReportActivity.class);
                startActivity(intent);
            }
        });

        new TrainModelTask().execute();

        /*context = this;

        try (Interpreter anotherInterpreter = new Interpreter(loadModelFile(context.getAssets(),"model.tflite"))) {
            int NUM_EPOCHS = 100;
            int BATCH_SIZE = 100;
            int IMG_HEIGHT = 28;
            int IMG_WIDTH = 28;
            int NUM_TRAININGS = 60000;
            int NUM_BATCHES = NUM_TRAININGS / BATCH_SIZE;

            List<FloatBuffer> trainImageBatches = new ArrayList<>(NUM_BATCHES);
            List<FloatBuffer> trainLabelBatches = new ArrayList<>(NUM_BATCHES);

            // Prepare training batches.
            for (int i = 0; i < NUM_BATCHES; ++i) {
                ByteBuffer trainImageBuffer = ByteBuffer.allocateDirect(4 * IMG_HEIGHT * IMG_WIDTH).order(ByteOrder.nativeOrder());
                FloatBuffer trainImages = trainImageBuffer.asFloatBuffer();

                ByteBuffer trainLabelsBuffer = ByteBuffer.allocateDirect(4 * 10).order(ByteOrder.nativeOrder());
                FloatBuffer trainLabels = trainLabelsBuffer.asFloatBuffer();

                // Fill the data values...
                trainImageBatches.add((FloatBuffer) trainImages.rewind());
                trainLabelBatches.add((FloatBuffer) trainLabels.rewind());
            }

            // Run training for a few steps.
            float[] losses = new float[NUM_EPOCHS];
            for (int epoch = 0; epoch < NUM_EPOCHS; ++epoch) {
                for (int batchIdx = 0; batchIdx < NUM_BATCHES; ++batchIdx) {
                    Map<String, Object> inputs = new HashMap<>();
                    inputs.put("x", trainImageBatches.get(batchIdx));
                    inputs.put("y", trainLabelBatches.get(batchIdx));

                    Map<String, Object> outputs = new HashMap<>();
                    FloatBuffer lossBuffer = FloatBuffer.allocate(1);
                    outputs.put("loss", lossBuffer);

                    anotherInterpreter.runSignature(inputs, outputs, "train");

                    //float lossValue = lossBuffer.get(0);

                    // Record the last loss.
                    if (batchIdx == NUM_BATCHES - 1) losses[epoch] = lossBuffer.get(0);
                }

                // Print the loss output for every 10 epochs.
                if ((epoch + 1) % 10 == 0) {
                    System.out.println(
                            "Finished " + (epoch + 1) + " epochs, current loss: " + losses[epoch]);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }*/
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
