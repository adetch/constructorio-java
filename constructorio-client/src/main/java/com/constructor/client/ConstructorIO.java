package com.constructor.client;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import org.json.JSONArray;
import java.io.UnsupportedEncodingException;

import com.google.gson.Gson;
// inconsistent capitalization: Unirest guys use Json, Crockford lib uses JSON
// Gson is just camelcaps, so Crockford is odd one out
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Constructor.io Client
 * @author Howon Lee <howon@constructor.io>
 * @version 0.0.1
 * @since Oct 19 2015
 * Go to Constructor.io for really profitable autocomplete-as-a-service.
 */
public class ConstructorIO
{

	public String apiToken;
	public String autocompleteKey;
	public String protocol;
	public String host;
	protected URLEncodedUtils encoder;

	/**
	 * Creates a constructor.io client.
	 * Unlike in the other Constructor.io clients, this one only takes in hashmaps,
	 * and you must write other helper methods to serialize other things.
	 * @param apiToken API Token, gotten from your <a href="https://constructor.io/dashboard">Constructor.io Dashboard</a>, and kept secret.
	 * @param autocompleteKey Autocomplete key, used publically in your in-site javascript client.
	 * @param isHTTPS true to use HTTPS, false to use HTTP. It is highly recommended that you use HTTPS.
	 * @param host The host of the autocomplete service that you are using. It is recommended that you let this value be null, in which case the host defaults to the Constructor.io autocomplete servic at ac.cnstrc.com.
	 */
	public ConstructorIO (String apiToken, String autocompleteKey, boolean isHTTPS, String host) {
		this.apiToken = apiToken;
		this.autocompleteKey = autocompleteKey;
		this.host = host;
		if (host == null) {
			this.host = "ac.cnstrc.com";
		}
		if (isHTTPS) {
			this.protocol = "https";
		} else {
			this.protocol = "http";
		}
		this.encoder = new URLEncodedUtils();
		Unirest.setDefaultHeader("Content-Type", "application/json");
	}

	/**
	 * Serializes url params in a rudimentary way.
	 *
	 * Unlike in the other Constructor.io clients, this one only takes in hashmaps,
	 * and you must write other helper methods to serialize other things.
	 * @param params HashMap of the parameters to encode.
	 * @return The encoded parameters, as a String.
	 */
	public static String serializeParams(HashMap<String, String> params) throws UnsupportedEncodingException {
		String urlString = "";
		boolean isFirst = true;
		for (String key: params.keySet()) {
			if (!isFirst) {
				urlString += "&";
			}
			urlString += URLEncoder.encode(key, "UTF-8");
			urlString += "=";
			urlString += URLEncoder.encode(params.get(key), "UTF-8");
			isFirst = false;
		}
		return urlString;
	}

	/**
	 * Makes a URL to issue the requests to.
	 *
	 * Note that the URL will automagically have the autocompleteKey embedded.
	 * @param endpoint Endpoint of the autocomplete service.
	 * @return The created URL. Now you can use it to issue requests and things!
	 */
	public String makeUrl(String endpoint) throws UnsupportedEncodingException {
		return makeUrl(endpoint, new HashMap<String, String>());
	}

	/**
	 * Makes a URL to issue the requests to.
	 *
	 * Note that the URL will automagically have the autocompleteKey embedded.
	 * @param endpoint Endpoint of the autocomplete service you are giving requests to
	 * @param params HashMap of the parameters you're encoding in the URL
	 * @return The created URL. Now you can use it to issue requests and things!
	 */
	public String makeUrl(String endpoint, HashMap<String, String> params) throws UnsupportedEncodingException {
		params.put("autocomplete_key", this.autocompleteKey);
		return String.format("%s://%s/%s?%s", this.protocol, this.host, endpoint, this.serializeParams(params));
	}

	private static String createItemParams(String itemName, String autocompleteSection) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("item_name", itemName);
		params.put("autocomplete_section", autocompleteSection);
		Gson gson = new Gson();
		return gson.toJson(params);
	}
	
	private static String createItemParams(String itemName, String autocompleteSection, Map<String, Object> otherJsonParams) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("item_name", itemName);
		params.put("autocomplete_section", autocompleteSection);
		params.putAll(otherJsonParams);
		Gson gson = new Gson();
		return gson.toJson(params);
	}

	private static String createTrackingParams(String term) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("term", term);
		Gson gson = new Gson();
		return gson.toJson(params);
	}

	private static String createTrackingParams(String term, String autocompleteSection) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("term", term);
		params.put("autocomplete_section", autocompleteSection);
		Gson gson = new Gson();
		return gson.toJson(params);
	}
	
	private static String createTrackingParams(String term, String autocompleteSection, Map<String, Object> otherJsonParams) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("term", term);
		params.put("autocomplete_section", autocompleteSection);
		params.putAll(otherJsonParams);
		Gson gson = new Gson();
		return gson.toJson(params);
	}

	private static String createTrackingParams(String term, Map<String, Object> otherJsonParams) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("term", term);
		params.putAll(otherJsonParams);
		Gson gson = new Gson();
		return gson.toJson(params);
	}

	private static boolean checkResponse(HttpResponse<JsonNode> resp, int expectedStatus) throws ConstructorException {
		if (resp.getStatus() != expectedStatus) {
			throw new ConstructorException(resp.getBody().toString());
		} else {
			return true;
		}
	}

	/**
	 * Queries an autocomplete service.
	 *
	 * Note that if you're making an autocomplete service on a website, you should definitely use our javascript client instead of doing it server-side!
	 * That's important. That will be a solid latency difference.
	 * @param queryStr The string that you will be autocompleting.
	 * @return An ArrayList of suggestions for querying
	 */
	public ArrayList<String> query(String queryStr) throws ConstructorException {
		try {
			String url = this.makeUrl("autocomplete/" + queryStr);
			HttpResponse<JsonNode> jsonRes = Unirest.get(url).asJson();
			if (checkResponse(jsonRes, 200)) {
				JSONArray arr = jsonRes.getBody()
															 .getObject()
															 .getJSONArray("suggestions");
				ArrayList<String> res = new ArrayList<String>();
				if (arr != null) {
					for (int i = 0; i < arr.length(); i++) {
						res.add(arr.get(i).toString());
					}
				}
				return res;
			}
			return null; // this should not get back here
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}

	/**
	 * Verifies that an autocomplete service is working.
	 *
	 * @exception ConstructorException if the service is not working
	 * @return true if working.
	 */
	public boolean verify() throws ConstructorException {
		try {
			String url = this.makeUrl("v1/verify");
			HttpResponse<JsonNode> jsonRes = Unirest.get(url)
																							.basicAuth(this.apiToken, "")
																							.asJson();
			if (checkResponse(jsonRes, 200)) {
				return true;
			}
			return false; // this should not get back here
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Adds an item to your autocomplete.
	 *
	 * @param item the item that you're adding.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean add (ConstructorItem item, String autocompleteSection) throws ConstructorException {
		return add(item.getItemName(), autocompleteSection, item.toJsonParams());
	}

	/**
	 * Adds an item to your autocomplete.
	 *
	 * @param itemName the item that you're adding.
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean add (String itemName, String autocompleteSection) throws ConstructorException {
		return add(itemName, autocompleteSection, null);
	}

	/**
	 * Adds an item to your autocomplete.
	 *
	 * @param itemName the item that you're adding.
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @param jsonParams a map of optional parameters. Optional parameters are in the <a href="https://constructor.io/docs/#add-an-item">API documentation</a>
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean add (String itemName, String autocompleteSection, Map<String, Object> jsonParams) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/item");
			String params = (jsonParams == null ?
											ConstructorIO.createItemParams(itemName, autocompleteSection) :
											ConstructorIO.createItemParams(itemName, autocompleteSection, jsonParams));
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}

	
	/**
	 * Adds an item to your autocomplete or updates it if it already exists.
	 *
	 * @param item the item that you're adding.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean addOrUpdate(ConstructorItem item, String autocompleteSection) throws ConstructorException {
		return addOrUpdate(item.getItemName(), autocompleteSection, item.toJsonParams());
	}

	/**
	 * Adds an item to your autocomplete or updates it if it already exists.
	 *
	 * @param itemName the item that you're adding.
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean addOrUpdate(String itemName, String autocompleteSection) throws ConstructorException {
		return addOrUpdate(itemName, autocompleteSection, null);
	}

	/**
	 * Adds an item to your autocomplete or updates it if it already exists.
	 *
	 * @param itemName the item that you're adding.
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @param jsonParams a map of optional parameters. Optional parameters are in the <a href="https://constructor.io/docs/#add-an-item">API documentation</a>
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean addOrUpdate(String itemName, String autocompleteSection, Map<String, Object> jsonParams) throws ConstructorException {
		try {
			HashMap<String, String> force = new HashMap<String, String>(2);
			force.put("force", "1");
			String url = this.makeUrl("v1/item", force);
			String params = (jsonParams == null ?
											ConstructorIO.createItemParams(itemName, autocompleteSection) :
											ConstructorIO.createItemParams(itemName, autocompleteSection, jsonParams));
			HttpResponse<JsonNode> jsonRes = Unirest.put(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Adds multiple items to your autocomplete.
	 *
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @param items the items you want to add.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean addBatch(String autocompleteSection, String... items) throws ConstructorException {
		ConstructorItem[] citems = new ConstructorItem[items.length];
		for (int i = 0; i < items.length; i++) citems[i] = new ConstructorItem(items[i]);
		return addBatch(autocompleteSection, citems);
	}
	
	/**
	 * Adds multiple items to your autocomplete.
	 *
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @param items the items you want to add.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean addBatch(String autocompleteSection, ConstructorItem... items) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/batch_items");
			
			// Build JSON
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("items", items);
			data.put("autocomplete_section", autocompleteSection);
			Gson gson = new Gson();
			String params = gson.toJson(data);
			
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Adds multiple items to your autocomplete whilst updating existing ones.
	 *
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @param items the items you want to add.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean addOrUpdateBatch(String autocompleteSection, String... items) throws ConstructorException {
		ConstructorItem[] citems = new ConstructorItem[items.length];
		for (int i = 0; i < items.length; i++) citems[i] = new ConstructorItem(items[i]);
		return addOrUpdateBatch(autocompleteSection, citems);
	}
	
	/**
	 * Adds multiple items to your autocomplete whilst updating existing ones.
	 *
	 * @param autocompleteSection the section of the autocomplete that you're adding the item to.
	 * @param items the items you want to add.
	 * @return true if working
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean addOrUpdateBatch(String autocompleteSection, ConstructorItem... items) throws ConstructorException {
		try {
			HashMap<String, String> force = new HashMap<String, String>(2);
			force.put("force", "1");
			String url = this.makeUrl("v1/batch_items", force);
			
			// Build JSON
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("items", items);
			data.put("autocomplete_section", autocompleteSection);
			Gson gson = new Gson();
			String params = gson.toJson(data);
			
			HttpResponse<JsonNode> jsonRes = Unirest.put(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}

	/**
	 * Removes an item from your autocomplete.
	 *
	 * @param item the item that you're removing.
	 * @param autocompleteSection the section of the autocomplete that you're removing the item from.
	 * @return true if successfully removed
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean remove(ConstructorItem item, String autocompleteSection) throws ConstructorException {
		return remove(item.getItemName(), autocompleteSection);
	}

	/**
	 * Removes an item from your autocomplete.
	 *
	 * @param itemName the item that you're removing.
	 * @param autocompleteSection the section of the autocomplete that you're removing the item from.
	 * @return true if successfully removed
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean remove(String itemName, String autocompleteSection) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/item");
			String params = ConstructorIO.createItemParams(itemName, autocompleteSection);
			HttpResponse<JsonNode> jsonRes = Unirest.delete(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Modifies an item from your autocomplete.
	 *
	 * @param oldItem the item that you're modifying.
	 * @param autocompleteSection the section of the autocomplete that you're modifying the item from.
	 * @param newItem the new item you want to replace the old one with.
	 * @return true if successfully modified
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean modify(ConstructorItem oldItem, String autocompleteSection, ConstructorItem newItem) throws ConstructorException {
		return modify(oldItem.getItemName(), autocompleteSection, newItem);
	}
	
	/**
	 * Modifies an item from your autocomplete, without renaming it.
	 *
	 * @param newItem the item that you're modifying, containing the updated values.
	 * @param autocompleteSection the section of the autocomplete that you're modifying the item from.
	 * @return true if successfully modified
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean modify(ConstructorItem newItem, String autocompleteSection) throws ConstructorException {
		return modify(newItem.getItemName(), autocompleteSection, newItem);
	}
	
	/**
	 * Modifies an item from your autocomplete.
	 *
	 * @param itemName the item that you're modifying.
	 * @param autocompleteSection the section of the autocomplete that you're modifying the item from.
	 * @param newItem the new item you want to replace the old one with.
	 * @return true if successfully modified
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean modify(String itemName, String autocompleteSection, ConstructorItem newItem) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/item");
			newItem.put("new_item_name", newItem.getItemName());
			newItem.setItemName(itemName);
			String params = newItem.toJson();
			HttpResponse<JsonNode> jsonRes = Unirest.put(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Modifies an item from your autocomplete.
	 *
	 * @param itemName the item that you're modifying.
	 * @param newItemName the new item name of the item you're modifying. If you don't wnat to change it, just put in the old itemName.
	 * @param autocompleteSection the section of the autocomplete that you're modifying the item from.
	 * @return true if successfully modified
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean modify(String itemName, String newItemName, String autocompleteSection) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/item");
			HashMap<String, Object> newItem = new HashMap<String, Object>();
			newItem.put("new_item_name", newItemName);
			String params = ConstructorIO.createItemParams(itemName, autocompleteSection, newItem);
			HttpResponse<JsonNode> jsonRes = Unirest.put(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Modifies an item from your autocomplete.
	 *
	 * @param itemName the item that you're modifying.
	 * @param newItemName the new item name of the item you're modifying. If you don't wnat to change it, just put in the old itemName.
	 * @param autocompleteSection the section of the autocomplete that you're modifying the item from.
	 * @param jsonParams a map of optional parameters. Optional parameters are in the <a href="https://constructor.io/docs/#modify-an-item">API documentation</a>
	 * @return true if successfully modified
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean modify(String itemName, String newItemName, String autocompleteSection, Map<String, Object> jsonParams) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/item");
			jsonParams.put("new_item_name", newItemName);
			String params = ConstructorIO.createItemParams(itemName, autocompleteSection, jsonParams);
			HttpResponse<JsonNode> jsonRes = Unirest.put(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Tracks the fact that someone converted on your site.
	 * Can be for any definition of conversion, whether someone buys a product or signs up or does something important to your site.
	 * @param term the term that someone converted from.
	 * @param autocompleteSection the autocomplete section that they converted from.
	 * @return true if successfully tracked.
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean trackConversion(String term, String autocompleteSection) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/conversion");
			String params = ConstructorIO.createTrackingParams(term, autocompleteSection);
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}

	/**
	 * Tracks the fact that someone converted on your site.
	 * Can be for any definition of conversion, whether someone buys a product or signs up or does something important to your site.
	 * @param term the term that someone converted from.
	 * @param autocompleteSection the autocomplete section that they converted from.
	 * @return true if successfully tracked.
	 * @param jsonParams a map of optional parameters. Optional parameters are in the <a href="https://constructor.io/docs/#track-a-conversion">API documentation</a>
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean trackConversion(String term, String autocompleteSection, Map<String, Object> jsonParams) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/conversion");
			String params = ConstructorIO.createTrackingParams(term, autocompleteSection, jsonParams);
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Tracks the fact that someone clicked through a search result on the site.
	 * @param term the term that someone clicked.
	 * @param autocompleteSection the autocomplete section of the term that they clicked.
	 * @return true if successfully tracked.
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean trackClickThrough(String term, String autocompleteSection) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/click_through");
			String params = ConstructorIO.createTrackingParams(term, autocompleteSection);
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Tracks the fact that someone clicked through a search result on the site.
	 * @param term the term that someone clicked.
	 * @param autocompleteSection the autocomplete section of the term that they clicked.
	 * @return true if successfully tracked.
	 * @param jsonParams a map of optional parameters. Optional parameters are in the <a href="https://constructor.io/docs/#track-a-click-through">API documentation</a>
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean trackClickThrough(String term, String autocompleteSection, Map<String, Object> jsonParams) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/click_through");
			String params = ConstructorIO.createTrackingParams(term, autocompleteSection, jsonParams);
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Tracks the fact that someone searched on your site.
	 * There's no autocompleteSection parameter because if you're searching, you aren't using an autocomplete.
	 * @param term the term that someone searched.
	 * @return true if successfully tracked.
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean trackSearch(String term) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/search");
			String params = ConstructorIO.createTrackingParams(term);
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
	
	/**
	 * Tracks the fact that someone searched on your site.
	 * There's no autocompleteSection parameter because if you're searching, you aren't using an autocomplete.
	 * @param term the term that someone searched.
	 * @return true if successfully tracked.
	 * @param jsonParams a map of optional parameters. Optional parameters are in the <a href="https://constructor.io/docs/#track-a-search">API documentation</a>
	 * @exception ConstructorException if the request is invalid.
	 */
	public boolean trackSearch(String term, Map<String, Object> jsonParams) throws ConstructorException {
		try {
			String url = this.makeUrl("v1/search");
			String params = ConstructorIO.createTrackingParams(term, jsonParams);
			HttpResponse<JsonNode> jsonRes = Unirest.post(url)
																							.basicAuth(this.apiToken, "")
																							.body(params)
																							.asJson();
			return checkResponse(jsonRes, 204);
		} catch (UnsupportedEncodingException encException) {
			throw new ConstructorException(encException);
		} catch (UnirestException uniException) {
			throw new ConstructorException(uniException);
		}
	}
}
