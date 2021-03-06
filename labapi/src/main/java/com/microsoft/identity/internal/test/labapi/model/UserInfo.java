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
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import org.threeten.bp.OffsetDateTime;
/**
 * UserInfo
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-06-01T10:19:44.716-07:00[America/Los_Angeles]")
public class UserInfo {
  @SerializedName("objectId")
  private String objectId = null;

  @SerializedName("homeObjectId")
  private String homeObjectId = null;

  @SerializedName("userType")
  private String userType = null;

  @SerializedName("userRole")
  private String userRole = null;

  @SerializedName("displayName")
  private String displayName = null;

  @SerializedName("givenName")
  private String givenName = null;

  @SerializedName("surName")
  private String surName = null;

  @SerializedName("licenses")
  private String licenses = null;

  @SerializedName("domainAccount")
  private String domainAccount = null;

  @SerializedName("upn")
  private String upn = null;

  @SerializedName("mfa")
  private String mfa = null;

  @SerializedName("protectionPolicy")
  private String protectionPolicy = null;

  @SerializedName("homeDomain")
  private String homeDomain = null;

  @SerializedName("tenantID")
  private String tenantID = null;

  @SerializedName("homeTenantID")
  private String homeTenantID = null;

  @SerializedName("homeUPN")
  private String homeUPN = null;

  @SerializedName("guestHomeAzureEnvironment")
  private String guestHomeAzureEnvironment = null;

  @SerializedName("b2cProvider")
  private String b2cProvider = null;

  @SerializedName("guestHomedIn")
  private String guestHomedIn = null;

  @SerializedName("hasAltID")
  private String hasAltID = null;

  @SerializedName("altIdSource")
  private String altIdSource = null;

  @SerializedName("altIdType")
  private String altIdType = null;

  @SerializedName("nonRoutableID")
  private String nonRoutableID = null;

  @SerializedName("tokenLifeTimePolicy")
  private String tokenLifeTimePolicy = null;

  @SerializedName("tokenType")
  private String tokenType = null;

  @SerializedName("tokenLifeTime")
  private String tokenLifeTime = null;

  @SerializedName("labName")
  private String labName = null;

  @SerializedName("lastUpdatedBy")
  private String lastUpdatedBy = null;

  @SerializedName("lastUpdatedDate")
  private String lastUpdatedDate = null;

  public UserInfo objectId(String objectId) {
    this.objectId = objectId;
    return this;
  }

   /**
   * Get objectId
   * @return objectId
  **/
  @Schema(description = "")
  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public UserInfo homeObjectId(String homeObjectId) {
    this.homeObjectId = homeObjectId;
    return this;
  }

   /**
   * Get homeObjectId
   * @return homeObjectId
  **/
  @Schema(description = "")
  public String getHomeObjectId() {
    return homeObjectId;
  }

  public void setHomeObjectId(String homeObjectId) {
    this.homeObjectId = homeObjectId;
  }

  public UserInfo userType(String userType) {
    this.userType = userType;
    return this;
  }

   /**
   * Get userType
   * @return userType
  **/
  @Schema(description = "")
  public String getUserType() {
    return userType;
  }

  public void setUserType(String userType) {
    this.userType = userType;
  }

  public UserInfo userRole(String userRole) {
    this.userRole = userRole;
    return this;
  }

   /**
   * Get userRole
   * @return userRole
  **/
  @Schema(description = "")
  public String getUserRole() {
    return userRole;
  }

  public void setUserRole(String userRole) {
    this.userRole = userRole;
  }

  public UserInfo displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

   /**
   * Get displayName
   * @return displayName
  **/
  @Schema(description = "")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public UserInfo givenName(String givenName) {
    this.givenName = givenName;
    return this;
  }

   /**
   * Get givenName
   * @return givenName
  **/
  @Schema(description = "")
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public UserInfo surName(String surName) {
    this.surName = surName;
    return this;
  }

   /**
   * Get surName
   * @return surName
  **/
  @Schema(description = "")
  public String getSurName() {
    return surName;
  }

  public void setSurName(String surName) {
    this.surName = surName;
  }

  public UserInfo licenses(String licenses) {
    this.licenses = licenses;
    return this;
  }

   /**
   * Get licenses
   * @return licenses
  **/
  @Schema(description = "")
  public String getLicenses() {
    return licenses;
  }

  public void setLicenses(String licenses) {
    this.licenses = licenses;
  }

  public UserInfo domainAccount(String domainAccount) {
    this.domainAccount = domainAccount;
    return this;
  }

   /**
   * Get domainAccount
   * @return domainAccount
  **/
  @Schema(description = "")
  public String getDomainAccount() {
    return domainAccount;
  }

  public void setDomainAccount(String domainAccount) {
    this.domainAccount = domainAccount;
  }

  public UserInfo upn(String upn) {
    this.upn = upn;
    return this;
  }

   /**
   * Get upn
   * @return upn
  **/
  @Schema(description = "")
  public String getUpn() {
    return upn;
  }

  public void setUpn(String upn) {
    this.upn = upn;
  }

  public UserInfo mfa(String mfa) {
    this.mfa = mfa;
    return this;
  }

   /**
   * Get mfa
   * @return mfa
  **/
  @Schema(description = "")
  public String getMfa() {
    return mfa;
  }

  public void setMfa(String mfa) {
    this.mfa = mfa;
  }

  public UserInfo protectionPolicy(String protectionPolicy) {
    this.protectionPolicy = protectionPolicy;
    return this;
  }

   /**
   * Get protectionPolicy
   * @return protectionPolicy
  **/
  @Schema(description = "")
  public String getProtectionPolicy() {
    return protectionPolicy;
  }

  public void setProtectionPolicy(String protectionPolicy) {
    this.protectionPolicy = protectionPolicy;
  }

  public UserInfo homeDomain(String homeDomain) {
    this.homeDomain = homeDomain;
    return this;
  }

   /**
   * Get homeDomain
   * @return homeDomain
  **/
  @Schema(description = "")
  public String getHomeDomain() {
    return homeDomain;
  }

  public void setHomeDomain(String homeDomain) {
    this.homeDomain = homeDomain;
  }

  public UserInfo tenantID(String tenantID) {
    this.tenantID = tenantID;
    return this;
  }

   /**
   * Get tenantID
   * @return tenantID
  **/
  @Schema(description = "")
  public String getTenantID() {
    return tenantID;
  }

  public void setTenantID(String tenantID) {
    this.tenantID = tenantID;
  }

  public UserInfo homeTenantID(String homeTenantID) {
    this.homeTenantID = homeTenantID;
    return this;
  }

   /**
   * Get homeTenantID
   * @return homeTenantID
  **/
  @Schema(description = "")
  public String getHomeTenantID() {
    return homeTenantID;
  }

  public void setHomeTenantID(String homeTenantID) {
    this.homeTenantID = homeTenantID;
  }

  public UserInfo homeUPN(String homeUPN) {
    this.homeUPN = homeUPN;
    return this;
  }

   /**
   * Get homeUPN
   * @return homeUPN
  **/
  @Schema(description = "")
  public String getHomeUPN() {
    return homeUPN;
  }

  public void setHomeUPN(String homeUPN) {
    this.homeUPN = homeUPN;
  }

  public UserInfo guestHomeAzureEnvironment(String guestHomeAzureEnvironment) {
    this.guestHomeAzureEnvironment = guestHomeAzureEnvironment;
    return this;
  }

   /**
   * Get guestHomeAzureEnvironment
   * @return guestHomeAzureEnvironment
  **/
  @Schema(description = "")
  public String getGuestHomeAzureEnvironment() {
    return guestHomeAzureEnvironment;
  }

  public void setGuestHomeAzureEnvironment(String guestHomeAzureEnvironment) {
    this.guestHomeAzureEnvironment = guestHomeAzureEnvironment;
  }

  public UserInfo b2cProvider(String b2cProvider) {
    this.b2cProvider = b2cProvider;
    return this;
  }

   /**
   * Get b2cProvider
   * @return b2cProvider
  **/
  @Schema(description = "")
  public String getB2cProvider() {
    return b2cProvider;
  }

  public void setB2cProvider(String b2cProvider) {
    this.b2cProvider = b2cProvider;
  }

  public UserInfo guestHomedIn(String guestHomedIn) {
    this.guestHomedIn = guestHomedIn;
    return this;
  }

   /**
   * Get guestHomedIn
   * @return guestHomedIn
  **/
  @Schema(description = "")
  public String getGuestHomedIn() {
    return guestHomedIn;
  }

  public void setGuestHomedIn(String guestHomedIn) {
    this.guestHomedIn = guestHomedIn;
  }

  public UserInfo hasAltID(String hasAltID) {
    this.hasAltID = hasAltID;
    return this;
  }

   /**
   * Get hasAltID
   * @return hasAltID
  **/
  @Schema(description = "")
  public String getHasAltID() {
    return hasAltID;
  }

  public void setHasAltID(String hasAltID) {
    this.hasAltID = hasAltID;
  }

  public UserInfo altIdSource(String altIdSource) {
    this.altIdSource = altIdSource;
    return this;
  }

   /**
   * Get altIdSource
   * @return altIdSource
  **/
  @Schema(description = "")
  public String getAltIdSource() {
    return altIdSource;
  }

  public void setAltIdSource(String altIdSource) {
    this.altIdSource = altIdSource;
  }

  public UserInfo altIdType(String altIdType) {
    this.altIdType = altIdType;
    return this;
  }

   /**
   * Get altIdType
   * @return altIdType
  **/
  @Schema(description = "")
  public String getAltIdType() {
    return altIdType;
  }

  public void setAltIdType(String altIdType) {
    this.altIdType = altIdType;
  }

  public UserInfo nonRoutableID(String nonRoutableID) {
    this.nonRoutableID = nonRoutableID;
    return this;
  }

   /**
   * Get nonRoutableID
   * @return nonRoutableID
  **/
  @Schema(description = "")
  public String getNonRoutableID() {
    return nonRoutableID;
  }

  public void setNonRoutableID(String nonRoutableID) {
    this.nonRoutableID = nonRoutableID;
  }

  public UserInfo tokenLifeTimePolicy(String tokenLifeTimePolicy) {
    this.tokenLifeTimePolicy = tokenLifeTimePolicy;
    return this;
  }

   /**
   * Get tokenLifeTimePolicy
   * @return tokenLifeTimePolicy
  **/
  @Schema(description = "")
  public String getTokenLifeTimePolicy() {
    return tokenLifeTimePolicy;
  }

  public void setTokenLifeTimePolicy(String tokenLifeTimePolicy) {
    this.tokenLifeTimePolicy = tokenLifeTimePolicy;
  }

  public UserInfo tokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

   /**
   * Get tokenType
   * @return tokenType
  **/
  @Schema(description = "")
  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public UserInfo tokenLifeTime(String tokenLifeTime) {
    this.tokenLifeTime = tokenLifeTime;
    return this;
  }

   /**
   * Get tokenLifeTime
   * @return tokenLifeTime
  **/
  @Schema(description = "")
  public String getTokenLifeTime() {
    return tokenLifeTime;
  }

  public void setTokenLifeTime(String tokenLifeTime) {
    this.tokenLifeTime = tokenLifeTime;
  }

  public UserInfo labName(String labName) {
    this.labName = labName;
    return this;
  }

   /**
   * Get labName
   * @return labName
  **/
  @Schema(description = "")
  public String getLabName() {
    return labName;
  }

  public void setLabName(String labName) {
    this.labName = labName;
  }

  public UserInfo lastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }

   /**
   * Get lastUpdatedBy
   * @return lastUpdatedBy
  **/
  @Schema(description = "")
  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public void setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  public UserInfo lastUpdatedDate(String lastUpdatedDate) {
    this.lastUpdatedDate = lastUpdatedDate;
    return this;
  }

   /**
   * Get lastUpdatedDate
   * @return lastUpdatedDate
  **/
  @Schema(description = "")
  public String getLastUpdatedDate() {
    return lastUpdatedDate;
  }

  public void setLastUpdatedDate(String lastUpdatedDate) {
    this.lastUpdatedDate = lastUpdatedDate;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserInfo userInfo = (UserInfo) o;
    return Objects.equals(this.objectId, userInfo.objectId) &&
        Objects.equals(this.homeObjectId, userInfo.homeObjectId) &&
        Objects.equals(this.userType, userInfo.userType) &&
        Objects.equals(this.userRole, userInfo.userRole) &&
        Objects.equals(this.displayName, userInfo.displayName) &&
        Objects.equals(this.givenName, userInfo.givenName) &&
        Objects.equals(this.surName, userInfo.surName) &&
        Objects.equals(this.licenses, userInfo.licenses) &&
        Objects.equals(this.domainAccount, userInfo.domainAccount) &&
        Objects.equals(this.upn, userInfo.upn) &&
        Objects.equals(this.mfa, userInfo.mfa) &&
        Objects.equals(this.protectionPolicy, userInfo.protectionPolicy) &&
        Objects.equals(this.homeDomain, userInfo.homeDomain) &&
        Objects.equals(this.tenantID, userInfo.tenantID) &&
        Objects.equals(this.homeTenantID, userInfo.homeTenantID) &&
        Objects.equals(this.homeUPN, userInfo.homeUPN) &&
        Objects.equals(this.guestHomeAzureEnvironment, userInfo.guestHomeAzureEnvironment) &&
        Objects.equals(this.b2cProvider, userInfo.b2cProvider) &&
        Objects.equals(this.guestHomedIn, userInfo.guestHomedIn) &&
        Objects.equals(this.hasAltID, userInfo.hasAltID) &&
        Objects.equals(this.altIdSource, userInfo.altIdSource) &&
        Objects.equals(this.altIdType, userInfo.altIdType) &&
        Objects.equals(this.nonRoutableID, userInfo.nonRoutableID) &&
        Objects.equals(this.tokenLifeTimePolicy, userInfo.tokenLifeTimePolicy) &&
        Objects.equals(this.tokenType, userInfo.tokenType) &&
        Objects.equals(this.tokenLifeTime, userInfo.tokenLifeTime) &&
        Objects.equals(this.labName, userInfo.labName) &&
        Objects.equals(this.lastUpdatedBy, userInfo.lastUpdatedBy) &&
        Objects.equals(this.lastUpdatedDate, userInfo.lastUpdatedDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectId, homeObjectId, userType, userRole, displayName, givenName, surName, licenses, domainAccount, upn, mfa, protectionPolicy, homeDomain, tenantID, homeTenantID, homeUPN, guestHomeAzureEnvironment, b2cProvider, guestHomedIn, hasAltID, altIdSource, altIdType, nonRoutableID, tokenLifeTimePolicy, tokenType, tokenLifeTime, labName, lastUpdatedBy, lastUpdatedDate);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserInfo {\n");
    
    sb.append("    objectId: ").append(toIndentedString(objectId)).append("\n");
    sb.append("    homeObjectId: ").append(toIndentedString(homeObjectId)).append("\n");
    sb.append("    userType: ").append(toIndentedString(userType)).append("\n");
    sb.append("    userRole: ").append(toIndentedString(userRole)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    givenName: ").append(toIndentedString(givenName)).append("\n");
    sb.append("    surName: ").append(toIndentedString(surName)).append("\n");
    sb.append("    licenses: ").append(toIndentedString(licenses)).append("\n");
    sb.append("    domainAccount: ").append(toIndentedString(domainAccount)).append("\n");
    sb.append("    upn: ").append(toIndentedString(upn)).append("\n");
    sb.append("    mfa: ").append(toIndentedString(mfa)).append("\n");
    sb.append("    protectionPolicy: ").append(toIndentedString(protectionPolicy)).append("\n");
    sb.append("    homeDomain: ").append(toIndentedString(homeDomain)).append("\n");
    sb.append("    tenantID: ").append(toIndentedString(tenantID)).append("\n");
    sb.append("    homeTenantID: ").append(toIndentedString(homeTenantID)).append("\n");
    sb.append("    homeUPN: ").append(toIndentedString(homeUPN)).append("\n");
    sb.append("    guestHomeAzureEnvironment: ").append(toIndentedString(guestHomeAzureEnvironment)).append("\n");
    sb.append("    b2cProvider: ").append(toIndentedString(b2cProvider)).append("\n");
    sb.append("    guestHomedIn: ").append(toIndentedString(guestHomedIn)).append("\n");
    sb.append("    hasAltID: ").append(toIndentedString(hasAltID)).append("\n");
    sb.append("    altIdSource: ").append(toIndentedString(altIdSource)).append("\n");
    sb.append("    altIdType: ").append(toIndentedString(altIdType)).append("\n");
    sb.append("    nonRoutableID: ").append(toIndentedString(nonRoutableID)).append("\n");
    sb.append("    tokenLifeTimePolicy: ").append(toIndentedString(tokenLifeTimePolicy)).append("\n");
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
    sb.append("    tokenLifeTime: ").append(toIndentedString(tokenLifeTime)).append("\n");
    sb.append("    labName: ").append(toIndentedString(labName)).append("\n");
    sb.append("    lastUpdatedBy: ").append(toIndentedString(lastUpdatedBy)).append("\n");
    sb.append("    lastUpdatedDate: ").append(toIndentedString(lastUpdatedDate)).append("\n");
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
