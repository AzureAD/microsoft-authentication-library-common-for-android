# DeleteDeviceApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiDeleteDeviceDelete**](DeleteDeviceApi.md#apiDeleteDeviceDelete) | **DELETE** /api/DeleteDevice | Delete&#x27;s a Device from AAD.   You need to provide the UPN and the Device ID (in GUID format) in Query String.

<a name="apiDeleteDeviceDelete"></a>
# **apiDeleteDeviceDelete**
> CustomSuccessResponse apiDeleteDeviceDelete(upn, deviceid)

Delete&#x27;s a Device from AAD.   You need to provide the UPN and the Device ID (in GUID format) in Query String.

Provides generic error messages

### Example
```java
// Import classes:
//import com.microsoft.identity.internal.test.labapi.ApiException;
//import com.microsoft.identity.internal.test.labapi.api.DeleteDeviceApi;


DeleteDeviceApi apiInstance = new DeleteDeviceApi();
String upn = "upn_example"; // String | Please enter a valid lab upn
String deviceid = "deviceid_example"; // String | Please enter the 32 digit Device ID in GUID Format
try {
    CustomSuccessResponse result = apiInstance.apiDeleteDeviceDelete(upn, deviceid);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DeleteDeviceApi#apiDeleteDeviceDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **upn** | **String**| Please enter a valid lab upn | [optional]
 **deviceid** | **String**| Please enter the 32 digit Device ID in GUID Format | [optional]

### Return type

[**CustomSuccessResponse**](CustomSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

