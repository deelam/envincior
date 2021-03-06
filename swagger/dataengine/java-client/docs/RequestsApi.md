# RequestsApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getRequest**](RequestsApi.md#getRequest) | **GET** /request/{id} | retrieve request
[**listOperations**](RequestsApi.md#listOperations) | **GET** /operations | retrieve list of operations for requests
[**refreshOperations**](RequestsApi.md#refreshOperations) | **GET** /operations/refresh | refresh list of operations
[**submitRequest**](RequestsApi.md#submitRequest) | **POST** /request | submit a request


<a name="getRequest"></a>
# **getRequest**
> Request getRequest(id)

retrieve request

get request based on id parameter

### Example
```java
// Import classes:
//import dataengine.ApiException;
//import dataengine.api.RequestsApi;


RequestsApi apiInstance = new RequestsApi();
String id = "id_example"; // String | request ID
try {
    Request result = apiInstance.getRequest(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RequestsApi#getRequest");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **String**| request ID |

### Return type

[**Request**](Request.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="listOperations"></a>
# **listOperations**
> List&lt;Operation&gt; listOperations()

retrieve list of operations for requests

list operations

### Example
```java
// Import classes:
//import dataengine.ApiException;
//import dataengine.api.RequestsApi;


RequestsApi apiInstance = new RequestsApi();
try {
    List<Operation> result = apiInstance.listOperations();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RequestsApi#listOperations");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;Operation&gt;**](Operation.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="refreshOperations"></a>
# **refreshOperations**
> Boolean refreshOperations()

refresh list of operations

refresh operations list from active workers

### Example
```java
// Import classes:
//import dataengine.ApiException;
//import dataengine.api.RequestsApi;


RequestsApi apiInstance = new RequestsApi();
try {
    Boolean result = apiInstance.refreshOperations();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RequestsApi#refreshOperations");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**Boolean**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="submitRequest"></a>
# **submitRequest**
> Request submitRequest(request)

submit a request

Submit a new request with given metadata

### Example
```java
// Import classes:
//import dataengine.ApiException;
//import dataengine.api.RequestsApi;


RequestsApi apiInstance = new RequestsApi();
Request request = new Request(); // Request | Request to add
try {
    Request result = apiInstance.submitRequest(request);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RequestsApi#submitRequest");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **request** | [**Request**](Request.md)| Request to add | [optional]

### Return type

[**Request**](Request.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

