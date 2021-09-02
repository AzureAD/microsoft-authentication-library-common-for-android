# ConfigApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiConfigGet**](ConfigApi.md#apiConfigGet) | **GET** /api/Config | Gets the Lab Configurartion (User/App/Lab) Based on Query Parameters with predefined defaults.   You can override the defaults.
[**apiConfigUpnGet**](ConfigApi.md#apiConfigUpnGet) | **GET** /api/Config/{upn} | Gets a single Lab User Config with Lab and App Info based on UPN

<a name="apiConfigGet"></a>
# **apiConfigGet**
> List&lt;ConfigInfo&gt; apiConfigGet(usertype, userrole, mfa, protectionpolicy, homedomain, homeupn, b2cprovider, federationprovider, azureenvironment, guesthomeazureenvironment, apptype, appplatform, publicclient, signinaudience, guesthomedin, hasaltid, altidsource, altidtype, passwordpolicyvalidityperiod, passwordpolicynotificationdays, tokenlifetimepolicy, tokentype, tokenlifetime, isadminconsented, optionalclaim)

Gets the Lab Configurartion (User/App/Lab) Based on Query Parameters with predefined defaults.   You can override the defaults.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ConfigApi;


ConfigApi apiInstance = new ConfigApi();
String usertype = "cloud"; // String | Allowed Values :  \"cloud\", \"federated\", \"onprem\", \"guest\", \"msa\", \"b2c\"
String userrole = "none"; // String | Allowed Values :  \"none\", \"clouddeviceadministrator\"
String mfa = "none"; // String | Allowed Values :  \"none\", \"mfaonall\", \"automfaonall\"
String protectionpolicy = "none"; // String | Allowed Values :  \"none\", \"ca\", \"cadj\", \"mam\", \"mdm\", \"mdmca\", \"mamca\", \"truemamca\",\"mamspo\", \"blocked\"
String homedomain = "none"; // String | Allowed Values :  \"none\", \"msidlab2.com\", \"msidlab3.com\", \"msidlab4.com\"
String homeupn = "none"; // String | Allowed Values :  \"none\", \"gidlab@msidlab2.com\", \"gidlab@msidlab3.com\", \"gidlab@msidlab4.com\"
String b2cprovider = "none"; // String | Allowed Values :  \"none\", \"amazon\", \"facebook\", \"google\", \"local\", \"microsoft\", \"twitter\"
String federationprovider = "adfsv4"; // String | Allowed Values :  \"none\", \"adfsv2\", \"adfsv3\", \"adfsv4\", \"adfsv2019\", \"b2c\", \"ping\", \"shibboleth\"
String azureenvironment = "azurecloud"; // String | Allowed Values :  \"azureb2ccloud\", \"azurechinacloud\", \"azurecloud\", \"azuregermanycloud\", \"azureppe\", \"azureusgovernment\"
String guesthomeazureenvironment = "none"; // String | Allowed Values :  \"none\", \"azurechinacloud\", \"azurecloud\", \"azureusgovernment\"
String apptype = "cloud"; // String | Allowed Values :  \"cloud\", \"onprem\"
String appplatform = "web"; // String | Allowed Values :  \"web\", \"spa\"
String publicclient = "yes"; // String | Allowed Values :  \"yes\", \"no\"
String signinaudience = "azureadmultipleorgs"; // String | Allowed Values :  \"azureadmyorg\", \"azureadmultipleorgs\", \"azureadandpersonalmicrosoftaccount\"
String guesthomedin = "none"; // String | Allowed Values :  \"none\", \"onprem\", \"hostazuread\"
String hasaltid = "no"; // String | Allowed Values :  \"no\", \"yes\"
String altidsource = "none"; // String | Allowed Values :  \"none\", \"onprem\"
String altidtype = "none"; // String | Allowed Values :  \"none\", \"upn\"
String passwordpolicyvalidityperiod = "60"; // String | Allowed Values :  \"60\"
String passwordpolicynotificationdays = "14"; // String | Allowed Values :  \"14\", \"61\"
String tokenlifetimepolicy = "OrganizationDefault"; // String | Allowed Values :  \"OrganizationDefault\", \"CAE\", \"CTL\"
String tokentype = "Access"; // String | Allowed Values :  \"Access\"
String tokenlifetime = "default"; // String | Allowed Values :  \"default\", \"short\", \"long\"
String isadminconsented = "yes"; // String | Allowed Values :  \"yes\", \"no\"
String optionalclaim = "none"; // String | Allowed Values :  \"none\", \"refresh_in\", \"pwd_exp\"
try {
    List<ConfigInfo> result = apiInstance.apiConfigGet(usertype, userrole, mfa, protectionpolicy, homedomain, homeupn, b2cprovider, federationprovider, azureenvironment, guesthomeazureenvironment, apptype, appplatform, publicclient, signinaudience, guesthomedin, hasaltid, altidsource, altidtype, passwordpolicyvalidityperiod, passwordpolicynotificationdays, tokenlifetimepolicy, tokentype, tokenlifetime, isadminconsented, optionalclaim);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ConfigApi#apiConfigGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usertype** | **String**| Allowed Values :  \&quot;cloud\&quot;, \&quot;federated\&quot;, \&quot;onprem\&quot;, \&quot;guest\&quot;, \&quot;msa\&quot;, \&quot;b2c\&quot; | [optional] [default to cloud]
 **userrole** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;clouddeviceadministrator\&quot; | [optional] [default to none]
 **mfa** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;mfaonall\&quot;, \&quot;automfaonall\&quot; | [optional] [default to none]
 **protectionpolicy** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;ca\&quot;, \&quot;cadj\&quot;, \&quot;mam\&quot;, \&quot;mdm\&quot;, \&quot;mdmca\&quot;, \&quot;mamca\&quot;, \&quot;truemamca\&quot;,\&quot;mamspo\&quot;, \&quot;blocked\&quot; | [optional] [default to none]
 **homedomain** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;msidlab2.com\&quot;, \&quot;msidlab3.com\&quot;, \&quot;msidlab4.com\&quot; | [optional] [default to none]
 **homeupn** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;gidlab@msidlab2.com\&quot;, \&quot;gidlab@msidlab3.com\&quot;, \&quot;gidlab@msidlab4.com\&quot; | [optional] [default to none]
 **b2cprovider** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;amazon\&quot;, \&quot;facebook\&quot;, \&quot;google\&quot;, \&quot;local\&quot;, \&quot;microsoft\&quot;, \&quot;twitter\&quot; | [optional] [default to none]
 **federationprovider** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;adfsv2\&quot;, \&quot;adfsv3\&quot;, \&quot;adfsv4\&quot;, \&quot;adfsv2019\&quot;, \&quot;b2c\&quot;, \&quot;ping\&quot;, \&quot;shibboleth\&quot; | [optional] [default to adfsv4]
 **azureenvironment** | **String**| Allowed Values :  \&quot;azureb2ccloud\&quot;, \&quot;azurechinacloud\&quot;, \&quot;azurecloud\&quot;, \&quot;azuregermanycloud\&quot;, \&quot;azureppe\&quot;, \&quot;azureusgovernment\&quot; | [optional] [default to azurecloud]
 **guesthomeazureenvironment** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;azurechinacloud\&quot;, \&quot;azurecloud\&quot;, \&quot;azureusgovernment\&quot; | [optional] [default to none]
 **apptype** | **String**| Allowed Values :  \&quot;cloud\&quot;, \&quot;onprem\&quot; | [optional] [default to cloud]
 **appplatform** | **String**| Allowed Values :  \&quot;web\&quot;, \&quot;spa\&quot; | [optional] [default to web]
 **publicclient** | **String**| Allowed Values :  \&quot;yes\&quot;, \&quot;no\&quot; | [optional] [default to yes]
 **signinaudience** | **String**| Allowed Values :  \&quot;azureadmyorg\&quot;, \&quot;azureadmultipleorgs\&quot;, \&quot;azureadandpersonalmicrosoftaccount\&quot; | [optional] [default to azureadmultipleorgs]
 **guesthomedin** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;onprem\&quot;, \&quot;hostazuread\&quot; | [optional] [default to none]
 **hasaltid** | **String**| Allowed Values :  \&quot;no\&quot;, \&quot;yes\&quot; | [optional] [default to no]
 **altidsource** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;onprem\&quot; | [optional] [default to none]
 **altidtype** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;upn\&quot; | [optional] [default to none]
 **passwordpolicyvalidityperiod** | **String**| Allowed Values :  \&quot;60\&quot; | [optional] [default to 60]
 **passwordpolicynotificationdays** | **String**| Allowed Values :  \&quot;14\&quot;, \&quot;61\&quot; | [optional] [default to 14]
 **tokenlifetimepolicy** | **String**| Allowed Values :  \&quot;OrganizationDefault\&quot;, \&quot;CAE\&quot;, \&quot;CTL\&quot; | [optional] [default to OrganizationDefault]
 **tokentype** | **String**| Allowed Values :  \&quot;Access\&quot; | [optional] [default to Access]
 **tokenlifetime** | **String**| Allowed Values :  \&quot;default\&quot;, \&quot;short\&quot;, \&quot;long\&quot; | [optional] [default to default]
 **isadminconsented** | **String**| Allowed Values :  \&quot;yes\&quot;, \&quot;no\&quot; | [optional] [default to yes]
 **optionalclaim** | **String**| Allowed Values :  \&quot;none\&quot;, \&quot;refresh_in\&quot;, \&quot;pwd_exp\&quot; | [optional] [default to none]

### Return type

[**List&lt;ConfigInfo&gt;**](ConfigInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="apiConfigUpnGet"></a>
# **apiConfigUpnGet**
> List&lt;ConfigInfo&gt; apiConfigUpnGet(upn)

Gets a single Lab User Config with Lab and App Info based on UPN

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ConfigApi;


ConfigApi apiInstance = new ConfigApi();
String upn = "upn_example"; // String | Enter the UPN of the Lab User. You cannot Query Locked Users at this endpoint.
try {
    List<ConfigInfo> result = apiInstance.apiConfigUpnGet(upn);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ConfigApi#apiConfigUpnGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **upn** | **String**| Enter the UPN of the Lab User. You cannot Query Locked Users at this endpoint. |

### Return type

[**List&lt;ConfigInfo&gt;**](ConfigInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

