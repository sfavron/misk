package misk.web

import misk.web.actions.WebSocket
import misk.web.jetty.headers
import misk.web.jetty.httpUrl
import okhttp3.Headers
import okhttp3.HttpUrl
import okio.BufferedSink
import okio.BufferedSource
import javax.servlet.http.HttpServletRequest

internal data class ServletHttpCall(
  override val url: HttpUrl,
  override val dispatchMechanism: DispatchMechanism,
  override val requestHeaders: Headers,
  var requestBody: BufferedSource? = null,
  val upstreamResponse: UpstreamResponse,
  var responseBody: BufferedSink? = null,
  var webSocket: WebSocket? = null
) : HttpCall {

  override var statusCode: Int
    get() = upstreamResponse.statusCode
    set(value) {
      upstreamResponse.statusCode = value
    }

  override val responseHeaders: Headers
    get() = upstreamResponse.headers

  override fun setResponseHeader(name: String, value: String) {
    upstreamResponse.setHeader(name, value)
  }

  override fun addResponseHeaders(headers: Headers) {
    upstreamResponse.addHeaders(headers)
  }

  override fun requireTrailers() {
    upstreamResponse.requireTrailers()
  }

  override fun setResponseTrailer(name: String, value: String) {
    upstreamResponse.setTrailer(name, value)
  }

  override fun takeRequestBody(): BufferedSource? {
    val result = requestBody
    requestBody = null
    return result
  }

  override fun putRequestBody(requestBody: BufferedSource) {
    check(this.requestBody == null) { "previous request body leaked; take it first" }
    this.requestBody = requestBody
  }

  override fun takeResponseBody(): BufferedSink? {
    val result = responseBody
    responseBody = null
    return result
  }

  override fun putResponseBody(responseBody: BufferedSink) {
    check(this.responseBody == null) { "previous response body leaked; take it first" }
    this.responseBody = responseBody
  }

  override fun takeWebSocket(): WebSocket? {
    val result = webSocket
    webSocket = null
    return result
  }

  override fun putWebSocket(webSocket: WebSocket) {
    check(this.webSocket == null) { "previous web socket leaked; take it first" }
    this.webSocket = webSocket
  }

  /** Adapts the underlying servlet call to send data to the client. */
  interface UpstreamResponse {
    var statusCode: Int
    val headers: Headers
    fun setHeader(name: String, value: String)
    fun addHeaders(headers: Headers)
    fun requireTrailers()
    fun setTrailer(name: String, value: String)
  }

  companion object {
    fun create(
      request: HttpServletRequest,
      dispatchMechanism: DispatchMechanism,
      upstreamResponse: UpstreamResponse,
      webSocket: WebSocket? = null,
      requestBody: BufferedSource? = null,
      responseBody: BufferedSink? = null
    ): ServletHttpCall {
      if (dispatchMechanism == DispatchMechanism.WEBSOCKET) {
        check(webSocket != null)
      }

      return ServletHttpCall(
          url = request.httpUrl(),
          dispatchMechanism = dispatchMechanism,
          requestHeaders = request.headers(),
          upstreamResponse = upstreamResponse,
          requestBody = requestBody,
          responseBody = responseBody,
          webSocket = webSocket
      )
    }
  }
}