# AppApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiAppAppidGet**](AppApi.md#apiAppAppidGet) | **GET** /api/App/{appid} | Gets App Info based on App ID
[**apiAppGet**](AppApi.md#apiAppGet) | **GET** /api/App | Gets App Info based on Azure Environment or Sign-in Audience

<a name="apiAppAppidGet"></a>
# **apiAppAppidGet**
> List&lt;AppInfo&gt; apiAppAppidGet(appid)

Gets App Info based on App ID

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.AppApi;


AppApi apiInstance = new AppApi();
String appid = "appid_example"; // String | Provide the Application ID (GUID format) to query Lab App Info
try {
    List<AppInfo> result = apiInstance.apiAppAppidGet(appid);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AppApi#apiAppAppidGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **appid** | **String**| Provide the Application ID (GUID format) to query Lab App Info |

### Return type

[**List&lt;AppInfo&gt;**](AppInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="apiAppGet"></a>
# **apiAppGet**
> List&lt;AppInfo&gt; apiAppGet(apptype, appplatform, azureenvironment, signinaudience, isadminconsented, publicclient)

Gets App Info based on Azure Environment or Sign-in Audience

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.AppApi;


AppApi apiInstance = new AppApi();
String apptype = "cloud"; // String | Allowed Values :  \"cloud\", \"onprem\"
String appplatform = "web"; // String | Allowed Values :  \"web\", \"spa\"
String azureenvironment = "azurecloud"; // String | Allowed Values :  \"azureb2ccloud\", \"azurechinacloud\", \"azurecloud\", \"azuregermanycloud\", \"azureppe\", \"azureusgovernment\"
String signinaudience = "azureadmultipleorgs"; // String | Allowed Values :  \"azureadmyorg\", \"azureadmultipleorgs\", \"azureadandpersonalmicrosoftaccount\"
String isadminconsented = "yes"; // String | Allowed Values :  \"yes\", \"no\"
String publicclient = "yes"; // String | Allowed Values :  \"yes\", \"no\"
try {
    List<AppInfo> result = apiInstance.apiAppGet(apptype, appplatform, azureenvironment, signinaudience, isadminconsented, publicclient);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AppApi#apiAppGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apptype** | **String**| Allowed Values :  \&quot;cloud\&quot;, \&quot;onprem\&quot; | [optional] [default to cloud]
 **appplatform** | **String**| Allowed Values :  \&quot;web\&quot;, \&quot;spa\&quot; | [optional] [default to web]
 **azureenvironment** | **String**| Allowed Values :  \&quot;azureb2ccloud\&quot;, \&quot;azurechinacloud\&quot;, \&quot;azurecloud\&quot;, \&quot;azuregermanycloud\&quot;, \&quot;azureppe\&quot;, \&quot;azureusgovernment\&quot; | [optional] [default to azurecloud]
 **signinaudience** | **String**| Allowed Values :  \&quot;azureadmyorg\&quot;, \&quot;azureadmultipleorgs\&quot;, \&quot;azureadandpersonalmicrosoftaccount\&quot; | [optional] [default to azureadmultipleorgs]
 **isadminconsented** | **String**| Allowed Values :  \&quot;yes\&quot;, \&quot;no\&quot; | [optional] [default to yes]
 **publicclient** | **String**| Allowed Values :  \&quot;yes\&quot;, \&quot;no\&quot; | [optional] [default to yes]

### Return type

[**List&lt;AppInfo&gt;**](AppInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

