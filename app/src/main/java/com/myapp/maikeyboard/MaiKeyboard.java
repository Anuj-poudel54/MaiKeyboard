package com.myapp.maikeyboard;

import android.widget.Toast;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.speech.SpeechRecognizer;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


class SimpleKeyboardActionListener implements KeyboardView.OnKeyboardActionListener {



    private final InputMethodService inputMethodService;
    private KeyboardView keyboardView;
    boolean translateStart = false;
    String totranslateText = "";

    String toLang = "mai";
    String fromLang = "en";

    private SpeechRecognizer speechRecognizer;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    private boolean isShifted = false;
    private boolean lang = true ; //true = mai, false = eng


    public SimpleKeyboardActionListener(InputMethodService service, KeyboardView kv) {
        this.inputMethodService = service;
        this.keyboardView = kv;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Constructor
    // Method to fetch translation from the API in background
    private void translate(String urlString) {
        executorService.submit(() -> {
            String response = fetchApiResponse(urlString);
            if (response != null) {
                // On response, process the result on the main thread
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> onApiResponse(response));
            }
        });
    }

    // Method to fetch API response
    private String fetchApiResponse(String urlString) {
        String response = "";
        try {
            // Create the URL and open connection
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds timeout
            connection.setReadTimeout(5000);

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            response = result.toString();
        } catch (Exception e) {
            Log.e("API Error", "Error fetching translation mai", e);
        }
        return response;
    }

    // Method to process the API response
    private void onApiResponse(String response){

        try {
            // Parse the response string as a JSONArray
            JSONArray jsonResponse = new JSONArray(response);

            // The first element in the response is an array, so get it
            JSONArray translationData = jsonResponse.getJSONArray(0);

            // The first element inside translationData is another array that contains the translation
            JSONArray translatedText = translationData.getJSONArray(0);

            // Extract the translated and original text
            String toText = translatedText.getString(0); // "नमस्कार"
            String fromText = translatedText.getString(1); // "hello"

            // Log the output or use it in your UI
            Log.d("Translated Text", "Maithili: " + fromText + ", English: " + toText);

            InputConnection inputConnection = inputMethodService.getCurrentInputConnection();
            Log.d("d", "reached here");
            if (inputConnection != null) {
                // Calculate the length of the text to replace

                int lengthToReplace = fromText.length();
                Log.d("replt", fromText + " " + lengthToReplace);

                // Delete the existing text
                inputConnection.deleteSurroundingText(lengthToReplace, 0);

                // Commit the translated text
                inputConnection.commitText(toText, 1);
            }

            // You can update your UI or InputMethodService based on these texts

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    private void toggleShiftMode() {
        isShifted = !isShifted;
        Keyboard newKeyboard = isShifted ? new Keyboard(inputMethodService, R.xml.keypad_shifted) : new Keyboard(inputMethodService, R.xml.keypad);
        keyboardView.setKeyboard(newKeyboard);
    }

    private void toggleLanguage() {
        lang = !lang;
        Keyboard newKeyboard = lang ? new Keyboard(inputMethodService, R.xml.keypad) : new Keyboard(inputMethodService, R.xml.keypad_en);
        keyboardView.setKeyboard(newKeyboard);
        String temp = toLang;
        toLang = fromLang;
        fromLang = temp;
    }


    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        // Access the current input connection
        if (inputMethodService.getCurrentInputConnection() != null) {
            switch (primaryCode) {
                case Keyboard.KEYCODE_DELETE:
                    // Handle backspace
                    inputMethodService.getCurrentInputConnection().deleteSurroundingText(1, 0);
                    if (translateStart){
                        totranslateText = totranslateText.substring(0, totranslateText.length() -1 );
                    }
                    break;
                case Keyboard.KEYCODE_DONE:
                    // Handle "Enter" or "Done" key
                    inputMethodService.getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_DONE);
                    break;

                case -1: // Translate
                    Log.d("KeyPressed", "Key code: " + primaryCode);
                    if (translateStart){
                        translate(Constants.host+"/tl/"+totranslateText+"/"+toLang+"/"+fromLang);
                        totranslateText = "";
                    }
                    translateStart = !translateStart;
                    break;

                case -2: // Tl change
                    toggleLanguage();
                    break;

                case -3: // shift toggle
                    if (lang){
                        toggleShiftMode();
                    }
                    break;

                case -101: // OCR
                    String packageName = "com.myapp.maikeyboard";
                    PackageManager packageManager = inputMethodService.getPackageManager();
                    Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required to start activity from a service
                        inputMethodService.startActivity(launchIntent);
                    } else {
                        Toast.makeText(inputMethodService, "App not found!", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case -102: // ACR
                    break;

                case -110: // Gif
                    showGifPanel();
                    break;

                default:
                    // Handle other keys
                    char code = (char) primaryCode;
                    inputMethodService.getCurrentInputConnection().commitText(String.valueOf(code), 1);
                    if (translateStart)
                        totranslateText += code;
                    break;
            }
        }
    }

    private void showGifPanel() {
        // Inflate the GIF panel layout
        View gifPanel = inputMethodService.getLayoutInflater().inflate(R.layout.gif_panel, null);

        RecyclerView recyclerView = gifPanel.findViewById(R.id.gifRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(inputMethodService, RecyclerView.HORIZONTAL, false));

        // Example list of GIF URLs
        List<String> gifUrls = Arrays.asList(
                "https://drive.google.com/u/0/drive-viewer/AKGpihblR2zAc7z5CKQLriaTWGBsv8vcxACW4uNSd6DYFTNjvRZ3YKiu87yBxJhaAYrW56yuR_qk3kbUmjWn8HR_aLh6NwewO4x9FUg=s2560"
        );

        GifAdapter adapter = new GifAdapter(inputMethodService, gifUrls, new GifAdapter.OnGifSelectedListener() {
            @Override
            public void onGifSelected(String selectedGifUrl) {
                sendGif(selectedGifUrl);
            }
        });

        recyclerView.setAdapter(adapter);

        // Display the panel
        Keyboard newKeyboard = new Keyboard(inputMethodService, R.layout.gif_panel);
        keyboardView.setKeyboard(newKeyboard);
    }

    private void sendGif(String gifUrl) {
        // Insert GIF URL into the text field (or send as needed)
        if (inputMethodService.getCurrentInputConnection() != null) {
            inputMethodService.getCurrentInputConnection().commitText(gifUrl, 1);
        }
    }

    @Override
    public void onPress(int primaryCode) {

    }
    @Override
    public void onRelease(int primaryCode) {}
    @Override
    public void onText(CharSequence text) {}
    @Override
    public void swipeLeft() {}
    @Override
    public void swipeRight() {}
    @Override
    public void swipeDown() {}
    @Override
    public void swipeUp() {}
}
public class MaiKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private  boolean isShifted = false;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.maikeyboard_layout, null);
        keyboard = new Keyboard(this, R.xml.keypad);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(new SimpleKeyboardActionListener(this, keyboardView));
        return keyboardView;
    }

    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
    }

    @Override
    public void onPrepareStylusHandwriting() {
        super.onPrepareStylusHandwriting();
    }

    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
//        InputConnection inputConnection = getCurrentInputConnection();
//        if (inputConnection == null){
//            return;
//        }
//        inputConnection.commitText(String.valueOf((char) primaryCode),1);
        Log.d("pressedsh", ""+primaryCode);
    }

    @Override
    public void onText(CharSequence charSequence) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }
}
