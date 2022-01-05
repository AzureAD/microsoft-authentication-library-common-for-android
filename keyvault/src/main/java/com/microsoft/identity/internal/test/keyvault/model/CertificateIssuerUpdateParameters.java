/*
 * KeyVaultClient
 * The key vault client performs cryptographic key operations and vault operations against the Key Vault service.
 *
 * OpenAPI spec version: 2016-10-01
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.microsoft.identity.internal.test.keyvault.model;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * The certificate issuer update parameters.
 */
@ApiModel(description = "The certificate issuer update parameters.")
public class CertificateIssuerUpdateParameters {
    @SerializedName("provider")
    private String provider = null;

    @SerializedName("credentials")
    private IssuerCredentials credentials = null;

    @SerializedName("org_details")
    private OrganizationDetails orgDetails = null;

    @SerializedName("attributes")
    private IssuerAttributes attributes = null;

    public CertificateIssuerUpdateParameters provider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * The issuer provider.
     * @return provider
     **/
    @ApiModelProperty(value = "The issuer provider.")
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public CertificateIssuerUpdateParameters credentials(IssuerCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    /**
     * The credentials to be used for the issuer.
     * @return credentials
     **/
    @ApiModelProperty(value = "The credentials to be used for the issuer.")
    public IssuerCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(IssuerCredentials credentials) {
        this.credentials = credentials;
    }

    public CertificateIssuerUpdateParameters orgDetails(OrganizationDetails orgDetails) {
        this.orgDetails = orgDetails;
        return this;
    }

    /**
     * Details of the organization as provided to the issuer.
     * @return orgDetails
     **/
    @ApiModelProperty(value = "Details of the organization as provided to the issuer.")
    public OrganizationDetails getOrgDetails() {
        return orgDetails;
    }

    public void setOrgDetails(OrganizationDetails orgDetails) {
        this.orgDetails = orgDetails;
    }

    public CertificateIssuerUpdateParameters attributes(IssuerAttributes attributes) {
        this.attributes = attributes;
        return this;
    }

    /**
     * Attributes of the issuer object.
     * @return attributes
     **/
    @ApiModelProperty(value = "Attributes of the issuer object.")
    public IssuerAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(IssuerAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CertificateIssuerUpdateParameters certificateIssuerUpdateParameters =
                (CertificateIssuerUpdateParameters) o;
        return Objects.equals(this.provider, certificateIssuerUpdateParameters.provider)
                && Objects.equals(this.credentials, certificateIssuerUpdateParameters.credentials)
                && Objects.equals(this.orgDetails, certificateIssuerUpdateParameters.orgDetails)
                && Objects.equals(this.attributes, certificateIssuerUpdateParameters.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, credentials, orgDetails, attributes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CertificateIssuerUpdateParameters {\n");

        sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
        sb.append("    credentials: ").append(toIndentedString(credentials)).append("\n");
        sb.append("    orgDetails: ").append(toIndentedString(orgDetails)).append("\n");
        sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
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
