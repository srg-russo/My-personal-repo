package com.example.fyp_hotspot_mobility

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object LocalRegistrar {
    data class Client(
        val clientId: String,
        val ip: String,
        val hostname: String?,
        val lastSeen: Long
    )

    private val clients = ConcurrentHashMap<String, Client>()
    private val _clientsFlow = MutableStateFlow<List<Client>>(emptyList())
    val clientsFlow = _clientsFlow.asStateFlow()

    private var server: RegistrationServer? = null
    
    fun start(port: Int = 8080): Boolean {
        if (server != null) return true
            try {
                server = RegistrationServer(port)
            server?.start(500, false)
            return true
        } catch (e: Exception) {
            server = null
            return false
        }
    }

    fun stop() {
        server?.stop()
        server = null
        clients.clear()
        _clientsFlow.value = emptyList()
    }

    private fun updateClient(clientId: String, ip: String, hostname: String?) {
        val now = System.currentTimeMillis()
        val c = Client(clientId = clientId, ip = ip, hostname = hostname, lastSeen = now)
        clients[clientId] = c
        _clientsFlow.value = clients.values.sortedByDescending { it.lastSeen }
    }

    private class RegistrationServer(port: Int) : NanoHTTPD(port) {
        private val executor = Executors.newSingleThreadExecutor()

        override fun serve(session: IHTTPSession): Response {
            return try {
                when (session.method) {
                    Method.GET -> handleGet(session)
                    Method.POST -> handlePost(session)
                    else -> newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "")
                }
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error")
            }
        }

        private fun handleGet(session: IHTTPSession): Response {
            return when (session.uri) {
                "/" -> newFixedLengthResponse(Response.Status.OK, "text/html", HTML_PAGE)
                "/clients" -> {
                    val arr = org.json.JSONArray()
                    clients.values.forEach { c ->
                        val o = JSONObject()
                        o.put("clientId", c.clientId)
                        o.put("ip", c.ip)
                        o.put("hostname", c.hostname)
                        o.put("lastSeen", c.lastSeen)
                        arr.put(o)
                    }
                    newFixedLengthResponse(Response.Status.OK, "application/json", arr.toString())
                }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "")
            }
        }

        private fun handlePost(session: IHTTPSession): Response {
            return when (session.uri) {
                "/register" -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val body = map["postData"] ?: ""
                    val ip = session.remoteIpAddress ?: session.headers["remote-addr"] ?: ""
                    try {
                        val json = JSONObject(body)
                        val clientId = if (json.has("clientId")) json.getString("clientId") else ip
                        val hostname = if (json.has("hostname")) json.getString("hostname") else null
                        // update asynchronously to avoid blocking
                        CoroutineScope(Dispatchers.Default).launch {
                            updateClient(clientId, ip, hostname)
                        }
                    } catch (e: Exception) {
                        // try to handle form params fallback
                        val params = session.parms
                        val clientId = params["clientId"] ?: ip
                        val hostname = params["hostname"]
                        CoroutineScope(Dispatchers.Default).launch {
                            updateClient(clientId, ip, hostname)
                        }
                    }
                    newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}")
                }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "")
            }
        }

        override fun stop() {
            try {
                super.stop()
            } finally {
                executor.shutdownNow()
            }
        }

        companion object {
            // Simple registration page that posts to /register
            private const val HTML_PAGE = """
<!doctype html>
<html>
<head>
  <meta charset=\"utf-8\"> 
  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"> 
  <title>Hotspot Register</title>
</head>
<body>
  <h3>Register to hotspot</h3>
  <p>This page will register your device with the hotspot host so it appears in the host app.</p>
  <script>
    (function(){
      try {
        let id = localStorage.getItem('hotspot_client_id');
        if(!id){ id = Math.random().toString(36).slice(2); localStorage.setItem('hotspot_client_id', id); }
        const payload = { clientId: id, hostname: navigator.userAgent };
        fetch('/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
          .then(r => r.json()).then(()=>{
            document.body.appendChild(document.createTextNode('Registered.'));
          }).catch(()=>{
            document.body.appendChild(document.createTextNode('Failed to register.'));
          });
      } catch(e){ document.body.appendChild(document.createTextNode('Error')) }
    })();
  </script>
</body>
</html>
"""
        }
    }
}
