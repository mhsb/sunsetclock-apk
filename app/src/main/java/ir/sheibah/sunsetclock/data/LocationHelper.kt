package ir.sheibah.sunsetclock.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

/** گرفتن موقعیت تقریبی بدون وابستگی به Google Play Services. */
object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun requestFix(context: Context, onResult: (Location?) -> Unit) {
        if (!hasPermission(context)) {
            onResult(null)
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            onResult(null)
            return
        }
        val providers = try {
            lm.getProviders(true)
        } catch (e: Exception) {
            emptyList<String>()
        }

        var best: Location? = null
        for (p in providers) {
            val l = try {
                lm.getLastKnownLocation(p)
            } catch (e: Exception) {
                null
            } ?: continue
            val current = best
            if (current == null || l.time > current.time) best = l
        }
        if (best != null) {
            onResult(best)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && providers.isNotEmpty()) {
            val provider = if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else providers.first()
            try {
                lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context)) { loc ->
                    onResult(loc)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        } else {
            onResult(null)
        }
    }
}
