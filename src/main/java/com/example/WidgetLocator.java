package com.example;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class WidgetLocator {
	private static String[] LOCATORS = { "tag", "class", "name", "id", "href", "alt", "xpath", "idxpath", "is_button",
			"location", "area", "shape", "visible_text", "neighbor_text" };
	private double[] WEIGHTS = { 1.5, 0.5, 1.5, 1.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 2.0, 1.0 };
	private static int[] SIMILARITY_FUNCTION = { 0, 1, 0, 0, 1, 1, 1, 1, 0, 3, 2, 2, 1, 4 };
	private static int FIRST_APP_INDEX = 0;
	private static int END_APP_INDEX = FIRST_APP_INDEX;
	private static int NO_THREADS = 20;

	private String elementsToExtract = "input,textarea,button,select,a,h1,h2,h3,h4,h5,li,span,div,p,th,tr,td,label,svg,img,section,link";
	private boolean logOn = true;
	private long highestDuration = 0;

	public WidgetLocator(String[] args) {
		if (args.length > 0) {
			FIRST_APP_INDEX = string2Int(args[0]);
			END_APP_INDEX = FIRST_APP_INDEX;
		}
		if (args.length > 1) {
			END_APP_INDEX = string2Int(args[1]);
		}
	}

	private String fromIdeToXPath(String ide) {
		if (ide == null) {
			return null;
		}
		if (ide.startsWith("xpath:")) {
			return ide.substring(6);
		} else if (ide.startsWith("id:")) {
			String id = ide.substring(3);
			return "//*[@id='" + id + "']";
		} else if (ide.startsWith("name:")) {
			String name = ide.substring(5);
			return "//*[@id='" + name + "']";
		} else if (ide.startsWith("linkText:")) {
			String linkText = ide.substring(9);
			return "//*[contains(text(),'" + linkText + "')]";
		} else {
			return ide;
		}
	}


	private String removePathAttributes(String html) {
        Document doc = Jsoup.parse(html);
        Elements pathElements = doc.select("path");

        for (Element pathElement : pathElements) {
            pathElement.removeAttr("d");
        }

        // Return the outer HTML of the body element
        return doc.body().html();
    }
	
	public Locator readMetadata_ofLocator(String xPath, WebDriver driver) {
		Locator locator = getLocatorForElement(xPath, driver);
		if (locator != null) {
			addMetadata(locator, "xpath", xPath);
			locator.savePropertiesToFile(App.DEFAULT_PROPERTIES_FILE_PATH);
		} else {
			log("No locator for xpath: " + xPath);
		}
		return locator;
	}

	public List<Locator> getAllLocators(Locator targetLocator, WebDriver driver) {

		String[] xPathLocators = { "xpath", "idxpath", "ide", "robula", "montoto" };
				for (String xPathLocator : xPathLocators) {
					int located = 0;
					int notLocated = 0;
					int incorrectlyLocated = 0;
						String targetXPath = targetLocator.getMetadata(xPathLocator);
						if (xPathLocator.equals("ide")) {
							targetXPath = fromIdeToXPath(targetXPath);
						}
						Locator candidateLocator = getXPathLocatorForElement(targetXPath, driver);
						if (candidateLocator != null) {
							// Found a candidate
							String elementXPath = candidateLocator.getMetadata("xpath");
							String candidateXPath = targetLocator.getMetadata("xpath");
							if (almostIdenticalXPaths(elementXPath, candidateXPath)) {
								// Found the correct candidate
								located++;
							} else {
								incorrectlyLocated++;
							}
						} else {
							// Did not find a candidate
							notLocated++;
						}
					System.out.println(xPathLocator + ":\t" + located + "\t" + notLocated + "\t" + incorrectlyLocated);
				}

				int located = 0;
				int notLocated = 0;
				System.out.println("Multilocator: \t" + located + "\t" + notLocated + "\t0");
				List<Locator> candidateLocators = getLocators(driver);
				candidateLocators.stream().forEach(a -> System.out.println(a.getMetadata("xpath")));
				if (targetLocator != null) {
					List<Locator> cLocator = similo(targetLocator, candidateLocators);
					System.out.println(cLocator.get(0).getMetadata("xpath"));
				}

				List<Locator> bestCandidate = new ArrayList<>();
				located = 0;
				notLocated = 0;
				int incorrectlyLocated = 0;
					bestCandidate = similo(targetLocator, candidateLocators);
					if (bestCandidate == null) {
						notLocated++;
					} else {
						String bestCandidateXPath = bestCandidate.get(0).getMetadata("xpath");
						String correctCandidateXPath = targetLocator.getMetadata("xpath");
						System.out.println(bestCandidateXPath+" ----- "+correctCandidateXPath);
						if (almostIdenticalXPaths(bestCandidateXPath, correctCandidateXPath)) {
							located++;
						} else {
							incorrectlyLocated++;
						}
					}
				
				System.out.println("Similo:\t" + located + "\t" + notLocated + "\t" + incorrectlyLocated);
				bestCandidate.stream().forEach(a -> System.out.println(a.getScore() +"   :    "+a.getMetadata("xpath")));
		return bestCandidate.subList(0, 3);
	}

	private String removeLastElement(String xpath) {
		int lastIndex = xpath.lastIndexOf('/');
		if (lastIndex > 0) {
			return xpath.substring(0, lastIndex);
		}
		return xpath;
	}

	private boolean almostIdenticalXPaths(String xpath1, String xpath2) {
		if (xpath1 == null || xpath2 == null) {
			return false;
		}
		int length1 = xpath1.length();
		int length2 = xpath2.length();
		if (length1 == length2) {
			return xpath1.equalsIgnoreCase(xpath2);
		} else if (length1 < length2) {
			xpath2 = removeLastElement(xpath2);
			return xpath1.equalsIgnoreCase(xpath2);
		} else {
			xpath1 = removeLastElement(xpath1);
			return xpath1.equalsIgnoreCase(xpath2);
		}
	}

	private class FindWebElementsThread extends Thread {
		private Locator targetWidget;
		private List<Locator> candidateWidgets;

		public FindWebElementsThread(Locator targetWidget, List<Locator> candidateWidgets) {
			this.targetWidget = targetWidget;
			this.candidateWidgets = candidateWidgets;
		}

		public void run() {
			similoCalculation(targetWidget, candidateWidgets);
		}
	}

	private List<Locator> similo(Locator targetWidget, List<Locator> candidateWidgets) {
		similoCalculation(targetWidget, candidateWidgets);

		for (Locator candidateWidget : candidateWidgets) {
			candidateWidget.setDuration(0);
		}

		// Split candidateWidgets for each thread
		List<List<Locator>> candidateWidgetsList = new ArrayList<List<Locator>>();
		int candidatesPerThread = candidateWidgets.size() / NO_THREADS + 1;
		List<Locator> candidateWidgetsToAdd = new ArrayList<Locator>();
		for (Locator candidateWidget : candidateWidgets) {
			candidateWidgetsToAdd.add(candidateWidget);
			if (candidateWidgetsToAdd.size() >= candidatesPerThread) {
				candidateWidgetsList.add(candidateWidgetsToAdd);
				candidateWidgetsToAdd = new ArrayList<Locator>();
			}
		}
		if (candidateWidgetsToAdd.size() > 0) {
			// Add the reminders
			candidateWidgetsList.add(candidateWidgetsToAdd);
		}

		// Create and start threads
		List<Thread> threads = new ArrayList<Thread>();
		for (List<Locator> candidateWidgetsInThread : candidateWidgetsList) {
			Locator targetWidgetClone = targetWidget.clone(targetWidget.getMetadata("xpath"));
			Thread thread = new FindWebElementsThread(targetWidgetClone, candidateWidgetsInThread);
			threads.add(thread);
			thread.start();
		}

		// Wait for the threads to complete
		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
		}

		highestDuration = 0;
		for (Locator candidateWidget : candidateWidgets) {
			if (candidateWidget.getDuration() > highestDuration) {
				highestDuration = candidateWidget.getDuration();
			}
		}

		Collections.sort(candidateWidgets);
		return candidateWidgets;
	}

	private void similoCalculation(Locator targetWidget, List<Locator> candidateWidgets) {
		long startTime = System.currentTimeMillis();
		double bestSimilarityScore = 0;
		for (Locator candidateWidget : candidateWidgets) {
			double similarityScore = 0;
			if (candidateWidget.getMaxScore() > bestSimilarityScore) {
				similarityScore = calcSimilarityScore(targetWidget, candidateWidget);
			}
			candidateWidget.setScore(similarityScore);
			if (similarityScore > bestSimilarityScore) {
				System.out.println("Best Score : "+ bestSimilarityScore+"    Similarity Score: "+similarityScore);
				bestSimilarityScore = similarityScore;
			}
			long duration = System.currentTimeMillis() - startTime;
			candidateWidget.setDuration(duration);
		}
	}

	private double calcMaxSimilarityScore(Locator candidateWidget) {
		double similarityScore = 0;
		int index = 0;
		for (String locator : LOCATORS) {
			double weight = WEIGHTS[index];
			String candidateValue = candidateWidget.getMetadata(locator);
			if (candidateValue != null) {
				similarityScore += weight;
			}
			index++;
		}
		return similarityScore;
	}

	private double calcSimilarityScore(Locator targetWidget, Locator candidateWidget) {
		double similarityScore = 0;
		int index = 0;
		for (String locator : LOCATORS) {
			double weight = WEIGHTS[index];
			double similarity = 0;

			String targetValue = targetWidget.getMetadata(locator);
			String candidateValue = candidateWidget.getMetadata(locator);

			if (targetValue != null && candidateValue != null) {
				int similarityFunction = SIMILARITY_FUNCTION[index];
				if (similarityFunction == 1) {
					similarity = ((double) stringSimilarity(targetValue, candidateValue, 100)) / 100;
				} else if (similarityFunction == 2) {
					similarity = ((double) integerSimilarity(targetValue, candidateValue, 100)) / 100;
				} else if (similarityFunction == 3) {
					// Use 2D distance

					int x = string2Int(targetWidget.getMetadata("x"));
					int y = string2Int(targetWidget.getMetadata("y"));
					int xc = string2Int(candidateWidget.getMetadata("x"));
					int yc = string2Int(candidateWidget.getMetadata("y"));

					int dx = x - xc;
					int dy = y - yc;
					int pixelDistance = (int) Math.sqrt(dx * dx + dy * dy);
					similarity = ((double) Math.max(100 - pixelDistance, 0)) / 100;
				} else if (similarityFunction == 4) {
					similarity = ((double) neighborTextSimilarity(targetValue, candidateValue, 100)) / 100;
				} else {
					similarity = (double) equalSimilarity(targetValue, candidateValue, 1);
				}
			}

			similarityScore += similarity * weight;
			index++;
		}
		System.out.println(similarityScore);
		return similarityScore;
	}

	private int equalSimilarity(String t1, String t2, int maxScore) {
		if (t1 != null && t2 != null) {
			if (t1.equalsIgnoreCase(t2)) {
				return maxScore;
			}
		}
		return 0;
	}

	private int integerSimilarity(String t1, String t2, int maxScore) {
		int value1 = string2Int(t1);
		int value2 = string2Int(t2);
		return integerSimilarity(value1, value2, maxScore);
	}

	private int integerSimilarity(int value1, int value2, int maxScore) {
		int distance = Math.abs(value1 - value2);
		int max = Math.max(value1, value2);
		int score = (max - distance) * maxScore / max;
		return score;
	}

	private int stringSimilarity(String s1, String s2, int maxScore) {
		if (s1.length() == 0 || s2.length() == 0) {
			return 0;
		}

		if (s1.equals(s2)) {
			return maxScore;
		}

		// Make sure s1 is longer (or equal)
		if (s1.length() < s2.length()) {
			// Swap
			String swap = s1;
			s1 = s2;
			s2 = swap;
		}

		int distance = computeLevenshteinDistance(s1, s2);
		return (s1.length() - distance) * maxScore / s1.length();
	}

	private int computeLevenshteinDistance(String s1, String s2) {
		s1 = stripString(s1);
		s2 = stripString(s2);

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0) {
					costs[j] = j;
				} else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						}
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0) {
				costs[s2.length()] = lastValue;
			}
		}
		return costs[s2.length()];
	}

	private String stripString(String s) {
		StringBuffer stripped = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isAlphabetic(c) || Character.isDigit(c)) {
				stripped.append(c);
			}
		}
		String strippedString = stripped.toString();
		return strippedString;
	}
	
	private void log(String text) {
		log("WidgetLocatorResults.txt", text);
	}

	private void log(String filename, String text) {
		if (!logOn) {
			return;
		}
		writeLine(filename, text);
	}

	private void writeLine(String filename, String text) {
		String logMessage = text + "\r\n";
		File file = new File(filename);
		try {
			FileOutputStream o = new FileOutputStream(file, true);
			o.write(logMessage.getBytes());
			o.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Get all locators that belong to any of the tags in elementsToExtract
	 * 
	 * @return A list of locators to web elements
	 */
	public List<Locator> getLocators(WebDriver webDriver) {
		List<Locator> locators = new ArrayList<Locator>();

		if (webDriver != null) {
			try {
				String javascript = loadTextFile("javascript.js");
				webDriver.manage().timeouts().setScriptTimeout(300, TimeUnit.SECONDS);
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				String script = "var xpathExpression = arguments[0]; var result = []; " +
					"var all = document.querySelectorAll(xpathExpression); " +
					"for (var i=0, max=all.length; i < max; i++) { " +
					"    if (elementIsVisible(all[i])) result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length, 'xpath': getXPath(all[i]), 'idxpath': getIdXPath(all[i])}); " +
					"} " +
					" return JSON.stringify(result);";
				Object object = executor.executeScript(javascript + script, elementsToExtract);

				String json = object.toString();
				JSONParser parser = new JSONParser();
				JSONArray jsonArray = (JSONArray) parser.parse(json);
				for (int i = 0; i < jsonArray.size(); i++) {
					JSONObject jsonObject = (JSONObject) jsonArray.get(i);

					String tag = object2String(jsonObject.get("tag"));
					if (tag != null) {
						tag = tag.toLowerCase();
					}
					String className = object2String(jsonObject.get("class"));
					String type = object2String(jsonObject.get("type"));
					String name = object2String(jsonObject.get("name"));
					String id = object2String(jsonObject.get("id"));
					String value = object2String(jsonObject.get("value"));
					String href = object2String(jsonObject.get("href"));
					String text = object2String(jsonObject.get("text"));
					String placeholder = object2String(jsonObject.get("placeholder"));
					String title = object2String(jsonObject.get("title"));
					String alt = object2String(jsonObject.get("alt"));
					String xpath = object2String(jsonObject.get("xpath"));
					String idxpath = object2String(jsonObject.get("idxpath"));
					String xStr = object2String(jsonObject.get("x"));
					String yStr = object2String(jsonObject.get("y"));
					String widthStr = object2String(jsonObject.get("width"));
					String heightStr = object2String(jsonObject.get("height"));

					int x = string2Int(xStr);
					int y = string2Int(yStr);
					int width = string2Int(widthStr);
					int height = string2Int(heightStr);

					if (width > 0 && height > 0) {
						Locator locator = new Locator();
						locator.setLocationArea(new Rectangle(x, y, width, height));
						locator.setX(x);
						locator.setY(y);
						locator.setWidth(width);
						locator.setHeight(height);

						addMetadata(locator, "tag", tag);
						addMetadata(locator, "class", className);
						addMetadata(locator, "type", type);
						addMetadata(locator, "name", name);
						addMetadata(locator, "id", id);
						addMetadata(locator, "value", value);
						addMetadata(locator, "href", href);
						if (isValidText(text)) {
							addMetadata(locator, "text", truncate(text));
						}
						addMetadata(locator, "placeholder", placeholder);
						addMetadata(locator, "title", title);
						addMetadata(locator, "alt", alt);
						addMetadata(locator, "xpath", xpath);
						addMetadata(locator, "idxpath", idxpath);
						addMetadata(locator, "x", xStr);
						addMetadata(locator, "y", yStr);
						addMetadata(locator, "height", heightStr);
						addMetadata(locator, "width", widthStr);

						int area = width * height;
						int shape = (width * 100) / height;
						addMetadata(locator, "area", "" + area);
						addMetadata(locator, "shape", "" + shape);

						String visibleText = locator.getVisibleText();
						if (visibleText != null) {
							locator.putMetadata("visible_text", visibleText);
						}
						String isButton = isButton(tag, type, className) ? "yes" : "no";
						;
						locator.putMetadata("is_button", isButton);
						locators.add(locator);
					}
				}

				for (Locator locator : locators) {
					addNeighborTexts(locator, locators);
					double maxScore = calcMaxSimilarityScore(locator);
					locator.setMaxScore(maxScore);
				}

				return locators;
			} catch (Exception e) {
				return null;
			}
		}

		return null;
	}

	private String truncate(String text) {
		if (text == null) {
			return null;
		}
		if (text.length() > 50) {
			return text.substring(0, 49);
		}
		return text;
	}

	String script = "var xpathExpression = arguments[0];" +
                        "var result = document.evaluate(xpathExpression, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);" +
                        "var nodes = [];" +
                        "for (var i = 0; i < result.snapshotLength; i++) {" +
                        "    nodes.push(result.snapshotItem(i).outerHTML);" +
                        "}" +
                        "return nodes;";

	public Locator getLocatorForElement(String elementXPath, WebDriver driver) {
		
		List<Locator> locators = getLocators(driver);
		if (locators != null) {
			for (Locator locator : locators) {
				String xpath = locator.getMetadata("xpath");
				JavascriptExecutor executor = (JavascriptExecutor) driver;
				@SuppressWarnings("unchecked")
				List<String> xpathObject = (List<String>) executor.executeScript(script, xpath);

				@SuppressWarnings("unchecked")
				List<String> targetObject = (List<String>) executor.executeScript(script, elementXPath);
				
				if (removePathAttributes(targetObject.get(0)).equals(removePathAttributes(xpathObject.get(0)))) {
					Locator locatorAll = getAllLocatorsForElement(elementXPath,  driver);
					if (locatorAll != null) {
						locator.putMetadata("ide", locatorAll.getMetadata("ide"));
						locator.putMetadata("robula", locatorAll.getMetadata("robula"));
						locator.putMetadata("montoto", locatorAll.getMetadata("montoto"));
					}
					return locator;
				}
			}
		}
		return null;
	}

	public Locator getAllLocatorsForElement(String elementXPath, WebDriver webDriver) {
		List<Locator> locators = new ArrayList<Locator>();

		if (webDriver != null) {
			try {
				String javascript = loadTextFile("javascript.js");
				webDriver.manage().timeouts().setScriptTimeout(300, TimeUnit.SECONDS);
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				String script = "var xpath = arguments[0]; var result = []; " +
					"var all = []; " +
					"var element = locateElementByXPath(xpath); " +
					"if (element!=null) all.push(element); " +
					"for (var i=0, max=all.length; i < max; i++) { " +
					"    result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length, 'robula': getRobulaPlusXPath(all[i]), 'montoto': getMonotoXPath(all[i]), 'ide': getSeleniumIDELocator(all[i]), 'xpath': getXPath(all[i]), 'idxpath': getIdXPath(all[i])}); " +
					"} " +
					" return JSON.stringify(result); ";
				Object object=executor.executeScript(javascript + script, elementXPath);				
				String json=object.toString();
				System.out.println(json.toString());
				JSONParser parser = new JSONParser();
				JSONArray jsonArray = (JSONArray) parser.parse(json);
				if (jsonArray.size() < 1) {
					return null;
				}

				for (int i = 0; i < jsonArray.size(); i++) {
					JSONObject jsonObject = (JSONObject) jsonArray.get(i);

					String tag = object2String(jsonObject.get("tag"));
					if (tag != null) {
						tag = tag.toLowerCase();
					}
					String className = object2String(jsonObject.get("class"));
					String type = object2String(jsonObject.get("type"));
					String name = object2String(jsonObject.get("name"));
					String id = object2String(jsonObject.get("id"));
					String value = object2String(jsonObject.get("value"));
					String href = object2String(jsonObject.get("href"));
					String text = object2String(jsonObject.get("text"));
					String placeholder = object2String(jsonObject.get("placeholder"));
					String title = object2String(jsonObject.get("title"));
					String alt = object2String(jsonObject.get("alt"));
					String xpath = object2String(jsonObject.get("xpath"));
					String idxpath = object2String(jsonObject.get("idxpath"));
					String ide = object2String(jsonObject.get("ide"));
					String robula = object2String(jsonObject.get("robula"));
					String montoto = object2String(jsonObject.get("montoto"));
					String xStr = object2String(jsonObject.get("x"));
					String yStr = object2String(jsonObject.get("y"));
					String widthStr = object2String(jsonObject.get("width"));
					String heightStr = object2String(jsonObject.get("height"));

					int x = string2Int(xStr);
					int y = string2Int(yStr);
					int width = string2Int(widthStr);
					int height = string2Int(heightStr);

					if (width > 0 && height > 0) {
						Locator locator = new Locator();

						locator.setLocationArea(new Rectangle(x, y, width, height));
						locator.setX(x);
						locator.setY(y);
						locator.setWidth(width);
						locator.setHeight(height);

						addMetadata(locator, "tag", tag);
						addMetadata(locator, "class", className);
						addMetadata(locator, "type", type);
						addMetadata(locator, "name", name);
						addMetadata(locator, "id", id);
						addMetadata(locator, "value", value);
						addMetadata(locator, "href", href);
						if (isValidText(text)) {
							addMetadata(locator, "text", text);
						}
						addMetadata(locator, "placeholder", placeholder);
						addMetadata(locator, "title", title);
						addMetadata(locator, "alt", alt);
						addMetadata(locator, "xpath", xpath);
						addMetadata(locator, "idxpath", idxpath);
						addMetadata(locator, "x", xStr);
						addMetadata(locator, "y", yStr);
						addMetadata(locator, "height", heightStr);
						addMetadata(locator, "width", widthStr);

						int area = width * height;
						int shape = (width * 100) / height;
						addMetadata(locator, "area", "" + area);
						addMetadata(locator, "shape", "" + shape);

						addMetadata(locator, "ide", ide);
						addMetadata(locator, "robula", robula);
						addMetadata(locator, "montoto", montoto);

						String visibleText = locator.getVisibleText();
						if (visibleText != null) {
							locator.putMetadata("visible_text", visibleText);
						}
						String isButton = isButton(tag, type, className) ? "yes" : "no";
						;
						locator.putMetadata("is_button", isButton);

						locators.add(locator);
					}
				}

				if (locators.size() != 1) {
					return null;
				}
				return locators.get(0);
			} catch (Exception e) {
				return null;
			}
		}

		return null;
	}

	public Locator getXPathLocatorForElement(String elementXPath, WebDriver webDriver) {
		List<Locator> locators = new ArrayList<Locator>();

		if (webDriver != null) {
			try {
				String javascript = loadTextFile("javascript.js");
				webDriver.manage().timeouts().setScriptTimeout(300, TimeUnit.SECONDS);
				JavascriptExecutor executor = (JavascriptExecutor) webDriver;
				String script = "var xpathExpression = arguments[0];var result = []; " +
						"var all = []; " +
						"var element = locateElementByXPath(xpathExpression); " +
						"if (element!=null) all.push(element); " +
						"for (var i=0, max=all.length; i < max; i++) { " +
						"    result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length, 'xpath': getXPath(all[i]), 'idxpath': getIdXPath(all[i])}); "
						+
						"} " +
						" return JSON.stringify(result); ";

				String script1 = "var xpath = arguments[0]; var result = []; " +
						"var all = []; " +
						"var element = locateElementByXPath(xpath); " +
						"if (element!=null) all.push(element); " +
						"for (var i=0, max=all.length; i < max; i++) { " +
						"    result.push({'tag': all[i].tagName, 'class': all[i].className, 'type': all[i].type, 'name': all[i].name, 'id': all[i].id, 'value': all[i].value, 'href': all[i].href, 'text': all[i].textContent, 'placeholder': all[i].placeholder, 'title': all[i].title, 'alt': all[i].alt, 'x': getXPosition(all[i]), 'y': getYPosition(all[i]), 'width': getMaxWidth(all[i]), 'height': getMaxHeight(all[i]), 'children': all[i].children.length, 'robula': getRobulaPlusXPath(all[i]), 'montoto': getMonotoXPath(all[i]), 'ide': getSeleniumIDELocator(all[i]), 'xpath': getXPath(all[i]), 'idxpath': getIdXPath(all[i])}); " +
						"} " +
						" return JSON.stringify(result); ";
				Object object = executor.executeScript(javascript+script, elementXPath);

				String json = object.toString();
				JSONParser parser = new JSONParser();
				JSONArray jsonArray = (JSONArray) parser.parse(json);

				if (jsonArray.size() < 1) {
					return null;
				}

				for (int i = 0; i < jsonArray.size(); i++) {
					JSONObject jsonObject = (JSONObject) jsonArray.get(i);

					String tag = object2String(jsonObject.get("tag"));
					if (tag != null) {
						tag = tag.toLowerCase();
					}
					String className = object2String(jsonObject.get("class"));
					String type = object2String(jsonObject.get("type"));
					String name = object2String(jsonObject.get("name"));
					String id = object2String(jsonObject.get("id"));
					String value = object2String(jsonObject.get("value"));
					String href = object2String(jsonObject.get("href"));
					String text = object2String(jsonObject.get("text"));
					String placeholder = object2String(jsonObject.get("placeholder"));
					String title = object2String(jsonObject.get("title"));
					String alt = object2String(jsonObject.get("alt"));
					String xpath = object2String(jsonObject.get("xpath"));
					String idxpath = object2String(jsonObject.get("idxpath"));
					String xStr = object2String(jsonObject.get("x"));
					String yStr = object2String(jsonObject.get("y"));
					String widthStr = object2String(jsonObject.get("width"));
					String heightStr = object2String(jsonObject.get("height"));
			
					String ide = object2String(jsonObject.get("ide"));
					String robula = object2String(jsonObject.get("robula"));
					String montoto = object2String(jsonObject.get("montoto"));

					int x = string2Int(xStr);
					int y = string2Int(yStr);
					int width = string2Int(widthStr);
					int height = string2Int(heightStr);

					if (width > 0 && height > 0) {
						Locator locator = new Locator();

						locator.setLocationArea(new Rectangle(x, y, width, height));
						locator.setX(x);
						locator.setY(y);
						locator.setWidth(width);
						locator.setHeight(height);

						addMetadata(locator, "tag", tag);
						addMetadata(locator, "class", className);
						addMetadata(locator, "type", type);
						addMetadata(locator, "name", name);
						addMetadata(locator, "id", id);
						addMetadata(locator, "value", value);
						addMetadata(locator, "href", href);
						if (isValidText(text)) {
							addMetadata(locator, "text", text);
						}
						addMetadata(locator, "placeholder", placeholder);
						addMetadata(locator, "title", title);
						addMetadata(locator, "alt", alt);
						addMetadata(locator, "xpath", xpath);
						addMetadata(locator, "idxpath", idxpath);
						addMetadata(locator, "x", xStr);
						addMetadata(locator, "y", yStr);
						addMetadata(locator, "height", heightStr);
						addMetadata(locator, "width", widthStr);

						int area = width * height;
						int shape = (width * 100) / height;
						addMetadata(locator, "area", "" + area);
						addMetadata(locator, "shape", "" + shape);

						addMetadata(locator, "ide", ide);
						addMetadata(locator, "robula", robula);
						addMetadata(locator, "montoto", montoto);

						String visibleText = locator.getVisibleText();
						if (visibleText != null) {
							locator.putMetadata("visible_text", visibleText);
						}
						String isButton = isButton(tag, type, className) ? "yes" : "no";
						;
						locator.putMetadata("is_button", isButton);

						locators.add(locator);
					}
				}

				if (locators.size() != 1) {
					return null;
				}
				return locators.get(0);
			} catch (Exception e) {
				return null;
			}
		}

		return null;
	}

	public static void addMetadata(Locator locator, String key, String value) {
		if (value != null && value.length() > 0) {
			String lowercaseValue = value;
			locator.putMetadata(key, lowercaseValue);
		}
	}

	private void addNeighborTexts(Locator locator, List<Locator> availableLocators) {
		if (locator.getLocationArea() == null) {
			return;
		}
		Rectangle r = locator.getLocationArea();
		if (r.height > 100 || r.width > 600) {
			return;
		}
		Rectangle largerRectangle = new Rectangle(r.x - 50, r.y - 50, r.width + 100, r.height + 100);

		List<Locator> neighbors = new ArrayList<Locator>();
		for (Locator available : availableLocators) {
			if (locator != available && available.getLocationArea() != null) {
				Rectangle rect = available.getLocationArea();
				if (rect.getHeight() <= 100 && largerRectangle.intersects(rect)) {
					neighbors.add(available);
				}
			}
		}

		List<String> words = new ArrayList<String>();
		Properties wordHash = new Properties();
		for (Locator neighbor : neighbors) {
			String visibleText = neighbor.getVisibleText();
			if (visibleText != null) {
				String[] visibleWords = visibleText.split("\\s+");
				for (String visibleWord : visibleWords) {
					String visibleWordLower = visibleWord.toLowerCase();
					if (!wordHash.containsKey(visibleWordLower)) {
						wordHash.put(visibleWordLower, true);
						words.add(visibleWordLower);
					}
				}
			}
		}

		StringBuffer wordString = new StringBuffer();
		for (String word : words) {
			if (wordString.length() > 0) {
				wordString.append(" ");
			}
			wordString.append(word);
		}

		if (wordString.length() > 0) {
			String text = wordString.toString();
			locator.putMetadata("neighbor_text", text);
		}
	}

	private boolean containsWord(String containsWord, String[] words) {
		for (String word : words) {
			if (containsWord.length() < word.length()
					&& (word.startsWith(containsWord) || word.endsWith(containsWord))) {
				return true;
			} else if (word.length() < containsWord.length()
					&& (containsWord.startsWith(word) || containsWord.endsWith(word))) {
				return true;
			} else if (containsWord.equals(word)) {
				return true;
			}
		}
		return false;
	}

	private int neighborTextSimilarity(String text1, String text2, int maxScore) {
		if (text1.length() == 0 || text2.length() == 0) {
			return 0;
		}

		String[] words1 = text1.split("\\s+");
		String[] words2 = text2.split("\\s+");

		int existsCount = 0;
		int wordCount = Math.max(text1.length() - words1.length + 1, text2.length() - words2.length + 1);
		for (String word1 : words1) {
			if (containsWord(word1, words2)) {
				existsCount += word1.length();
			}
		}
		int score = Math.min((existsCount * maxScore) / wordCount, 100);
		return score;
	}

	public static String object2String(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof String) {
			String s = (String) o;
			return s.trim();
		} else if (o instanceof Integer) {
			Integer i = (Integer) o;
			return i.toString();
		}
		if (o instanceof Double) {
			Double d = (Double) o;
			int i = d.intValue();
			return "" + i;
		} else if (o instanceof Long) {
			Long l = (Long) o;
			return l.toString();
		}
		return null;
	}

	public static int string2Int(String text) {
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean isValidText(String text) {
		if (text == null) {
			return false;
		}
		String trimmedText = text.trim();
		if (trimmedText.length() < 3 || trimmedText.length() > 50) {
			// Too short or too long
			return false;
		}
		if (trimmedText.indexOf('\n') >= 0) {
			// Contains newline
			return false;
		}
		if (trimmedText.indexOf('\t') >= 0) {
			// Contains tab
			return false;
		}
		return true;
	}

	public static boolean isButton(String tag, String type, String className) {
		if (tag == null) {
			return false;
		}
		if (tag.equalsIgnoreCase("a") && className != null && className.indexOf("btn") >= 0) {
			return true;
		}
		if (tag.equalsIgnoreCase("button")) {
			return true;
		}
		if (tag.equalsIgnoreCase("input") && ("button".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type)
				|| "reset".equalsIgnoreCase(type))) {
			return true;
		}
		return false;
	}

	private List<String> readLines(File file) {
		List<String> lines = new ArrayList<String>();
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.length() > 0) {
					lines.add(line);
				}
			}
			scanner.close();
		} catch (Exception e) {
		}
		return lines;
	}

	public String loadTextFile(String filename) {
		File file = new File(filename);
		if (file.exists()) {
			List<String> lines = readLines(file);
			StringBuffer buf = new StringBuffer();
			for (String line : lines) {
				buf.append(line);
				buf.append("\n");
			}
			return buf.toString();
		}
		return null;
	}

	/**
	 * Delay the thread a number of milliseconds
	 * 
	 * @param milliseconds
	 */
	public void delay(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
		}
	}
}
