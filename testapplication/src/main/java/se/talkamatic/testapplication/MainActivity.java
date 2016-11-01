package se.talkamatic.testapplication;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import se.talkamatic.frontend.ActionResult;
import se.talkamatic.frontend.IBackendStatusListener;
import se.talkamatic.frontend.IEventListener;
import se.talkamatic.frontend.Language;
import se.talkamatic.frontend.TdmConnector;
import se.talkamatic.frontend.WhQueryResult;
import se.talkamatic.frontend.asr.AsrListenerAdapter;
import se.talkamatic.frontend.asr.AsrRecognitionHypothesis;
import se.talkamatic.frontend.asr.IAsrListener;

public class MainActivity extends AppCompatActivity {

    private TdmConnector tdmConnector;
    private IAsrListener recognitionListener;
    private IEventListener eventListener;
    private enum AsrState {
        DISABLED,
        IDLE,
        REQUESTED_TO_START_LISTENING,
        LISTENING,
        REQUESTED_TO_STOP_LISTENING
    }
    private AsrState asrState;
    private final String TAG = "MainActivity";

    private static final Map<String, String> CONTACTS = new HashMap<String, String>() {{
        put("John", "0701234567");
        put("Lisa", "0709876543");
        put("Mary", "0706574839");
        put("Andy", null);
    }};

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
        final Button button = (Button) findViewById(R.id.connectButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tdmConnector.connect("ws://localhost:9090/maharani");
            }
        });
    }

    private void setupDisconnectButton() {
        final Button button = (Button) findViewById(R.id.disconnectButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tdmConnector.disconnect();
            }
        });
    }

    private void setupPttButton() {
        final Button button = (Button) findViewById(R.id.pttButton);
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
            public void onClose(int code, String reason) {
                Log.e("backendStatusListener", "onClose(" + code + ", " + reason + ")");
                setAsrState(AsrState.DISABLED);
                displayBackendStatus("Closed with code " + code + ": " + reason);
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
        final TextView backendStatusView = (TextView) findViewById(R.id.backendStatus);
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
        eventListener = new IEventListener() {
            @Override
            public void onShowPopup(String title, List<Map<String, String>> options) {
                Log.d("eventListener", "onShowPopup(title: " + title + ", options: " + options + ")");
            }

            @Override
            public void onAction(String ddd, String name, Map<String, String> args) {
                Log.d("eventListener", "onPerformAction(DDD: " + ddd + ", name: " + name + ", args: " + args + ")");
                final TextView performActionView = (TextView) findViewById(R.id.performAction);
                String text = name + ": " + args.toString();
                updateTextViewInUiThread(performActionView, text);
                //TODO: perform the action
                ActionResult result = new ActionResult(true);
                tdmConnector.getEventHandler().actionFinished(result);
            }

            @Override
            public void onWhQuery(String ddd, String name, Map<String, String> args) {
                Log.d("eventListener", "onPerformWHQuery(DDD: " + ddd + ", name: " + name + ", args: " + args + ")");
                final TextView performQueryView = (TextView) findViewById(R.id.performQuery);
                String text = name + ": " + args.toString();
                updateTextViewInUiThread(performQueryView, text);

                if(name.equals("phone_number_of_contact")) {
                    WhQueryResult resultObject = phoneNumberOfContactResult(args);
                    tdmConnector.getEventHandler().whQueryFinished(resultObject);
                }
                else {
                    WhQueryResult resultObject = WhQueryResult.fromStrings(new ArrayList<String>());
                    tdmConnector.getEventHandler().whQueryFinished(resultObject);
                }
            }

            private WhQueryResult phoneNumberOfContactResult(Map<String, String> args) {
                List<Map<String, String>> result = new ArrayList<>();
                String selectedContact = args.get("selected_contact_of_phone_number");
                String phoneNumber = CONTACTS.get(selectedContact);
                Map<String, String> phoneNumberEntity = createPhoneNumberEntity(phoneNumber);
                result.add(phoneNumberEntity);
                WhQueryResult resultObject = WhQueryResult.fromMaps(result);
                return resultObject;
            }

            private Map<String, String> createPhoneNumberEntity(final String number) {
                Map<String, String> phoneNumberEntity = new HashMap<String, String>() {{
                    put("grammar_entry", number);
                }};
                return phoneNumberEntity;
            }

            @Override
            public void onValidity(String ddd, String name, Map<String, String> args) {
                Log.d("eventListener", "onPerformValidity(DDD: " + ddd + ", name: " + name + ", args: " + args + ")");
                final TextView validateParameterView = (TextView) findViewById(R.id.validateParameter);
                String text = name + ": " + args.toString();
                updateTextViewInUiThread(validateParameterView, text);

                boolean is_valid = true;
                if(name.equals("CallerNumberAvailable")) {
                    String selectedContact = args.get("selected_contact_to_call");
                    is_valid = contactHasPhoneNumber(selectedContact);
                }
                if(name.equals("PhoneNumberAvailable")) {
                    String selectedContact = args.get("selected_contact_of_phone_number");
                    is_valid = contactHasPhoneNumber(selectedContact);
                }
                tdmConnector.getEventHandler().validityFinished(is_valid);
            }

            private boolean contactHasPhoneNumber(String contact) {
                String phoneNumber = CONTACTS.get(contact);
                if(phoneNumber != null) {
                    return true;
                }
                return false;
            }

            @Override
            public void onEntityRecognizer(String ddd, String name, Map<String, String> args) {
                Log.d("eventListener", "onPerformEntityRecognition(DDD: " + ddd + ", name: " + name + ", args: " + args + ")");
                final TextView recognizeEntityView = (TextView) findViewById(R.id.recognizeEntity);
                String text = name + ": " + args.toString();
                updateTextViewInUiThread(recognizeEntityView, text);

                String searchString = args.get("search_string");
                List<Map<String, String>> result = new ArrayList<>();

                if(name.equals("ContactRecognizer")) {
                    List<Map<String, String>> recognizedContacts = recognizeContacts(searchString);
                    result.addAll(recognizedContacts);
                }
                Log.d("eventListener", "onPerformEntityRecognition: result="+result.toString());
                tdmConnector.getEventHandler().entityRecognizerFinished(result);
            }

            private List<Map<String, String>> recognizeContacts(String searchString) {
                List<Map<String, String>> recognizedContacts = new ArrayList<>();
                String loweredSearchString = searchString.toLowerCase();
                List<String> words = Arrays.asList(loweredSearchString.split(" "));
                Iterator contactIterator = CONTACTS.entrySet().iterator();
                while (contactIterator.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry) contactIterator.next();
                    String contact = entry.getKey();
                    if (words.contains(contact.toLowerCase())) {
                        Map<String, String> contactEntity = createContactEntity(contact);
                        recognizedContacts.add(contactEntity);
                    }
                }
                return recognizedContacts;
            }

            private Map<String, String> createContactEntity(final String contact) {
                Map<String, String> recognizedContactEntity = new HashMap<String, String>() {{
                    put("sort", "contact");
                    put("grammar_entry", contact);
                }};
                return recognizedContactEntity;
            }

            @Override
            public void onSystemUtteranceToSpeak(String utterance) {
                Log.d("eventListener", "onSystemUtteranceToSpeak(" + utterance + ")");
                final TextView systemUtteranceView = (TextView) findViewById(R.id.systemUtterance);
                updateTextViewInUiThread(systemUtteranceView, utterance);
            }

            @Override
            public void onSelectedRecognition(String recognition) {
                Log.d("eventListener", "onSystemUtteranceToSpeak(" + recognition + ")");
                final TextView systemUtteranceView = (TextView) findViewById(R.id.interpretedUtterance);
                updateTextViewInUiThread(systemUtteranceView, recognition);
            }

            @Override
            public void onActiveDddChanged(String activeDdd, String languageCode) {
                Log.d("eventListener", "onActiveDddChanged(" + activeDdd + ", " + languageCode + ")");
                final TextView activeDddView = (TextView) findViewById(R.id.activeDdd);
                String text = activeDdd + ", " + languageCode;
                updateTextViewInUiThread(activeDddView, text);
            }
        };
        tdmConnector.registerEventListener(eventListener);
    }

    private void registerRecognitionListener() {
        recognitionListener = new AsrListenerAdapter() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                setAsrState(AsrState.LISTENING);
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "Ready");
            }

            @Override
            public void onBeginningOfSpeech() {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "Began speaking");

                final TextView asrRecognitionView = (TextView) findViewById(R.id.userUtterance);
                updateTextViewInUiThread(asrRecognitionView, "");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onEndOfSpeech() {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "Finished speaking");
            }

            @Override
            public void onError(String reason) {
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "AsrError: " + reason);
            }

            @Override
            public void onSpeechTimeout() {
                setAsrState(AsrState.IDLE);
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "ASR timed out");
            }

            @Override
            public void onEmptyResult() {
                setAsrState(AsrState.IDLE);
                final TextView pttStatus = (TextView) findViewById(R.id.pttStatus);
                updateTextViewInUiThread(pttStatus, "ASR results empty");
            }

            @Override
            public void onResults(List<AsrRecognitionHypothesis> hypotheses) {
                setAsrState(AsrState.IDLE);
                final TextView asrRecognitionView = (TextView) findViewById(R.id.userUtterance);
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

        final Button button = (Button) findViewById(R.id.pttButton);
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
