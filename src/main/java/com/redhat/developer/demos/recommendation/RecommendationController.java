package com.redhat.developer.demos.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
public class RecommendationController {

    private static final String RESPONSE_STRING_FORMAT = "recommendation v5 from '%s'\n";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;

    @Value("${external.api.url:https://simpleproject.apps.openshift.rhcasalab.com/api/products}")
    private String remoteURL;

    /**
     * Counter to help us see the lifecycle
     */
    private int count = 0;

    /**
     * Flag for throwing a 503 when enabled
     */
    private boolean misbehave = false;

    private boolean timeout = false;

    private static final String HOSTNAME = parseContainerIdFromHostname(
            System.getenv().getOrDefault("HOSTNAME", "unknown"));

    static String parseContainerIdFromHostname(String hostname) {
        return hostname.replaceAll("recommendation-v\\d+-", "");
    }


    public RecommendationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/")
    public ResponseEntity<?> getRecommendations() {
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(remoteURL, String.class);
            String response = responseEntity.getBody();
            return ResponseEntity.ok(String.format(RESPONSE_STRING_FORMAT, response.trim()));
        } catch (HttpStatusCodeException ex) {
            logger.warn("Exception trying to get the response from recommendation service.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT,
                            String.format("%d %s", ex.getRawStatusCode(), createHttpErrorResponseString(ex))));
        } catch (RestClientException ex) {
            logger.warn("Exception trying to get the response from recommendation service.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT, ex.getMessage()));
        }
    }

    private String createHttpErrorResponseString(HttpStatusCodeException ex) {
        String responseBody = ex.getResponseBodyAsString().trim();
        if (responseBody.startsWith("null")) {
            return ex.getStatusCode().getReasonPhrase();
        }
        return responseBody;
    }

    private void timeout() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.info("Thread interrupted");
        }
    }

    private ResponseEntity<String> doMisbehavior() {
        count = 0;
        misbehave = false;
        logger.debug(String.format("Misbehaving %d", count));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(String.format("recommendation misbehavior from '%s'\n", HOSTNAME));
    }

    @RequestMapping("/misbehave")
    public ResponseEntity<String> flagMisbehave() {
        this.misbehave = true;
        logger.debug("'misbehave' has been set to 'true'");
        return ResponseEntity.ok("Next request to / will return a 503\n");
    }

    @RequestMapping("/timeout")
    public ResponseEntity<String> flagTimeout() {
        this.timeout = true;
        logger.debug("'timeout' has been set to 'true'");
        return ResponseEntity.ok("Next request to / will take 5sec\n");
    }

}
