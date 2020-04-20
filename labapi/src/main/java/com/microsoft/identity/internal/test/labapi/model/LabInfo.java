/*
 * Azure Identity Labs API
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.0.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.microsoft.identity.internal.test.labapi.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModelProperty;

/**
 * LabInfo
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-04-07T02:41:56.083Z")
public class LabInfo {
  @SerializedName("labName")
  private String labName = null;

  @SerializedName("domain")
  private String domain = null;

  @SerializedName("region")
  private String region = null;

  @SerializedName("id")
  private Integer id = null;

  @SerializedName("active")
  private String active = null;

  @SerializedName("tenantId")
  private String tenantId = null;

  @SerializedName("federationProvider")
  private String federationProvider = null;

  @SerializedName("defaultLab")
  private String defaultLab = null;

  @SerializedName("azureEnvironment")
  private String azureEnvironment = null;

  @SerializedName("credentialVaultKeyName")
  private String credentialVaultKeyName = null;

  @SerializedName("authority")
  private String authority = null;

  @SerializedName("adfsEndpoint")
  private String adfsEndpoint = null;

  public LabInfo labName(String labName) {
    this.labName = labName;
    return this;
  }

   /**
   * Get labName
   * @return labName
  **/
  @ApiModelProperty(value = "")
  public String getLabName() {
    return labName;
  }

  public void setLabName(String labName) {
    this.labName = labName;
  }

  public LabInfo domain(String domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Get domain
   * @return domain
  **/
  @ApiModelProperty(value = "")
  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public LabInfo region(String region) {
    this.region = region;
    return this;
  }

   /**
   * Get region
   * @return region
  **/
  @ApiModelProperty(value = "")
  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public LabInfo id(Integer id) {
    this.id = id;
    return this;
  }

   /**
   * Get id
   * @return id
  **/
  @ApiModelProperty(value = "")
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public LabInfo active(String active) {
    this.active = active;
    return this;
  }

   /**
   * Get active
   * @return active
  **/
  @ApiModelProperty(value = "")
  public String getActive() {
    return active;
  }

  public void setActive(String active) {
    this.active = active;
  }

  public LabInfo tenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

   /**
   * Get tenantId
   * @return tenantId
  **/
  @ApiModelProperty(value = "")
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public LabInfo federationProvider(String federationProvider) {
    this.federationProvider = federationProvider;
    return this;
  }

   /**
   * Get federationProvider
   * @return federationProvider
  **/
  @ApiModelProperty(value = "")
  public String getFederationProvider() {
    return federationProvider;
  }

  public void setFederationProvider(String federationProvider) {
    this.federationProvider = federationProvider;
  }

  public LabInfo defaultLab(String defaultLab) {
    this.defaultLab = defaultLab;
    return this;
  }

   /**
   * Get defaultLab
   * @return defaultLab
  **/
  @ApiModelProperty(value = "")
  public String getDefaultLab() {
    return defaultLab;
  }

  public void setDefaultLab(String defaultLab) {
    this.defaultLab = defaultLab;
  }

  public LabInfo azureEnvironment(String azureEnvironment) {
    this.azureEnvironment = azureEnvironment;
    return this;
  }

   /**
   * Get azureEnvironment
   * @return azureEnvironment
  **/
  @ApiModelProperty(value = "")
  public String getAzureEnvironment() {
    return azureEnvironment;
  }

  public void setAzureEnvironment(String azureEnvironment) {
    this.azureEnvironment = azureEnvironment;
  }

  public LabInfo credentialVaultKeyName(String credentialVaultKeyName) {
    this.credentialVaultKeyName = credentialVaultKeyName;
    return this;
  }

   /**
   * Get credentialVaultKeyName
   * @return credentialVaultKeyName
  **/
  @ApiModelProperty(value = "")
  public String getCredentialVaultKeyName() {
    return credentialVaultKeyName;
  }

  public void setCredentialVaultKeyName(String credentialVaultKeyName) {
    this.credentialVaultKeyName = credentialVaultKeyName;
  }

  public LabInfo authority(String authority) {
    this.authority = authority;
    return this;
  }

   /**
   * Get authority
   * @return authority
  **/
  @ApiModelProperty(value = "")
  public String getAuthority() {
    return authority;
  }

  public void setAuthority(String authority) {
    this.authority = authority;
  }

  public LabInfo adfsEndpoint(String adfsEndpoint) {
    this.adfsEndpoint = adfsEndpoint;
    return this;
  }

   /**
   * Get adfsEndpoint
   * @return adfsEndpoint
  **/
  @ApiModelProperty(value = "")
  public String getAdfsEndpoint() {
    return adfsEndpoint;
  }

  public void setAdfsEndpoint(String adfsEndpoint) {
    this.adfsEndpoint = adfsEndpoint;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LabInfo labInfo = (LabInfo) o;
    return Objects.equals(this.labName, labInfo.labName) &&
        Objects.equals(this.domain, labInfo.domain) &&
        Objects.equals(this.region, labInfo.region) &&
        Objects.equals(this.id, labInfo.id) &&
        Objects.equals(this.active, labInfo.active) &&
        Objects.equals(this.tenantId, labInfo.tenantId) &&
        Objects.equals(this.federationProvider, labInfo.federationProvider) &&
        Objects.equals(this.defaultLab, labInfo.defaultLab) &&
        Objects.equals(this.azureEnvironment, labInfo.azureEnvironment) &&
        Objects.equals(this.credentialVaultKeyName, labInfo.credentialVaultKeyName) &&
        Objects.equals(this.authority, labInfo.authority) &&
        Objects.equals(this.adfsEndpoint, labInfo.adfsEndpoint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(labName, domain, region, id, active, tenantId, federationProvider, defaultLab, azureEnvironment, credentialVaultKeyName, authority, adfsEndpoint);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LabInfo {\n");
    
    sb.append("    labName: ").append(toIndentedString(labName)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    region: ").append(toIndentedString(region)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    tenantId: ").append(toIndentedString(tenantId)).append("\n");
    sb.append("    federationProvider: ").append(toIndentedString(federationProvider)).append("\n");
    sb.append("    defaultLab: ").append(toIndentedString(defaultLab)).append("\n");
    sb.append("    azureEnvironment: ").append(toIndentedString(azureEnvironment)).append("\n");
    sb.append("    credentialVaultKeyName: ").append(toIndentedString(credentialVaultKeyName)).append("\n");
    sb.append("    authority: ").append(toIndentedString(authority)).append("\n");
    sb.append("    adfsEndpoint: ").append(toIndentedString(adfsEndpoint)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

