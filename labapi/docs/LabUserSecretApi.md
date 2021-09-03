# LabUserSecretApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiLabUserSecretGet**](LabUserSecretApi.md#apiLabUserSecretGet) | **GET** /api/LabUserSecret | Use LabSecret EndPoint Instead of this. This will be deprecated end of March 2020  You need to provide the secret in Query String.

<a name="apiLabUserSecretGet"></a>
# **apiLabUserSecretGet**
> SecretResponse apiLabUserSecretGet(secret)

Use LabSecret EndPoint Instead of this. This will be deprecated end of March 2020  You need to provide the secret in Query String.

If not found it will return the KeyVault providers generic error message &#x27;not found&#x27;

### Example
```java
// Import classes:
//import com.microsoft.identity.internal.test.labapi.ApiException;
//import com.microsoft.identity.internal.test.labapi.api.LabUserSecretApi;


LabUserSecretApi apiInstance = new LabUserSecretApi();
String secret = "secret_example"; // String | Enter the Lab Name as the Secret Param
try {
    SecretResponse result = apiInstance.apiLabUserSecretGet(secret);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabUserSecretApi#apiLabUserSecretGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **secret** | **String**| Enter the Lab Name as the Secret Param | [optional]

### Return type

[**SecretResponse**](SecretResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

