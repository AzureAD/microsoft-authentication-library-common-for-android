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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A Deleted Certificate consisting of its previous id, attributes and its tags, as well as information on when it will be purged.
 */
@ApiModel(
        description =
                "A Deleted Certificate consisting of its previous id, attributes and its tags, as well as information on when it will be purged.")
public class DeletedCertificateBundle {
    @SerializedName("id")
    private String id = null;

    @SerializedName("kid")
    private String kid = null;

    @SerializedName("sid")
    private String sid = null;

    @SerializedName("x5t")
    private String x5t = null;

    @SerializedName("policy")
    private CertificatePolicy policy = null;

    @SerializedName("cer")
    private byte[] cer = null;

    @SerializedName("contentType")
    private String contentType = null;

    @SerializedName("attributes")
    private CertificateAttributes attributes = null;

    @SerializedName("tags")
    private Map<String, String> tags = null;

    /**
     * The certificate id.
     * @return id
     **/
    @ApiModelProperty(value = "The certificate id.")
    public String getId() {
        return id;
    }

    /**
     * The key id.
     * @return kid
     **/
    @ApiModelProperty(value = "The key id.")
    public String getKid() {
        return kid;
    }

    /**
     * The secret id.
     * @return sid
     **/
    @ApiModelProperty(value = "The secret id.")
    public String getSid() {
        return sid;
    }

    /**
     * Thumbprint of the certificate.
     * @return x5t
     **/
    @ApiModelProperty(value = "Thumbprint of the certificate.")
    public String getX5t() {
        return x5t;
    }

    /**
     * The management policy.
     * @return policy
     **/
    @ApiModelProperty(value = "The management policy.")
    public CertificatePolicy getPolicy() {
        return policy;
    }

    public DeletedCertificateBundle cer(byte[] cer) {
        this.cer = cer;
        return this;
    }

    /**
     * CER contents of x509 certificate.
     * @return cer
     **/
    @ApiModelProperty(value = "CER contents of x509 certificate.")
    public byte[] getCer() {
        return cer;
    }

    public void setCer(byte[] cer) {
        this.cer = cer;
    }

    public DeletedCertificateBundle contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * The content type of the secret.
     * @return contentType
     **/
    @ApiModelProperty(value = "The content type of the secret.")
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public DeletedCertificateBundle attributes(CertificateAttributes attributes) {
        this.attributes = attributes;
        return this;
    }

    /**
     * The certificate attributes.
     * @return attributes
     **/
    @ApiModelProperty(value = "The certificate attributes.")
    public CertificateAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(CertificateAttributes attributes) {
        this.attributes = attributes;
    }

    public DeletedCertificateBundle tags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    public DeletedCertificateBundle putTagsItem(String key, String tagsItem) {
        if (this.tags == null) {
            this.tags = new HashMap<String, String>();
        }
        this.tags.put(key, tagsItem);
        return this;
    }

    /**
     * Application specific metadata in the form of key-value pairs
     * @return tags
     **/
    @ApiModelProperty(value = "Application specific metadata in the form of key-value pairs")
    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeletedCertificateBundle deletedCertificateBundle = (DeletedCertificateBundle) o;
        return Objects.equals(this.id, deletedCertificateBundle.id)
                && Objects.equals(this.kid, deletedCertificateBundle.kid)
                && Objects.equals(this.sid, deletedCertificateBundle.sid)
                && Objects.equals(this.x5t, deletedCertificateBundle.x5t)
                && Objects.equals(this.policy, deletedCertificateBundle.policy)
                && Arrays.equals(this.cer, deletedCertificateBundle.cer)
                && Objects.equals(this.contentType, deletedCertificateBundle.contentType)
                && Objects.equals(this.attributes, deletedCertificateBundle.attributes)
                && Objects.equals(this.tags, deletedCertificateBundle.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, kid, sid, x5t, policy, Arrays.hashCode(cer), contentType, attributes, tags);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DeletedCertificateBundle {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    kid: ").append(toIndentedString(kid)).append("\n");
        sb.append("    sid: ").append(toIndentedString(sid)).append("\n");
        sb.append("    x5t: ").append(toIndentedString(x5t)).append("\n");
        sb.append("    policy: ").append(toIndentedString(policy)).append("\n");
        sb.append("    cer: ").append(toIndentedString(cer)).append("\n");
        sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
        sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
        sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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
