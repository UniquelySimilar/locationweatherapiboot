package com.tcoveney.locationweatherapiboot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@CrossOrigin
public class LocationWeatherController {
	private static final Logger logger = LoggerFactory.getLogger(LocationWeatherController.class);
	private final int maxLatitude = 90;
	private final int maxLongitude = 180;

	@Value("${apikey.weather}")
	private String weatherApiKey;

	@GetMapping("/data")
	public ResponseEntity<String> getLocationWeather(@RequestParam("numLocations") int numLocations) {
		String responseBody = "{ \"error\": \"Return value not set\" }";
		HttpStatus httpStatus = HttpStatus.OK;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());

		try {
			if (numLocations < 1 || numLocations > 20) {
				throw new Exception("Request parameter 'numLocations' must be between 1 and 20");
			}

			// Get random latitude values in range -90 to 90:
			String[] latitudeAry = retrieveRandomNumbers(restTemplate, numLocations, maxLatitude);
			if (null == latitudeAry) {
				throw new Exception("Could not retrieve latitude numbers.");
			}

			// Get random longitude values in range -180 to 180:
			String[] longitudeAry = retrieveRandomNumbers(restTemplate, numLocations, maxLongitude);
			if (null == longitudeAry) {
				throw new Exception("Could not retrieve longitude numbers.");
			}

			// Create and send weather API requests using numbers
			String weatherDataAry = "[";
			for (int i = 0; i < numLocations; i++) {
				String weatherData = retrieveWeather(restTemplate, latitudeAry[i], longitudeAry[i]);
				if (null == weatherData) {
					throw new Exception("Could not retrieve weather data.");
				}
				if (i < (numLocations - 1)) {
					weatherDataAry += weatherData + ",";
				} else {
					weatherDataAry += weatherData;
				}
			}

			responseBody = weatherDataAry + "]";
		} catch (Exception e) {
			logger.error(e.getMessage());
			responseBody = "{ \"error\": \"" + e.getMessage() + "\"}";
			// Set response status code general 500 for client
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return new ResponseEntity<String>(responseBody, responseHeaders, httpStatus);
	}

	private String[] retrieveRandomNumbers(RestTemplate restTemplate, int numLocations, int max) {
		String[] numAry = null;
		String url = "https://www.random.org/integers/?num=" + numLocations + "&min=-" + max + "&max=" + max
				+ "&col=1&base=10&format=plain&rnd=new";
		ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
		int statusCode = responseEntity.getStatusCodeValue();
		if (statusCode != 200) {
			logger.error("ERROR: Random number service response status: " + statusCode);
		} else {
			String body = responseEntity.getBody();
			// logger.debug("Random number service response body: " + body);
			String delimiter = System.lineSeparator();
			numAry = body.split(delimiter);
			// for (String number : numAry) {
			// logger.debug("Number array element: " + number);
			// }
		}

		return numAry;
	}

	private String retrieveWeather(RestTemplate restTemplate, String latitude, String longitude) {
		String retVal = null;
		String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude
				+ "&units=imperial&appid=" + weatherApiKey;
		ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
		int statusCode = responseEntity.getStatusCodeValue();
		if (statusCode != 200) {
			logger.error("ERROR: Weather service response status: " + statusCode);
		} else {
			retVal = responseEntity.getBody();
		}

		return retVal;
	}

	private ClientHttpRequestFactory getClientHttpRequestFactory() {
		// Use a timeout value of two minutes.
		int timeout = 2000;
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setConnectTimeout(timeout);

		return clientHttpRequestFactory;
	}
}