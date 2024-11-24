package com.myapp.maikeyboard;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class JsonFileHandler {

    public static void processJsonFile(String filePath) {
        try {
            // Step 1: Read the file into a String
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            // Step 2: Parse the JSON
            String jsonResponse = jsonBuilder.toString();
            JSONArray responseArray = new JSONArray(jsonResponse);

            // Step 3: Extract data (same as before)
            JSONArray translationArray = responseArray.getJSONArray(0);
            JSONArray mainTranslationArray = translationArray.getJSONArray(0);

            String translatedText = mainTranslationArray.getString(0); // Extract translated text
            String sourceText = mainTranslationArray.getString(1);     // Extract source text

            System.out.println("Translated Text: " + translatedText);
            System.out.println("Source Text: " + sourceText);

        } catch (IOException e) {
            System.err.println("Error reading the JSON file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing JSON: " + e.getMessage());
        }
    }
}
