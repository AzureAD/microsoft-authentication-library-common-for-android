# LabApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiLabLabnameGet**](LabApi.md#apiLabLabnameGet) | **GET** /api/Lab/{labname} | Gets Lab Info based on Lab Name

<a name="apiLabLabnameGet"></a>
# **apiLabLabnameGet**
> List&lt;LabInfo&gt; apiLabLabnameGet(labname)

Gets Lab Info based on Lab Name

### Example
```java
// Import classes:
//import com.microsoft.identity.internal.test.labapi.ApiException;
//import com.microsoft.identity.internal.test.labapi.api.LabApi;


LabApi apiInstance = new LabApi();
String labname = "labname_example"; // String | Provide the Lab Name to query Lab Info
try {
    List<LabInfo> result = apiInstance.apiLabLabnameGet(labname);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabApi#apiLabLabnameGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **labname** | **String**| Provide the Lab Name to query Lab Info |

### Return type

[**List&lt;LabInfo&gt;**](LabInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

