package com.example.data

import com.example.api.RetrofitClient
import com.example.shortdrama.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppUpdateRepository {
    suspend fun getAppConfig(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.supabaseApi.getAppConfig(
                apiKey = BuildConfig.SUPABASE_KEY,
                authorization = "Bearer ${BuildConfig.SUPABASE_KEY}"
            )
            response.associate { it.key to it.value }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
}
