# TempUserInfoApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiTempUserInfoGet**](TempUserInfoApi.md#apiTempUserInfoGet) | **GET** /api/TempUserInfo | Let&#x27;s you query for Locked Users and get user specific details.

<a name="apiTempUserInfoGet"></a>
# **apiTempUserInfoGet**
> TempUserInfo apiTempUserInfoGet(upn)

Let&#x27;s you query for Locked Users and get user specific details.

When you create temporary user, it gets created with \&quot;Locked_\&quot; in it&#x27;s name so we can delete the account in 90 minutes.

### Example
```java
// Import classes:
//import com.microsoft.identity.internal.test.labapi.ApiException;
//import com.microsoft.identity.internal.test.labapi.api.TempUserInfoApi;


TempUserInfoApi apiInstance = new TempUserInfoApi();
String upn = "upn_example"; // String | Enter the Locked User UPN
try {
    TempUserInfo result = apiInstance.apiTempUserInfoGet(upn);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TempUserInfoApi#apiTempUserInfoGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **upn** | **String**| Enter the Locked User UPN | [optional]

### Return type

[**TempUserInfo**](TempUserInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

