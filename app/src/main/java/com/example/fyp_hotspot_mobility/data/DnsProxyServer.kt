package com.example.fyp_hotspot_mobility.data

import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.nio.ByteBuffer

class DnsProxyServer(
    private val repository: DeviceRepository,
    private val port: Int = 5353 
) {
    private val TAG = "DnsProxyServer"
    private var serverJob: Job? = null
    private var serverSocket: DatagramSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        if (serverJob?.isActive == true) return
        
        serverJob = scope.launch {
            try {
                val socket = DatagramSocket(port)
                serverSocket = socket
                Log.d(TAG, "DNS Proxy started on port $port")
                
                val buffer = ByteArray(512)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        handleDnsQuery(socket, packet)
                    } catch (e: Exception) {
                        if (isActive) Log.e(TAG, "Error receiving packet", e)
                    }
                }
            } catch (e: Exception) {
                if (isActive) Log.e(TAG, "DNS Proxy error", e)
            } finally {
                serverSocket?.close()
                serverSocket = null
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverSocket?.close()
        serverJob = null
        serverSocket = null
        Log.d(TAG, "DNS Proxy stopped")
    }

    private suspend fun handleDnsQuery(socket: DatagramSocket, queryPacket: DatagramPacket) = withContext(Dispatchers.IO) {
        val clientIp = queryPacket.address.hostAddress ?: return@withContext
        val queryData = queryPacket.data.copyOfRange(0, queryPacket.length)

        val isBlocked = repository.isBlocked(clientIp)
        
        Log.d(TAG, "Received DNS query from $clientIp. Blocked=$isBlocked")

        if (isBlocked) {
            val response = buildErrorResponse(queryData, 3)
            val responsePacket = DatagramPacket(response, response.size, queryPacket.address, queryPacket.port)
            socket.send(responsePacket)
            Log.d(TAG, "Blocked DNS query for $clientIp (Sent NXDOMAIN)")
        } else {
            try {
                forwardQueryToUpstream(socket, queryPacket)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve for $clientIp", e)
            }
        }
    }

    private fun forwardQueryToUpstream(socket: DatagramSocket, queryPacket: DatagramPacket) {
        // Forward the exact packet to an upstream DNS (Google DNS)
        val upstreamAddr = InetAddress.getByName("8.8.8.8")
        val upstreamSocket = DatagramSocket()
        upstreamSocket.soTimeout = 2000
        
        try {
            val forwardPacket = DatagramPacket(queryPacket.data, queryPacket.length, upstreamAddr, 53)
            upstreamSocket.send(forwardPacket)
            
            val responseBuffer = ByteArray(512)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            upstreamSocket.receive(responsePacket)
            
            // Send back to original client
            val replyPacket = DatagramPacket(
                responsePacket.data, 
                responsePacket.length, 
                queryPacket.address, 
                queryPacket.port
            )
            socket.send(replyPacket)
            Log.d(TAG, "Forwarded resolution for ${queryPacket.address.hostAddress}")
        } catch (e: Exception) {
            Log.e(TAG, "Upstream DNS failure", e)
        } finally {
            upstreamSocket.close()
        }
    }

    private fun buildErrorResponse(queryData: ByteArray, rcode: Int): ByteArray {
        if (queryData.size < 12) return queryData
        
        val response = queryData.copyOf()
        val buffer = ByteBuffer.wrap(response)

        var flags = buffer.getShort(2).toInt()
        flags = flags or 0x8000
        flags = flags or 0x0080
        flags = (flags and 0xFFF0) or (rcode and 0x000F)
        
        buffer.putShort(2, flags.toShort())

        buffer.putShort(6, 0)
        buffer.putShort(8, 0)
        buffer.putShort(10, 0)
        
        return response
    }
}
