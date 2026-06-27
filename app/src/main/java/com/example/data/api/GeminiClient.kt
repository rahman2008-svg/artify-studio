package com.example.data.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Enhances a simple text description into a rich visual prompt.
     * Uses Gemini API if a key is available, or falls back to a creative offline local engine.
     */
    suspend fun optimizePrompt(userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext enhancePromptLocally(userPrompt)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemPrompt = "You are an expert prompt engineer for Microsoft Designer and DALL-E. " +
                "Enhance the following basic concept into a highly descriptive, visually stunning, artistic, " +
                "professional graphic design prompt. Specify rich colors, lighting, art style, and composition details. " +
                "Keep it concise, punchy, and under 75 words. Just return the enhanced prompt text directly without any quotes."

        try {
            val json = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            
            partObj.put("text", "$systemPrompt\n\nConcept: $userPrompt")
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            json.put("contents", contentsArray)

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext enhancePromptLocally(userPrompt) + " (Local enhancement - API returned code ${response.code})"
            }

            val body = response.body?.string() ?: return@withContext enhancePromptLocally(userPrompt)
            val responseJson = JSONObject(body)
            val text = responseJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            text.trim().replace("\"", "")
        } catch (e: Exception) {
            e.printStackTrace()
            enhancePromptLocally(userPrompt) + " (Offline Mode)"
        }
    }

    private fun enhancePromptLocally(prompt: String): String {
        val styles = listOf(
            "minimalist vector illustration, pastel dream colors, smooth gradients, clean geometric lines",
            "modern flat graphic design style, vibrant pop colors, bold typography, high contrast composition",
            "aesthetic 3D claymation style, colorful render, soft shadows, cute and playful design",
            "isometric digital art, neon cyberpunk highlights, dark atmospheric background, high fidelity",
            "elegant watercolor and gold foil accents, sophisticated serif accents, soft organic elements"
        )
        val selectedStyle = styles[prompt.hashCode().coerceAtLeast(0) % styles.size]
        return "A high-quality visual of '$prompt', rendered in a $selectedStyle, optimized for digital posters and social media layout, 8k resolution, modern designer style."
    }
}
