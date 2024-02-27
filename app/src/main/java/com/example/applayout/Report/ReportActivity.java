package com.example.applayout.Report;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
    private Context context;
    Button btStart;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        modelDetails = findViewById(R.id.modelDetails);
        modelDetails.setText("Model Name: model.tflite\nLast Trained On: 30/01/2024");

        context = this;

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
