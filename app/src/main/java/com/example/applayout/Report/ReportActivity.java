package com.example.applayout.Report;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applayout.Communications.CommunicationActivity;
import com.example.applayout.R;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {
    private Context context;
    Button btStart;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        context = this;

        try (Interpreter anotherInterpreter = new Interpreter(loadModelFile(context.getAssets(),"mobilenetv1.tflite"))) {
            ByteBuffer inputBuffer = prepareInputData();
            FloatBuffer outputBuffer = FloatBuffer.allocate(40);
            Map<Integer, Object> outputs = new HashMap<>();
            int outputTensorIndex = 0;

            outputs.put(outputTensorIndex, outputBuffer);

            anotherInterpreter.runForMultipleInputsOutputs(new Object[]{inputBuffer}, outputs);
            float[] results = new float[4];
            outputBuffer.get(results);
        } catch (IOException e){
            e.printStackTrace();
        }

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
