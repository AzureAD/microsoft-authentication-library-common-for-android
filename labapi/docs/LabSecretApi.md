# LabSecretApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiLabSecretGet**](LabSecretApi.md#apiLabSecretGet) | **GET** /api/LabSecret | Gets the Lab Secret from Identity Labs Secret (msidlabs) KeyVault.   You need to provide the secret in Query String.

<a name="apiLabSecretGet"></a>
# **apiLabSecretGet**
> SecretResponse apiLabSecretGet(secret)

Gets the Lab Secret from Identity Labs Secret (msidlabs) KeyVault.   You need to provide the secret in Query String.

If not found it will return the KeyVault providers generic error message &#x27;not found&#x27;

### Example
```java
// Import classes:
//import com.microsoft.identity.internal.test.labapi.ApiException;
//import com.microsoft.identity.internal.test.labapi.api.LabSecretApi;


LabSecretApi apiInstance = new LabSecretApi();
String secret = "secret_example"; // String | Enter the Secret Name as the Param. e.g. 'msidlab1' or 'b2csecret'
try {
    SecretResponse result = apiInstance.apiLabSecretGet(secret);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabSecretApi#apiLabSecretGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **secret** | **String**| Enter the Secret Name as the Param. e.g. &#x27;msidlab1&#x27; or &#x27;b2csecret&#x27; | [optional]

### Return type

[**SecretResponse**](SecretResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

