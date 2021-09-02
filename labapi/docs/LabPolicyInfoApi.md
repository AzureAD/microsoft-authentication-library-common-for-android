# LabPolicyInfoApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiLabPolicyInfoGet**](LabPolicyInfoApi.md#apiLabPolicyInfoGet) | **GET** /api/LabPolicyInfo | Will give you the different Policies available to be used with Create Temp User / Enable/Disable Policy API

<a name="apiLabPolicyInfoGet"></a>
# **apiLabPolicyInfoGet**
> CustomSuccessResponse apiLabPolicyInfoGet()

Will give you the different Policies available to be used with Create Temp User / Enable/Disable Policy API

Policy will be listed as GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. Use the LabUserInfo endpoint to query the user policy info.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.LabPolicyInfoApi;


LabPolicyInfoApi apiInstance = new LabPolicyInfoApi();
try {
    CustomSuccessResponse result = apiInstance.apiLabPolicyInfoGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabPolicyInfoApi#apiLabPolicyInfoGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**CustomSuccessResponse**](CustomSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

