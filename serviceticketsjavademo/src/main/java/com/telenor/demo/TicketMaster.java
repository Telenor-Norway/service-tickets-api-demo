package com.telenor.demo;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Hello world!
 *
 */
class TicketMaster
{

    private static final Logger logger = Logger.getLogger(TicketMaster.class.getName());
    //Authorization header name
    private static final String AUTH_HEADER = "Authorization";

    //The authentication mechanism prefix for Basic Authentication
    private static final String AUTH_HEADER_PREFIX_BASIC = "Basic "; //note the space

    //The authentication mechanism prefix for the OAUTH token
    private static final String AUTH_HEADER_PREFIX_BEARER = "Bearer "; //note the space

    //For illustrative purposes only. Get this from a secure, ecrypted configuration file
    //DO NOT put credentials in your code.
    private static final String PROPERTIES_FILE = "credentials.properties";

    private String apiBaseUrl;
    private String clientId;
    private String clientSecret;
    private String systemUsername;
    private String systemPassword;

    //Endpoints

    private static final String OAUTH_TOKEN_RESOURCE = "/oauth/v2/token";
    private static final String SERVICE_TICKET_RESOURCE = "/service-tickets/v2/incidents";

    /**
     * Will retrieve an access token to be used for the Service Ticket API.
     * @return access_token
     */
    AccessToken getAccessToken(){

        //prepare basic auth header
        String usernameAndPassword = clientId + ":" + clientSecret;
        String authorizationHeaderValue = AUTH_HEADER_PREFIX_BASIC + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );

        //prepare body of /token request
        Form form = new Form();
        form.param("grant_type", "password");
        form.param("username", systemUsername);
        form.param("password", systemPassword);

        //build and execute HTTP request
        Client client = ClientBuilder.newClient();
        Response response = client.target(apiBaseUrl)
                .path(OAUTH_TOKEN_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, authorizationHeaderValue ) // The basic authentication header goes here
                .header("Content-Type" , "application/x-www-form-urlencoded")
                .post(Entity.form(form),  Response.class);

        //Check response
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            AccessToken readEntity = response.readEntity(AccessToken.class);
            String resultTxt = readEntity.toString();
            logger.log(Level.INFO, "Result : %s", resultTxt);
            //ok, fetch a list of incidents.
            return readEntity;
        }else {
            String errorMessage = response.readEntity(String.class);
            logger.log(Level.SEVERE, "Got error status: %s ", response.getStatus());
            logger.log(Level.SEVERE, "error message : %s", errorMessage);
            return null;
        }
    }

    String listIncidents(AccessToken accessToken){



        //build and execute HTTP request
        Client client = ClientBuilder.newClient();
        Response response = client.target(apiBaseUrl)
                .path(SERVICE_TICKET_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, AUTH_HEADER_PREFIX_BEARER +accessToken.getAccess_token() ) // The Access token goes here!
                .get(Response.class);

        //Check response
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            String jsonResult = response.readEntity(String.class);
            logger.info(jsonResult);
            return jsonResult;
        }else {
            logger.log(Level.SEVERE, "Got error status: %s", response.getStatus());
            logger.log(Level.SEVERE, "Error msg: %s",response.readEntity(String.class));
            return null;
        }
    }

    TicketMaster(){
        Properties prop = new Properties();

        try {
            // load the properties file
            prop.load(getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));

            // get the property values
            apiBaseUrl = prop.getProperty("API_BASE_URL");
            clientId = prop.getProperty("CLIENT_ID");
            clientSecret = prop.getProperty("CLIENT_SECRET");
            systemUsername = prop.getProperty("SYSTEM_USERNAME");
            systemPassword = prop.getProperty("SYSTEM_PASSWORD");

            logger.log(Level.INFO, "clientId = %s",  clientId);


        } catch (IOException ex) {

            logger.log(Level.SEVERE, "Couldn't read the credentials: %s", ex.getMessage());
            System.exit(1);
        }

    }
}
