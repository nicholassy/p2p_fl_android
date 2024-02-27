package com.example.applayout.FederatedLearning;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applayout.MainActivity;
import com.example.applayout.R;
import com.example.applayout.Report.ReportActivity;

import org.tensorflow.lite.Interpreter;

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
    Context context;
    ProgressBar progressBar;
    TextView status;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_federatedlearning);

        status = findViewById(R.id.federated_learning_status);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        status.setText("Learning From: [device name]\nML Objective: "+ MainActivity.string +"\nML Model Size: [file size in MB]");

        context = this;

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
        }
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
