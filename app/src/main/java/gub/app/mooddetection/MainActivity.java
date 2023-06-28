package gub.app.mooddetection;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {


    String modelPath = "lite-model_yamnet_classification_tflite_1.tflite";
    float probabilityThreshold = 0.3f;
    AudioClassifier classifier;
    private TensorAudio tensor;
    private AudioRecord record;
    private TimerTask timerTask;


    public final static int REQUEST_RECORD_AUDIO = 2033;

    protected TextView outputTextView;
    protected ImageButton startRecordingButton;
    protected ImageButton stopRecordingButton;


    ListView listView;
    ArrayList<String> list = new ArrayList<>();
    ArrayAdapter<String> adapter;

    AlertDialog.Builder dialogBuilder;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputTextView = findViewById(R.id.textViewOutput);
        startRecordingButton = findViewById(R.id.buttonStartRecording);
        stopRecordingButton = findViewById(R.id.buttonStopRecording);

        listView = findViewById(R.id.listView);

        stopRecordingButton.setEnabled(false);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }


    }

    public void onStartRecording(View view) {

        Toast.makeText(getApplicationContext(), "Audio Recording Started", Toast.LENGTH_SHORT).show();
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setEnabled(true);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        // Loading the model from the assets folder
        try {
            classifier = AudioClassifier.createFromFile(this, modelPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Creating an audio recorder
        tensor = classifier.createInputTensorAudio();
        // showing the audio recorder specification
        TensorAudio.TensorAudioFormat format = classifier.getRequiredTensorAudioFormat();
        // Creating and start recording
        record = classifier.createAudioRecord();
        record.startRecording();


        timerTask = new TimerTask() {
            int c = 0;
            String result = "";

            @Override
            public void run() {
                // Classifying audio data
                // val numberOfSamples = tensor.load(record)
                // val output = classifier.classify(tensor)
                int numberOfSamples = tensor.load(record);
                List<Classifications> output = classifier.classify(tensor);

                // Filtering out classifications with low probability
                List<Category> finalOutput = new ArrayList<>();
                for (Classifications classifications : output) {
                    for (Category category : classifications.getCategories()) {
                        if (category.getScore() > probabilityThreshold) {
                            finalOutput.add(category);
                        }
                    }
                }

                // Sorting the results
                Collections.sort(finalOutput, (o1, o2) -> (int) (o1.getScore() - o2.getScore()));

                // Creating a multiline string with the filtered results
                StringBuilder outputStr = new StringBuilder();
                for (Category category : finalOutput) {
                    outputStr.append(category.getLabel())
                            .append(": ").append(category.getScore()).append("\n");
                }

                // Updating the UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (finalOutput.isEmpty()) {
                            outputTextView.setText("Don't understand what you mean");
                        } else {
                            c++;

                            result=outputStr.toString();
                            if(result.contains("Speech") || result.contains("Conversation")  || result.contains("Narration")
                                    ||result.contains("monologue")|| result.contains("Child speech")||result.contains("kid speaking")||result.contains("Humming") ||
                                    result.contains("Whispering")){
                                result="neutral";
                                outputTextView.setText(c + " : " + result.toString());

                                //neutral
                                list.add(result.toString());
                                showAlertDialog(R.layout.neutral_layout);


                            }
                            if (result.contains("Screaming") || result.contains("Children shouting")
                                    || result.contains("Yell") || result.contains("Shout")) {
                                result="Angry";
                                outputTextView.setText(c + " : " + result.toString());

                                //Angry
                                list.add(result.toString());
                                showAlertDialog(R.layout.angry_layout);

                            }

                            if(result.contains("laugh")|| result.contains("cheering") || result.contains("chuckle") ||result.contains("Belly laugh") ||
                                    result.contains("Snicker") ||result.contains("Laughter") || result.contains("Baby laughter") ||result.contains("Giggle")){
                                      result="Happy";
                                outputTextView.setText(c + " : " + result.toString());
                               //Happy
                                list.add(result.toString());
                                showAlertDialog(R.layout.happy_layout);


                            }
                            if(result.contains("Crying") || result.contains("sobbing") || result.contains("Baby cry") || result.contains("infant cry") ||
                                    result.contains("sigh") ||result.contains("Wail") || result.contains("moan") || result.contains("Whimper")){
                                //sad
                                result="sad";
                                outputTextView.setText(c + " : " + result.toString());
                                list.add(result.toString());
                                showAlertDialog(R.layout.sad_layout);

                            }

                            adapter.notifyDataSetChanged();
                            listView.setAdapter(adapter);
                            listView.setSelection(listView.getAdapter().getCount() - 1);


                        }
                    }
                });
            }
        };

        new Timer().scheduleAtFixedRate(timerTask, 1, 1200);

    }

    public void onStopRecording(View view) {

        Toast.makeText(getApplicationContext(), "Audio Recording Stopped", Toast.LENGTH_SHORT).show();
        startRecordingButton.setEnabled(true);
        stopRecordingButton.setEnabled(false);
        timerTask.cancel();
        record.stop();
    }
    private void showAlertDialog(int layout){
        dialogBuilder=new AlertDialog.Builder(MainActivity.this);
        View layoutView = getLayoutInflater().inflate(layout, null);
        Button dialogButton = layoutView.findViewById(R.id.btnDialog);
        dialogBuilder.setView(layoutView);
        alertDialog = dialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                alertDialog.cancel();
            }
        });

    }


}