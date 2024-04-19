package com.example.applayout.Report;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.checkerframework.checker.units.qual.A;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applayout.Communications.CommunicationActivity;
import com.example.applayout.R;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {
    TextView modelDetails;
    ListView lvReport;
    private Context context;
    Button btStart;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        modelDetails = findViewById(R.id.modelDetails);
        modelDetails.setText("Model Name: model.tflite\nLast Trained On: 20/03/2024\n");

        lvReport = findViewById(R.id.lvReport);
        ArrayList<String> reportList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reportList);
        lvReport.setAdapter(adapter);

        context = this;

        FloatBuffer output = null;

        try (Interpreter anotherInterpreter = new Interpreter(loadModelFile(context.getAssets(),"model.tflite"))) {
            // Restore the weights from the checkpoint file.

            int NUM_TESTS = 4;
            ByteBuffer testImageBuffer = ByteBuffer.allocateDirect(4 * 28 * 28).order(ByteOrder.nativeOrder());
            FloatBuffer testImages = testImageBuffer.asFloatBuffer();

            ByteBuffer testLabelsBuffer = ByteBuffer.allocateDirect(4 * 10 * 4).order(ByteOrder.nativeOrder());
            output = testLabelsBuffer.asFloatBuffer();

            // Fill the test data.

            // Run the inference.
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("x", testImages.rewind());
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("output", output);
            anotherInterpreter.runSignature(inputs, outputs, "infer");
            output.rewind();

            // Process the result to get the final category values.
            int[] testLabels = new int[NUM_TESTS];
            float[] confidenceScore = new float[NUM_TESTS];
            for (int i = 0; i < NUM_TESTS; ++i) {
                int maxIndex = 0; // Start assuming the first index has the maximum value
                float maxValue = output.get(i * 10); // Assume first element is the largest initially
                for (int j = 1; j < 10; ++j) {
                    float currentValue = output.get(i * 10 + j);
                    if (currentValue > maxValue) {
                        maxValue = currentValue;
                        maxIndex = j;
                    }
                }
                testLabels[i] = maxIndex;
                confidenceScore[i] = maxValue;

                reportList.add("Test " + i + ": Label = " + maxIndex + ", Confidence = " + String.format("%.2f", maxValue));

                //Log.d("ReportActivity", "Test " + i + ": Label = " + maxIndex + ", Confidence = " + maxValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ex){
            if (output != null) {
                Log.e("ReportActivity", "Error accessing buffer at position: " + output.position() + " with limit: " + output.limit(), ex);
            }
            throw ex;
        }

        /*Model.Options options;
        CompatibilityList compatList = new CompatibilityList();

        if(compatList.isDelegateSupportedOnThisDevice()){
            options = new Model.Options.Builder().setDevice(Model.Device.GPU).build();
        } else {
            options = new Model.Options.Builder().setNumThreads(4).build();
        }
            //Export the trained weights as checkpoint file
            File outputFile = new File(getFilesDir(), "checkpoint.ckpt");
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("checkpoint_path", outputFile.getAbsolutePath());
            Map<String, Object> outputs = new HashMap<>();
            anotherInterpreter.runSignature(inputs, outputs, "save");
            }

            //model inference
            ByteBuffer inputBuffer = prepareInputData();
            FloatBuffer outputBuffer = FloatBuffer.allocate(360000);
            Map<Integer, Object> outputs = new HashMap<>();
            int outputTensorIndex = 0;
            outputs.put(outputTensorIndex, outputBuffer);
            anotherInterpreter.runForMultipleInputsOutputs(new Object[]{inputBuffer}, outputs);
            float[] results = new float[4];
            outputBuffer.get(results);
            Log.e("Output: ", Arrays.toString(outputBuffer.array()));
            anotherInterpreter.close();
        } catch (IOException e){
            e.printStackTrace();
        }*/

        btStart = findViewById(R.id.btStartFL);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), CommunicationActivity.class);
                startActivity(intent);
            }
        });
    }

    private ByteBuffer prepareInputData() {
        int batchSize = 1;
        int inputHeight = 300;
        int inputWidth = 300;
        int channelCount = 3;

        int inputSize = batchSize * inputHeight * inputWidth * channelCount;

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(inputSize);
        inputBuffer.order(ByteOrder.nativeOrder());

        return inputBuffer;
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
