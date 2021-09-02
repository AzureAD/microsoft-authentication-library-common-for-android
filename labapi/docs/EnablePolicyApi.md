# EnablePolicyApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiEnablePolicyPut**](EnablePolicyApi.md#apiEnablePolicyPut) | **PUT** /api/EnablePolicy | lets you enable CA/Special Policies for any Locked User

<a name="apiEnablePolicyPut"></a>
# **apiEnablePolicyPut**
> CustomSuccessResponse apiEnablePolicyPut(upn, policy)

lets you enable CA/Special Policies for any Locked User

Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.   Also test users can have more than 1 policy assigned to the same user.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.EnablePolicyApi;


EnablePolicyApi apiInstance = new EnablePolicyApi();
String upn = "upn_example"; // String | Enter a valid Locked User UPN
String policy = "policy_example"; // String | Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.
try {
    CustomSuccessResponse result = apiInstance.apiEnablePolicyPut(upn, policy);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling EnablePolicyApi#apiEnablePolicyPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **upn** | **String**| Enter a valid Locked User UPN | [optional]
 **policy** | **String**| Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. | [optional]

### Return type

[**CustomSuccessResponse**](CustomSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

