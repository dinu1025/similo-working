package com.example;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Represents a locator for an element.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Locator implements Comparable<Object> {
    private long index = 0;
    private Properties properties = new Properties();
    private Rectangle locationArea = null;
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    private double maxScore = 0;
    private double score = 0;
    private long duration = 0;

    // Default properties file path
    private static final String DEFAULT_PROPERTIES_FILE_PATH = "metadata.json";
    private ObjectMapper objectMapper = new ObjectMapper();
    

    public Locator() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Locator(Properties properties) {
        setProperties(properties);
    }

    public Locator(String filePath, String xpath) {
        loadPropertiesFromFile(filePath, xpath);
    }

    public Locator clone(String xpath) {
        Locator clone = new Locator();
        clone.loadPropertiesFromFile(DEFAULT_PROPERTIES_FILE_PATH, xpath);
        clone.index = index;
        clone.locationArea = new Rectangle(locationArea);
        clone.properties = new Properties();
        Set<Object> keySet = properties.keySet();
        for (Object o : keySet) {
            String key = (String) o;
            String value = properties.getProperty(key);
            clone.properties.put(key, value);
        }
        return clone;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Get metadata for key
     * 
     * @param key The key to get metadata from
     * @return Metadata for key or null
     */
    public String getMetadata(String key) {
        return properties.getProperty(key);
    }

    public int getNoProperties() {
        return properties.size();
    }

    /**
     * Save properties to file
     * 
     * @param filePath The file path to save the properties
     */
    @JsonIgnore
    public void savePropertiesToFile(String filePath) {
        String keyToUpdate = this.getProperties().getProperty("xpath");
        JSONObject jsonObject1 = new JSONObject();

        for (String key : properties.stringPropertyNames()) {
            jsonObject1.put(key, properties.getProperty(key));
        }


        try {
            // Read the existing JSON file
            String content = new String(Files.readAllBytes(Paths.get(filePath)));

            // Convert the content to a JSON object or array
            Object json = new org.json.JSONTokener(content).nextValue();
            if (json instanceof JSONObject) {
                // Update the JSON object
                JSONObject jsonObject = (JSONObject) json;
                updateJsonObject(jsonObject, keyToUpdate, jsonObject1);
                Files.write(Paths.get(filePath), jsonObject.toString(4).getBytes());

            } else if (json instanceof JSONArray) {
                // Update the JSON array
                JSONArray jsonArray = (JSONArray) json;
                updateJsonArray(jsonArray, keyToUpdate, jsonObject1);
                Files.write(Paths.get(filePath), jsonArray.toString(4).getBytes());
            }

            System.out.println("JSON updated successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void updateJsonObject(JSONObject jsonObject, String key, JSONObject newValue) {
        // Update or insert the JSON object under the given key
        jsonObject.put(key, newValue);
    }

    private static void updateJsonArray(JSONArray jsonArray, String key, JSONObject newValue) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            // Update or insert the JSON object under the given key
            jsonObject.put(key, newValue);
        }
    }

    /**
     * Load properties from file
     * 
     * @param filePath The file path to load the properties from
     */
    @JsonIgnore@JsonIgnoreProperties(ignoreUnknown = true)
    public void loadPropertiesFromFile(String filePath, String xpath) {
      
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Read the JSON file
            JSONObject jsonFileContent = new JSONObject(new String(Files.readAllBytes(Paths.get(filePath))));

            // Get the JSON object associated with the given key
            if (jsonFileContent.has(xpath)) {
                JSONObject keyJsonObject = jsonFileContent.getJSONObject(xpath);

                // Map the JSON object to the Locator class
                Locator loadedLocator = objectMapper.readValue(keyJsonObject.toString(), Locator.class);

                
                // Set properties
                this.index = loadedLocator.index;
                this.properties = loadedLocator.properties;
                this.locationArea = new Rectangle(loadedLocator.x, loadedLocator.y, loadedLocator.width, loadedLocator.height);
                this.x = loadedLocator.x;
                this.y = loadedLocator.y;
                this.width = loadedLocator.width;
                this.height = loadedLocator.height;
                this.maxScore = loadedLocator.maxScore;
                this.score = loadedLocator.score;
                this.duration = loadedLocator.duration;
                this.setLocationArea(new Rectangle(x, y, width, height));
                this.setX(x);
                this.setY(y);
                this.setWidth(width);
                this.setHeight(height);
                this.putMetadata("x", String.valueOf(x));
                this.putMetadata( "y", String.valueOf(y));
                this.putMetadata( "height", String.valueOf(height));
                this.putMetadata( "width", String.valueOf(width));

                int area = width * height;
                int shape = (width * 100) / height;
                this.putMetadata("area", "" + area);
                this.putMetadata("shape", "" + shape);

                this.putMetadata("xpath", keyJsonObject.getString("xpath"));
                this.putMetadata("idxpath", keyJsonObject.getString("idxpath"));
                this.putMetadata("robula", keyJsonObject.getString("robula"));
                this.putMetadata("montoto", keyJsonObject.getString("montoto"));
                this.putMetadata("ide", keyJsonObject.getString("ide"));
                this.putMetadata("visible_text", keyJsonObject.getString("visible_text"));
                if(keyJsonObject.has("title"))
                    this.putMetadata("title", keyJsonObject.getString("title"));
                this.putMetadata("is_button", keyJsonObject.getString("is_button"));
                if(keyJsonObject.has("neighbor_text"))
                    this.putMetadata("neighbor_text", keyJsonObject.getString("neighbor_text"));
                if(keyJsonObject.has("text"))
                    this.putMetadata("text", keyJsonObject.getString("text"));

                if(keyJsonObject.has("href"))
                    this.putMetadata("href", keyJsonObject.getString("href"));
                this.putMetadata("tag", keyJsonObject.getString("tag"));
                this.putMetadata("class", keyJsonObject.getString("class"));
                System.out.println("Properties file loaded successfully: " + loadedLocator.getMetadata("xpath"));
            } else {
                System.out.println("Properties/Parameters not found in JSON file for Xpath: " + xpath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Put meta data associated with this state
     * 
     * @param key   The metadata key
     * @param value The metadata value
     */
    public void putMetadata(String key, String value) {
        if (value != null) {
            properties.put(key, value);
        }
    }

    /**
     * Remove meta data for key
     * 
     * @param key The metadata key
     */
    public void removeMetadata(String key) {
        properties.remove(key);
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public void clearRepairedMetadata() {
        removeMetadata("repaired");
    }

    public boolean containsRepairedMetadata() {
        String repaired = getMetadata("repaired");
        return repaired != null && !repaired.isEmpty();
    }

    public boolean isRepairedMetadata(String key) {
        String repaired = getMetadata("repaired");
        if (repaired == null) {
            return false;
        }
        String paddedRepaired = " " + repaired + " ";
        return paddedRepaired.contains(" " + key + " ");
    }

    public void addRepairedMetadata(String key) {
        if (isRepairedMetadata(key)) {
            // Already repaired
            return;
        }
        String repaired = getMetadata("repaired");
        if (repaired == null) {
            repaired = "";
        }
        if (!repaired.isEmpty()) {
            repaired += " ";
        }
        repaired += key;
        putMetadata("repaired", repaired);
    }

    public boolean isIgnoredMetadata(String key) {
        String ignored = getMetadata("ignored");
        if (ignored == null) {
            return false;
        }
        String paddedIgnored = " " + ignored + " ";
        return paddedIgnored.contains(" " + key + " ");
    }

    public void addIgnoredMetadata(String key) {
        if (isIgnoredMetadata(key)) {
            // Already ignored
            return;
        }
        String ignored = getMetadata("ignored");
        if (ignored == null) {
            ignored = "";
        }
        if (!ignored.isEmpty()) {
            ignored += " ";
        }
        ignored += key;
        putMetadata("ignored", ignored);
    }

    public Rectangle getLocationArea() {
        return locationArea;
    }

    public void setLocationArea(Rectangle locationArea) {
        this.locationArea = locationArea;
    }

    public String getVisibleText() {
        String text = getMetadata("text");
        String value = getMetadata("value");
        String placeholder = getMetadata("placeholder");

        if (text != null && !text.trim().isEmpty()) {
            return text.trim();
        } else if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        } else if (placeholder != null && !placeholder.trim().isEmpty()) {
            return placeholder.trim();
        }

        return null;
    }

    public int compareTo(Object o) {
        Locator compareTo = (Locator) o;
        return (int) (compareTo.getScore() * 1000 - getScore() * 1000);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void increaseScore(double increaseScore) {
        this.score += increaseScore;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * Get all metadata keys
     * 
     * @return A list of metadata keys
     */
    public List<String> getMetadataKeys() {
        if (properties == null) {
            return null;
        }
        Set<Object> keySet = properties.keySet();
        List<String> keyList = new ArrayList<>();
        for (Object o : keySet) {
            keyList.add((String) o);
        }
        return keyList;
    }

    /**
     * Save metadata to file
     */
    @JsonIgnore
    public void saveMetadataToFile() {
        savePropertiesToFile(DEFAULT_PROPERTIES_FILE_PATH);
    }
}
