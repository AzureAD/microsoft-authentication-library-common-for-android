# LabAvailabilityApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiLabAvailabilityGet**](LabAvailabilityApi.md#apiLabAvailabilityGet) | **GET** /api/LabAvailability | Will give you the current status of the labs

<a name="apiLabAvailabilityGet"></a>
# **apiLabAvailabilityGet**
> List&lt;LabAvailability&gt; apiLabAvailabilityGet()

Will give you the current status of the labs

Query this when a test fails to see if the failure is because of a lab. If so retry after sometime

### Example
```java
// Import classes:
//import com.microsoft.identity.internal.test.labapi.ApiException;
//import com.microsoft.identity.internal.test.labapi.api.LabAvailabilityApi;


LabAvailabilityApi apiInstance = new LabAvailabilityApi();
try {
    List<LabAvailability> result = apiInstance.apiLabAvailabilityGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabAvailabilityApi#apiLabAvailabilityGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;LabAvailability&gt;**](LabAvailability.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

