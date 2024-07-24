package com.example;

import java.awt.Rectangle;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Represents an locator for an element.
 */
public class Locator_old implements Comparable<Object> {
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
    private static final String DEFAULT_PROPERTIES_FILE_PATH = "metadata.properties";

    public Locator_old() {
    }

    public Locator_old(Properties properties) {
        setProperties(properties);
    }

    public Locator_old(String filePath) {
        loadPropertiesFromFile(filePath);

    }

    public Locator_old clone() {
        Locator_old clone = new Locator_old();
        clone.loadPropertiesFromFile(DEFAULT_PROPERTIES_FILE_PATH);
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
    public void savePropertiesToFile(String filePath) {
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            properties.store(output, "Locator Properties");
            System.out.println("Properties file saved successfully: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load properties from file
     * 
     * @param filePath The file path to load the properties from
     */
    public void loadPropertiesFromFile(String filePath) {
        try (FileInputStream input = new FileInputStream(filePath)) {
            properties.load(input);
            setX(Integer.parseInt(getMetadata("x")));
            setY(Integer.parseInt(getMetadata("y")));
            setHeight(Integer.parseInt(getMetadata("height")));
            setWidth(Integer.parseInt(getMetadata("width")));
            setLocationArea(new Rectangle(Integer.parseInt(getMetadata("x")),
                    Integer.parseInt(getMetadata("y")), Integer.parseInt(getMetadata("width")),
                    Integer.parseInt(getMetadata("height"))));
            System.out.println("Properties file loaded successfully: " + filePath);
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
    public void saveMetadataToFile() {
        List<String> keys = getMetadataKeys();
        for (String key : keys) {
            properties.setProperty(key, getMetadata(key));
        }
        savePropertiesToFile("metadata.properties");
    }
}
