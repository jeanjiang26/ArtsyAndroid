package com.jeannie.artsyfinal.network

import android.content.Context
import com.franmontiel.persistentcookiejar.ClearableCookieJar
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrl

object RetrofitClient {
    private const val TAG = "RetrofitClient"

    private var cookieJar: ClearableCookieJar? = null

    // Add a detailed header logging interceptor
    private val headerLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()

        // Log the complete URL
        val url = request.url.toString()
        Log.d(TAG, "🔍 REQUEST URL: $url")

        // Log all request headers
        Log.d(TAG, "🔍 REQUEST HEADERS:")
        val headers = request.headers
        if (headers.size > 0) {
            for (i in 0 until headers.size) {
                val name = headers.name(i)
                val value = if (name.equals("Cookie", ignoreCase = true) ||
                    name.equals("Authorization", ignoreCase = true)) {
                    "[FILTERED FOR SECURITY]" // Don't log full auth values
                } else {
                    headers.value(i)
                }
                Log.d(TAG, "  $name: $value")
            }

            // Specifically log if authentication related headers exist
            if (request.header("Cookie") != null) {
                Log.d(TAG, "  ✅ Cookie header is present")

                // Log session cookie specifically (safer to check existence)
                val cookieHeader = request.header("Cookie") ?: ""
                if (cookieHeader.contains("session_")) {
                    Log.d(TAG, "  ✅ Session cookie is present")
                } else {
                    Log.d(TAG, "  ❌ No session cookie found in Cookie header")
                }
            } else {
                Log.d(TAG, "  ❌ No Cookie header found")
            }

            if (request.header("Authorization") != null) {
                Log.d(TAG, "  ✅ Authorization header is present")
            } else {
                Log.d(TAG, "  ℹ️ No Authorization header found (may be using cookies instead)")
            }
        } else {
            Log.d(TAG, "  ⚠️ No headers in request!")
        }

        // Proceed with the request
        val response = chain.proceed(request)

        // Log response code and authentication-related headers
        Log.d(TAG, "🔍 RESPONSE CODE: ${response.code}")
        Log.d(TAG, "🔍 RESPONSE HEADERS:")

        // Check for Set-Cookie or auth-related headers
        val responseCookies = response.headers("Set-Cookie")
        if (responseCookies.isNotEmpty()) {
            Log.d(TAG, "  ✅ Server sent ${responseCookies.size} cookies")
        } else {
            Log.d(TAG, "  ℹ️ No cookies set by server")
        }

        response
    }

    private val bodyLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ✅ Expose this as a public val instead of using a conflicting function
    val statelessClient: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(headerLoggingInterceptor) // Add our detailed header logging
                    .addInterceptor(bodyLoggingInterceptor)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Method to clear cookies from the persistent cookie jar
    fun clearCookies(context: Context) {
        Log.d(TAG, "Clearing all cookies")

        // Clear the current cookieJar if it exists
        cookieJar?.clear()

        // Also clear from the SharedPrefs as a backup
        val sharedPrefsCookiePersistor = SharedPrefsCookiePersistor(context)
        sharedPrefsCookiePersistor.clear()

        Log.d(TAG, "All cookies cleared")
    }

    // Inside RetrofitClient object
    fun getClientWithCookies(context: Context): Retrofit {
        // Create a new cookie jar if we don't have one
        if (cookieJar == null) {
            cookieJar = PersistentCookieJar(
                SetCookieCache(),
                SharedPrefsCookiePersistor(context)
            )
            Log.d(TAG, "Created new PersistentCookieJar")
        } else {
            Log.d(TAG, "Using existing PersistentCookieJar")
        }

        // Add this block to manually check cookie handling
        val sharedPrefsCookiePersistor = SharedPrefsCookiePersistor(context)
        val storedCookies = sharedPrefsCookiePersistor.loadAll()
        Log.d(TAG, "Manual check: Found ${storedCookies.size} cookies in storage")

        // Ensure cookies are loaded into the cookie jar's cache
        if (cookieJar is PersistentCookieJar) {
            val persistentJar = cookieJar as PersistentCookieJar
            // Force reload cookies from storage to cache
            for (cookie in storedCookies) {
                Log.d(TAG, "Manually loading cookie: ${cookie.name} for ${cookie.domain}")
                try {
                    // Use the modern way to create an HttpUrl
                    val url = "https://${cookie.domain}".toHttpUrl()
                    persistentJar.saveFromResponse(url, listOf(cookie))
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading cookie into jar: ${e.message}")
                }
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(headerLoggingInterceptor)
            .addInterceptor(bodyLoggingInterceptor)
            .cookieJar(cookieJar!!)
            // Add this interceptor to manually add cookies if needed
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                // If no Cookie header in request, check for cookies
                // Always load fresh cookies directly from storage
                val freshCookiePersistor = SharedPrefsCookiePersistor(context)
                val currentCookies = freshCookiePersistor.loadAll()

                if (originalRequest.header("Cookie") == null && currentCookies.isNotEmpty()) {
                    Log.d(TAG, "⚠️ Request missing Cookie header despite having cookies in storage!")

                    // Build cookie string manually with safer domain matching
                    val requestHost = originalRequest.url.host
                    val cookieHeader = currentCookies
                        .filter { cookie ->
                            // More flexible domain matching
                            requestHost == cookie.domain ||
                                    requestHost.endsWith(".${cookie.domain}")
                        }
                        .joinToString("; ") { "${it.name}=${it.value}" }

                    if (cookieHeader.isNotEmpty()) {
                        Log.d(TAG, "🔧 Manually adding Cookie header: $cookieHeader")
                        val newRequest = originalRequest.newBuilder()
                            .addHeader("Cookie", cookieHeader)
                            .build()
                        return@addInterceptor chain.proceed(newRequest)
                    }
                }

                chain.proceed(originalRequest)
            }
            .addInterceptor { chain ->
                val original = chain.request()

                // Add a debug header with timestamp to prevent caching
                val request = original.newBuilder()
                    .header("X-Debug-Timestamp", System.currentTimeMillis().toString())
                    .build()

                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Create a fresh client by clearing cookies first
    fun createFreshClient(context: Context): Retrofit {
        // Clear existing cookies
        clearCookies(context)

        // Force recreation of cookieJar
        cookieJar = PersistentCookieJar(
            SetCookieCache(),
            SharedPrefsCookiePersistor(context)
        )

        Log.d(TAG, "Created fresh client with new cookie jar")

        return getClientWithCookies(context)
    }

    fun dumpCookies(context: Context) {
        val sharedPrefsCookiePersistor = SharedPrefsCookiePersistor(context)
        val cookies = sharedPrefsCookiePersistor.loadAll()

        Log.d(TAG, "📝 COOKIE DUMP: Found ${cookies.size} cookies in persistent storage")
        cookies.forEachIndexed { index, cookie ->
            Log.d(TAG, "📝 Cookie #${index + 1}:")
            Log.d(TAG, "  Name: ${cookie.name}")
            Log.d(TAG, "  Domain: ${cookie.domain}")
            Log.d(TAG, "  Path: ${cookie.path}")
            Log.d(TAG, "  Expiration: ${cookie.expiresAt}")
            Log.d(TAG, "  Secure: ${cookie.secure}")
            Log.d(TAG, "  HttpOnly: ${cookie.httpOnly}")
            Log.d(TAG, "  Persistent: ${cookie.persistent}")
        }
    }
}