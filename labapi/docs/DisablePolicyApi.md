# DisablePolicyApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiDisablePolicyPut**](DisablePolicyApi.md#apiDisablePolicyPut) | **PUT** /api/DisablePolicy | lets you disable CA/Special Policies for any Locked User

<a name="apiDisablePolicyPut"></a>
# **apiDisablePolicyPut**
> CustomSuccessResponse apiDisablePolicyPut(upn, policy)

lets you disable CA/Special Policies for any Locked User

Disable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.   Also test users can have more than 1 policy assigned to the same user.

### Example
```java
// Import classes:
//import com.microsoft.identity.internal.test.labapi.ApiException;
//import com.microsoft.identity.internal.test.labapi.api.DisablePolicyApi;


DisablePolicyApi apiInstance = new DisablePolicyApi();
String upn = "upn_example"; // String | Enter a valid Locked User UPN
String policy = "policy_example"; // String | Disable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.
try {
    CustomSuccessResponse result = apiInstance.apiDisablePolicyPut(upn, policy);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DisablePolicyApi#apiDisablePolicyPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **upn** | **String**| Enter a valid Locked User UPN | [optional]
 **policy** | **String**| Disable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. | [optional]

### Return type

[**CustomSuccessResponse**](CustomSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

