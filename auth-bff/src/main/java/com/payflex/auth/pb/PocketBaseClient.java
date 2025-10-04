package com.payflex.auth.pb;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class PocketBaseClient {

  private final WebClient http;
  private final PocketBaseProperties props;

  public PocketBaseClient(PocketBaseProperties props){
    this.props = props;
    this.http = WebClient.builder().baseUrl(props.getUrl()).build();
  }


  public Mono<Map<String,Object>> authWithPassword(String identity, String password){
    String path = "/api/collections/" + props.getCollection() + "/auth-with-password";
    return http.post().uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("identity", identity, "password", password))
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {});
  }
}
