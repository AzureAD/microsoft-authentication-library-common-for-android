//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.internal.test.labapi.model;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A secret consisting of a value, id and its attributes.
 */
public class SecretBundle {
  @SerializedName("value")
  private String value = null;

  @SerializedName("id")
  private String id = null;

  @SerializedName("contentType")
  private String contentType = null;

  @SerializedName("attributes")
  private SecretAttributes attributes = null;

  @SerializedName("tags")
  private Map<String, String> tags = null;

  @SerializedName("kid")
  private String kid = null;

  @SerializedName("managed")
  private Boolean managed = null;

  public SecretBundle value(String value) {
    this.value = value;
    return this;
  }

   /**
   * The secret value.
   * @return value
  **/
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public SecretBundle id(String id) {
    this.id = id;
    return this;
  }

   /**
   * The secret id.
   * @return id
  **/
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SecretBundle contentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

   /**
   * The content type of the secret.
   * @return contentType
  **/
  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public SecretBundle attributes(SecretAttributes attributes) {
    this.attributes = attributes;
    return this;
  }

   /**
   * The secret management attributes.
   * @return attributes
  **/
  public SecretAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(SecretAttributes attributes) {
    this.attributes = attributes;
  }

  public SecretBundle tags(Map<String, String> tags) {
    this.tags = tags;
    return this;
  }

  public SecretBundle putTagsItem(String key, String tagsItem) {
    if (this.tags == null) {
      this.tags = new HashMap<String, String>();
    }
    this.tags.put(key, tagsItem);
    return this;
  }

   /**
   * Application specific metadata in the form of key-value pairs.
   * @return tags
  **/
  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

   /**
   * If this is a secret backing a KV certificate, then this field specifies the corresponding key backing the KV certificate.
   * @return kid
  **/
  public String getKid() {
    return kid;
  }

   /**
   * True if the secret&#39;s lifetime is managed by key vault. If this is a secret backing a certificate, then managed will be true.
   * @return managed
  **/
  public Boolean isManaged() {
    return managed;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SecretBundle secretBundle = (SecretBundle) o;
    return Objects.equals(this.value, secretBundle.value) &&
        Objects.equals(this.id, secretBundle.id) &&
        Objects.equals(this.contentType, secretBundle.contentType) &&
        Objects.equals(this.attributes, secretBundle.attributes) &&
        Objects.equals(this.tags, secretBundle.tags) &&
        Objects.equals(this.kid, secretBundle.kid) &&
        Objects.equals(this.managed, secretBundle.managed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, id, contentType, attributes, tags, kid, managed);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SecretBundle {\n");
    
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    kid: ").append(toIndentedString(kid)).append("\n");
    sb.append("    managed: ").append(toIndentedString(managed)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
