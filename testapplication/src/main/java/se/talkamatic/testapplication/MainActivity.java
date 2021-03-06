package se.talkamatic.testapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import se.talkamatic.frontend.IBackendStatusListener;
import se.talkamatic.frontend.IEventListener;
import se.talkamatic.frontend.Language;
import se.talkamatic.frontend.TdmConnector;
import se.talkamatic.frontend.asr.AsrListenerAdapter;
import se.talkamatic.frontend.asr.AsrRecognitionHypothesis;
import se.talkamatic.frontend.asr.IAsrListener;
import se.talkamatic.frontend.gsonmessages.ResponseMessage.Parameter;

public class MainActivity extends AppCompatActivity {

    private TdmConnector tdmConnector;
    private enum AsrState {
        DISABLED,
        IDLE,
        REQUESTED_TO_START_LISTENING,
        LISTENING,
        REQUESTED_TO_STOP_LISTENING
    }
    private final String SERVER_ADDRESS = "localhost";
    private final int SERVER_PORT = 9090;
    private AsrState asrState;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();

        tdmConnector = TdmConnector.createTdmConnector(context);
        tdmConnector.setLanguage(Language.ENGLISH);

        registerRecognitionListener();
        registerEventListener();
        registerBackendStatusListener();

        setupConnectButton();
        setupDisconnectButton();
        setupPttButton();
        setAsrState(AsrState.DISABLED);
    }

    private void setupConnectButton() {
        final Button button = findViewById(R.id.connectButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tdmConnector.connect("http://"+SERVER_ADDRESS+":"+SERVER_PORT+"/interact");
            }
        });
    }

    private void setupDisconnectButton() {
        final Button button = findViewById(R.id.disconnectButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tdmConnector.disconnect();
            }
        });
    }

    private void setupPttButton() {
        final Button button = findViewById(R.id.pttButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handlePttButtonClicked();
            }
        });
    }

    private void handlePttButtonClicked() {
        switch(asrState) {
            case IDLE:
                setAsrState(AsrState.REQUESTED_TO_START_LISTENING);
                tdmConnector.startListening();
                break;

            case LISTENING:
                setAsrState(AsrState.REQUESTED_TO_STOP_LISTENING);
                tdmConnector.stopListening();
                break;

            default:
                Log.e(TAG, "handlePttButtonClicked(): unexpected AsrState " + asrState);
        }
    }

    private void registerBackendStatusListener() {
        IBackendStatusListener backendStatusListener = new IBackendStatusListener() {
            @Override
            public void onOpen() {
                displayBackendStatus("Opened");
                Log.d("backendStatusListener", "onOpen()");
                setAsrState(AsrState.IDLE);
            }

            @Override
            public void onClose() {
                Log.e("backendStatusListener", "onClose()");
                setAsrState(AsrState.DISABLED);
                displayBackendStatus("Closed");
            }

            @Override
            public void onError(String reason) {
                Log.e("backendStatusListener", "onError(" + reason + ")");
                displayBackendStatus("Error: " + reason);
            }
        };
        tdmConnector.registerBackendStatusListener(backendStatusListener);
    }

    private void displayBackendStatus(String status) {
        final TextView backendStatusView = findViewById(R.id.backendStatus);
        String oldTextPrependedWithNewStatus = status + "\n" + backendStatusView.getText();
        updateTextViewInUiThread(backendStatusView, oldTextPrependedWithNewStatus);
    }

    private void updateTextViewInUiThread(final TextView view, final String text) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                view.setText(text);
            }
        };
        runOnUiThread(runnable);
    }

    private void registerEventListener() {
        IEventListener eventListener = new IEventListener() {
            @Override
            public void onAction(String ddd, String name, Map<String, Parameter> args) {
                Log.d("eventListener", "onPerformAction(DDD: " + ddd + ", name: " + name + ", args: " + args + ")");
                final TextView performActionView = findViewById(R.id.performAction);
                String text = name + ": " + args.toString();
                updateTextViewInUiThread(performActionView, text);

                if (name.equals("call")) {
                    Parameter phone_number = args.get("phone_number_to_call");
                    Intent intent = new Intent(Intent.ACTION_DIAL,
                            Uri.fromParts("tel", phone_number.getGrammar_entry(), null));
                    startActivity(intent);
                }
            }

            @Override
            public void onSystemUtteranceToSpeak(String utterance) {
                Log.d("eventListener", "onSystemUtteranceToSpeak(" + utterance + ")");
                final TextView systemUtteranceView = findViewById(R.id.systemUtterance);
                updateTextViewInUiThread(systemUtteranceView, utterance);
            }

            @Override
            public void onNextPassivityDuration(Long milliseconds) {
                Log.d("eventListener", "onNextPassivityDuration(" + milliseconds + ")");
            }
        };
        tdmConnector.registerEventListener(eventListener);
    }

    private void registerRecognitionListener() {
        IAsrListener recognitionListener = new AsrListenerAdapter() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                setAsrState(AsrState.LISTENING);
                final TextView pttStatus = findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "Ready");
            }

            @Override
            public void onBeginningOfSpeech() {
                final TextView pttStatus = findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "Began speaking");

                final TextView asrRecognitionView = findViewById(R.id.userUtterance);
                updateTextViewInUiThread(asrRecognitionView, "");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onEndOfSpeech() {
                final TextView pttStatus = findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "Finished speaking");
            }

            @Override
            public void onError(String reason) {
                final TextView pttStatus = findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "AsrError: " + reason);
            }

            @Override
            public void onSpeechTimeout() {
                setAsrState(AsrState.IDLE);
                final TextView pttStatus = findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "ASR timed out");
            }

            @Override
            public void onEmptyResult() {
                setAsrState(AsrState.IDLE);
                final TextView pttStatus = findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "ASR results empty");
            }

            @Override
            public void onResults(List<AsrRecognitionHypothesis> hypotheses) {
                setAsrState(AsrState.IDLE);
                final TextView asrRecognitionView = findViewById(R.id.userUtterance);
                if (hypotheses.isEmpty()) {
                    updateTextViewInUiThread(asrRecognitionView, "");
                } else {
                    String recognition = hypotheses.get(0).getRecognition();
                    updateTextViewInUiThread(asrRecognitionView, recognition);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }
        };
        tdmConnector.registerRecognitionListener(recognitionListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setAsrState(AsrState state) {
        asrState = state;
        updatePttButton();
    }

    private void updatePttButton() {
        String text;
        boolean enabled;

        switch (asrState) {
            case DISABLED:
                text = "START LISTEN";
                enabled = false;
                break;

            case IDLE:
                text = "START LISTEN";
                enabled = true;
                break;

            case REQUESTED_TO_START_LISTENING:
                text = "START LISTEN";
                enabled = false;
                break;

            case LISTENING:
                text = "STOP LISTEN";
                enabled = true;
                break;

            case REQUESTED_TO_STOP_LISTENING:
                text = "STOP LISTEN";
                enabled = false;
                break;

            default:
                Log.e(TAG, "updatePttButton(): unexpected AsrState " + asrState);
                return;
        }

        final Button button = findViewById(R.id.pttButton);
        updateButtonInUiThread(button, text, enabled);
    }

    private void updateButtonInUiThread(final Button button, final String text, final boolean enabled) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                button.setText(text);
                button.setEnabled(enabled);
            }
        };
        runOnUiThread(runnable);
    }
}
