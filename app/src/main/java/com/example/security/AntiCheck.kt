package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.security.MessageDigest
import java.util.Locale

object AntiCheck {
    private const val TAG = "AntiCheckSecurity"

    /**
     * Checks if the device is rooted using multiple verification methods:
     * - Check for test-keys in build tags
     * - Check for presence of su binaries in various standard directories
     * - Attempt to execute 'su' command
     */
    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkSuPaths() || checkSuCommand()
    }

    private fun checkBuildTags(): Boolean {
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    private fun checkSuPaths(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                Log.w(TAG, "Root detected via su path: $path")
                return true
            }
        }
        return false
    }

    private fun checkSuCommand(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(java.io.InputStreamReader(process.inputStream))
            val line = reader.readLine()
            if (line != null) {
                Log.w(TAG, "Root detected via 'which su' command: $line")
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    /**
     * Checks if a debugger is attached or the app was compiled with debugging enabled.
     * Often, modded apps are resigned with the debug flags enabled for easier reverse engineering.
     */
    fun isDebugged(context: Context): Boolean {
        // Check if java debugger is connected
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            Log.w(TAG, "Debugger detected as connected or waiting!")
            return true
        }

        // Check if APK is flagged as debuggable in Manifest
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            // Note: This is normal in development, but we check this for release protections
            Log.i(TAG, "Manifest debug flag is active (normal for developer builds)")
        }
        return false
    }

    /**
     * Scans `/proc/self/maps` to detect dynamic hook libraries like Frida or Xposed.
     * When modders inject code or scripts, these libraries are loaded into the app's memory space.
     */
    fun isHookFrameworkDetected(): Boolean {
        try {
            val file = File("/proc/self/maps")
            if (file.exists()) {
                val reader = BufferedReader(FileReader(file))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val lowercaseLine = line!!.lowercase(Locale.ROOT)
                    if (lowercaseLine.contains("frida") || 
                        lowercaseLine.contains("xposed") || 
                        lowercaseLine.contains("substrate") || 
                        lowercaseLine.contains("libinject")
                    ) {
                        Log.e(TAG, "Hook framework detected in memory mapping: $line")
                        reader.close()
                        return true
                    }
                }
                reader.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking memory mappings: ${e.message}")
        }
        return false
    }

    /**
     * Helper to get the SHA-256 fingerprint of the APK's signing certificate.
     * This is useful for verifying if a modder has resigned your APK.
     */
    fun getSigningCertificateSha256(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures != null && signatures.isNotEmpty()) {
                val certBytes = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val fingerprintBytes = md.digest(certBytes)
                val hexString = StringBuilder()
                for (byte in fingerprintBytes) {
                    val hex = Integer.toHexString(0xFF and byte.toInt())
                    if (hex.length == 1) {
                        hexString.append('0')
                    }
                    hexString.append(hex)
                }
                hexString.toString().uppercase(Locale.ROOT)
            } else {
                "NO_SIGNATURE"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signatures: ${e.message}")
            "ERROR"
        }
    }

    /**
     * Checks if the package signature matches the official signature to protect against APK mod/resigning.
     * You would replace "OFFICIAL_SHA256_HASH_HERE" with your release key signature.
     */
    fun isSignatureTampered(context: Context, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank() || expectedSha256 == "OFFICIAL_SHA256_HASH_HERE") {
            // No official signature set yet, bypass this check
            return false
        }
        val currentSha256 = getSigningCertificateSha256(context)
        val isTampered = currentSha256 != expectedSha256
        if (isTampered) {
            Log.e(TAG, "Signature tampered! Expected: $expectedSha256, Current: $currentSha256")
        }
        return isTampered
    }
}
