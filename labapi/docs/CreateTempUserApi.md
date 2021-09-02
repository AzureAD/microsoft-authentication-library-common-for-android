# CreateTempUserApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiCreateTempUserPost**](CreateTempUserApi.md#apiCreateTempUserPost) | **POST** /api/CreateTempUser | Let&#x27;s you create a temp (a.k.a LOCKED USER) CLOUD User for testing purposes.   All users created by this API will be auto-deleted after 90 minutes.

<a name="apiCreateTempUserPost"></a>
# **apiCreateTempUserPost**
> TempUser apiCreateTempUserPost(usertype)

Let&#x27;s you create a temp (a.k.a LOCKED USER) CLOUD User for testing purposes.   All users created by this API will be auto-deleted after 90 minutes.

You can create the following type of cloud users.        1. Basic : Account can be used for all manual testing including password resets, etc      2. GLOBALMFA : User with Global MFA      3. MFAONSPO : User requires MFA on a specific resource and the resource is SharePoint      4. MFAONEXO : User requires MFA on a specific resource and the resource is Exchange Online      5. MAMCA : User requires MAM on SharePoint      6. MDMCA : User requires MDM on SharePoint

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.CreateTempUserApi;


CreateTempUserApi apiInstance = new CreateTempUserApi();
String usertype = "usertype_example"; // String | You can create the following type of cloud users. Basic, GLOBALMFA, MFAONSPO, MFAONEXO, MAMCA, MDMCA
try {
    TempUser result = apiInstance.apiCreateTempUserPost(usertype);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CreateTempUserApi#apiCreateTempUserPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usertype** | **String**| You can create the following type of cloud users. Basic, GLOBALMFA, MFAONSPO, MFAONEXO, MAMCA, MDMCA | [optional]

### Return type

[**TempUser**](TempUser.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

