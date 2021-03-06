package com.example.androidDeviceDetails.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.androidDeviceDetails.DeviceDetailsApplication
import com.example.androidDeviceDetails.R
import com.example.androidDeviceDetails.collectors.AppEventCollectionHelper
import com.example.androidDeviceDetails.database.RoomDB
import com.example.androidDeviceDetails.databinding.AppTypeMoreInfoBinding
import com.example.androidDeviceDetails.models.Chart
import com.example.androidDeviceDetails.models.appInfo.AppDetails
import com.example.androidDeviceDetails.models.appInfo.AppInfoCookedData
import com.example.androidDeviceDetails.models.appInfo.AppTypeModel
import com.example.androidDeviceDetails.models.appInfo.EventType
import com.example.androidDeviceDetails.models.signal.Signal
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

object Utils {
    private const val format = "HH:mm dd MMM yyyy"
    private val f = SimpleDateFormat(format, Locale.ENGLISH)
    const val COLLECTION_INTERVAL: Long = 5 //in Minutes

    fun getDateTime(millis: Long): String = f.format(Date(millis))

    fun getApplicationLabel(packageName: String): String {
        val packageManager = DeviceDetailsApplication.instance.packageManager
        return try {
            val info = packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
            packageManager.getApplicationLabel(info) as String
        } catch (e: Exception) {
            packageName
        }
    }

    fun getFileSize(size: Long): String {
        if (size <= 0) return "0 KB"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
    }

    fun getApplicationIcon(packageName: String): Drawable {
        return try {
            DeviceDetailsApplication.instance.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            ContextCompat.getDrawable(
                DeviceDetailsApplication.instance,
                R.drawable.ic_baseline_android_24
            )!!
        }
    }

    fun getAppDetails(context: Context, packageName: String): AppDetails {
        val appDetails = AppDetails(-1, "Null", -1, "Not Found", false)
        try {
            val pInfo2 = context.packageManager.getApplicationInfo(packageName, 0)
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                appDetails.versionCode = pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                appDetails.versionCode = pInfo.versionCode.toLong()
            }
            appDetails.versionName = pInfo.versionName ?: "Unknown version name"
            val file = File(pInfo2.sourceDir)
            appDetails.appSize = file.length()
            appDetails.appTitle = context.packageManager.getApplicationLabel(pInfo2).toString()
            val mask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            appDetails.isSystemApp = (pInfo2.flags and mask == 0).not()
            Log.d(
                "isSystem App",
                " ${appDetails.appTitle} is system App : ${appDetails.isSystemApp}"
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return appDetails
    }

    fun getPackageDetails(context: Context, appInfoCookedData: AppInfoCookedData): AppTypeModel {
        val simpleDateFormat = SimpleDateFormat("EEE, MMM d ''yy, hh:mm a", Locale.ENGLISH)
        val packageInfo = context.packageManager.getPackageInfo(appInfoCookedData.packageName, 0)
        return AppTypeModel(
            getApplicationIcon(appInfoCookedData.packageName),
            appInfoCookedData.appName,
            appInfoCookedData.packageName,
            appInfoCookedData.versionCode.toString(),
            packageInfo.versionName.toString(),
            getFileSize(appInfoCookedData.size),
            simpleDateFormat.format(Date(packageInfo.firstInstallTime)),
            simpleDateFormat.format(Date(packageInfo.lastUpdateTime))
        )
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun addInitialData(context: Context) {
        val appEventCollectionHelper = AppEventCollectionHelper()
        val db = RoomDB.getDatabase(context)!!
        val packages = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        GlobalScope.launch(Dispatchers.IO) {
            for (i in packages) {
                val details = getAppDetails(context, i.packageName)
                appEventCollectionHelper.writeToAppsDb(0, i.packageName, details, db)
                val id = db.appsDao().getIdByName(i.packageName)
                appEventCollectionHelper.writeToAppHistoryDb(
                    id,
                    EventType.ENROLL.ordinal,
                    details,
                    db
                )
            }
        }
    }

    fun loadPreviousDayTime(): Long {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR] = 0
        cal[Calendar.MINUTE] = 0
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return cal.timeInMillis
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Uninstalls the app
     *
     * @param [packageName] Package name of the App
     * @param [packageManager] Package manager
     * @param context Context
     */
    fun uninstallApp(packageName: String, packageManager: PackageManager, context: Context) {
        if (isPackageInstalled(packageName, packageManager)) {
            val packageURI = Uri.parse("package:${packageName}")
            val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI)
            try {
                context.startActivity(uninstallIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "App cannot be uninstalled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun isUsageAccessGranted(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.packageManager.getApplicationInfo(context.packageName, 0).uid,
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun showAlertDialog(context: Context, appTypeModel: AppTypeModel) {
        val binding: AppTypeMoreInfoBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.app_type_more_info,
            null,
            false
        )
        binding.Icon.setImageDrawable(appTypeModel.appIcon)
        binding.appTitle.text = appTypeModel.appTitle
        binding.packageName.text = appTypeModel.packageName
        binding.versionCode.text = appTypeModel.versionCode
        binding.versionName.text = appTypeModel.versionName
        binding.appSize.text = appTypeModel.packageSize
        binding.installTime.text = appTypeModel.installTime
        binding.updateTime.text = appTypeModel.updateTime
        AlertDialog.Builder(context)
            .setCancelable(false)
            .setView(binding.root)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun graphCalculator(dataSize: Double, total: Double) =
        ceil((dataSize).div(total).times(100)).toInt()

    fun drawChart(chart: Chart, xSet: Array<String>, ySet: Array<Any>) {
        val chartView = chart.chartView
        val chartModel = AAChartModel()
            .chartType(AAChartType.Spline)
            .title(chart.title)
            .categories(xSet)
            .yAxisLabelsEnabled(true)
            .yAxisGridLineWidth(0f)
            .markerRadius(chart.markerRadius)
            .xAxisLabelsEnabled(false)
            .touchEventEnabled(true)
            .backgroundColor(R.attr.mainBackground)
            .yAxisMin(chart.yAxisMin)
            .yAxisMax(chart.yAxisMax)
            .yAxisTitle("")
            .tooltipValueSuffix("dBm")
            .colorsTheme(arrayOf(chart.color))
            .legendEnabled(false)
            .zoomType(AAChartZoomType.XY)
            .series(arrayOf(AASeriesElement().name(chart.title).data(ySet)))
        chartView.aa_drawChartWithChartModel(chartModel)
    }

    fun getTimePeriodInWords(timeStamp: Long): String {
        var timeString = formatDurationWords(timeStamp, true, true)
        timeString = timeString.replace("hour", "hr")
        timeString = timeString.replace("minute", "min")
        timeString = timeString.replace("second", "sec")
        return timeString
    }

    fun getSignalLevel(level: Int): String {
        return when (level) {
            Signal.LEVEL_POOR -> "Poor"
            Signal.LEVEL_LOW -> "Low"
            Signal.LEVEL_MEDIUM -> "Medium"
            Signal.LEVEL_GOOD -> "Good"
            Signal.LEVEL_EXCELLENT -> "Excellent"
            else -> "Unknown"
        }
    }
}