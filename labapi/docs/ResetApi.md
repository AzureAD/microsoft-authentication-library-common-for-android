# ResetApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiResetPut**](ResetApi.md#apiResetPut) | **PUT** /api/Reset | Resets MFA/Password for a lab test user based on UPN.   You need to provide the UPN and Operation(MFA/Password) in Query String.

<a name="apiResetPut"></a>
# **apiResetPut**
> CustomSuccessResponse apiResetPut(upn, operation)

Resets MFA/Password for a lab test user based on UPN.   You need to provide the UPN and Operation(MFA/Password) in Query String.

Provides generic error messages

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ResetApi;


ResetApi apiInstance = new ResetApi();
String upn = "upn_example"; // String | Enter the Lab User UPN
String operation = "operation_example"; // String | Allowed Values : \"MFA\", Password\"
try {
    CustomSuccessResponse result = apiInstance.apiResetPut(upn, operation);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ResetApi#apiResetPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **upn** | **String**| Enter the Lab User UPN | [optional]
 **operation** | **String**| Allowed Values : \&quot;MFA\&quot;, Password\&quot; | [optional]

### Return type

[**CustomSuccessResponse**](CustomSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

