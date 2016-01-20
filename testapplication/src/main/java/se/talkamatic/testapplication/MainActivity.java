package se.talkamatic.testapplication;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import se.talkamatic.frontend.ActionResult;
import se.talkamatic.frontend.Language;
import se.talkamatic.frontend.asr.AsrListenerAdapter;
import se.talkamatic.frontend.asr.AsrRecognitionHypothesis;
import se.talkamatic.frontend.asr.IAsrListener;
import se.talkamatic.frontend.IBackendStatusListener;
import se.talkamatic.frontend.IEventListener;
import se.talkamatic.frontend.TdmConnector;

public class MainActivity extends AppCompatActivity {

    private TdmConnector tdmConnector;
    private IAsrListener recognitionListener;
    private IEventListener eventListener;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();

        mainHandler = new Handler(context.getMainLooper());

        tdmConnector = TdmConnector.createTdmConnector(context);
        tdmConnector.setLanguage(Language.English);

        registerRecognitionListener();
        registerEventListener();
        registerBackendStatusListener();

        setupConnectButton();
        setupDisconnectButton();
        setupPttButton();
    }

    private void setupConnectButton() {
        final Button button = (Button) findViewById(R.id.connectButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Runnable handleClickRunnable = new Runnable() {
                    @Override
                    public void run() {
                        tdmConnector.connect("ws://localhost:9090/maharani");
                    }
                };
                mainHandler.post(handleClickRunnable);
            }
        });
    }

    private void setupDisconnectButton() {
        final Button button = (Button) findViewById(R.id.disconnectButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Runnable handleClickRunnable = new Runnable() {
                    @Override
                    public void run() {
                        tdmConnector.disconnect();
                    }
                };
                mainHandler.post(handleClickRunnable);
            }
        });
    }

    private void setupPttButton() {
        final Button button = (Button) findViewById(R.id.pttButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Runnable handleClickRunnable = new Runnable() {
                    @Override
                    public void run() {
                        tdmConnector.notifyPTTPushed();
                    } // To run in main thread
                };
                mainHandler.post(handleClickRunnable);
            }
        });
    }

    private void registerBackendStatusListener() {
        IBackendStatusListener backendStatusListener = new IBackendStatusListener() {
            @Override
            public void onOpen() {
                Log.e("backendStatusListener", "onOpen()");
                setBackendStatus("Opened");
            }

            @Override
            public void onClose(int code, String reason) {
                Log.e("backendStatusListener", "onClose(" + code + ", " + reason + ")");
                setBackendStatus("Closed with code " + code + ": " + reason);
            }

            @Override
            public void onError(String reason) {
                Log.e("backendStatusListener", "onError(" + reason + ")");
                setBackendStatus("AsrError: " + reason);
            }
        };
        tdmConnector.registerBackendStatusListener(backendStatusListener);
    }

    private void setBackendStatus(String status) {
        final TextView backendStatusView = (TextView) findViewById(R.id.backendStatus);
        backendStatusView.setText(status);
    }

    private void registerEventListener() {
        eventListener = new IEventListener() {
            @Override
            public void onShowPopup(String title, List<Map<String, String>> options) {
                Log.d("eventListener", "onShowPopup(title: " + title + ", options: " + options + ")");
            }

            @Override
            public void onPerformAction(String appName, String name, Map<String, String> args) {
                Log.d("eventListener", "onPerformAction(appName: " + appName + ", name: " + name + ", args: " + args + ")");
                tdmConnector.getEventHandler().deviceActionFinished(new ActionResult(true));
            }

            @Override
            public void onSystemUtteranceToSpeak(String utterance) {
                Log.d("eventListener", "onSystemUtteranceToSpeak(" + utterance + ")");
                final TextView systemUtteranceView = (TextView) findViewById(R.id.systemUtterance);
                systemUtteranceView.setText(utterance);
            }

            @Override
            public void onSelectedRecognition(String recognition) {
                Log.d("eventListener", "onSystemUtteranceToSpeak(" + recognition + ")");
                final TextView systemUtteranceView = (TextView) findViewById(R.id.interpretedUtterance);
                systemUtteranceView.setText(recognition);
            }
        };
        tdmConnector.registerEventListener(eventListener);
    }

    private void registerRecognitionListener() {
        recognitionListener = new AsrListenerAdapter() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                pttStatus.setText("Ready");
            }

            @Override
            public void onBeginningOfSpeech() {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                pttStatus.setText("Began speaking");

                final TextView asrRecognitionView = (TextView) findViewById(R.id.userUtterance);
                asrRecognitionView.setText("");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onEndOfSpeech() {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                pttStatus.setText("Finished speaking");
            }

            @Override
            public void onError(String reason) {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                pttStatus.setText("AsrError: " + reason);
            }

            @Override
            public void onSpeechTimeout() {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                pttStatus.setText("ASR timed out");
            }

            @Override
            public void onResults(List<AsrRecognitionHypothesis> hypotheses) {
                final TextView asrRecognitionView = (TextView) findViewById(R.id.userUtterance);
                if (hypotheses.isEmpty()) {
                    asrRecognitionView.setText("");
                } else {
                    asrRecognitionView.setText(hypotheses.get(0).getRecognition());
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
}
