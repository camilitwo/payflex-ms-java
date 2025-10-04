package com.payflex.auth.pb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pocketbase")
public class PocketBaseProperties {
  private String url;
  private String collection;
  private String merchantField = "merchantId";
  private String rolesField = "roles";

  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
  public String getCollection() { return collection; }
  public void setCollection(String collection) { this.collection = collection; }
  public String getMerchantField() { return merchantField; }
  public void setMerchantField(String merchantField) { this.merchantField = merchantField; }
  public String getRolesField() { return rolesField; }
  public void setRolesField(String rolesField) { this.rolesField = rolesField; }
}
