package de.kfzteile24.salesOrderHub.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CognitoAuthResponse {

    @JsonProperty("access_token")
    private String accessToken;
}
