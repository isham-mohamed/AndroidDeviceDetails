package com.example.androidDeviceDetails.viewModel

import android.content.Context
import androidx.core.view.isVisible
import com.example.androidDeviceDetails.DeviceDetailsApplication
import com.example.androidDeviceDetails.R
import com.example.androidDeviceDetails.adapters.AppInfoListAdapter
import com.example.androidDeviceDetails.base.BaseViewModel
import com.example.androidDeviceDetails.collectors.AppInfoManager
import com.example.androidDeviceDetails.databinding.ActivityAppInfoBinding
import com.example.androidDeviceDetails.models.appInfoModels.AppInfoCookedData
import com.example.androidDeviceDetails.models.appInfoModels.EventType
import kotlin.math.ceil

/**
 * Implements [BaseViewModel]
 */
class AppInfoViewModel(private val binding: ActivityAppInfoBinding, val context: Context) :
    BaseViewModel() {
    companion object {
        var eventFilter = 0
    }

    /**
     * Displays provided data on UI as List view and a donut chart
     *
     * Overrides : [onData] in [BaseViewModel]
     * @param [outputList] list of cooked data
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> onDone(outputList: ArrayList<T>) {
        val appList = outputList as ArrayList<AppInfoCookedData>
        AppInfoManager.appList = appList
        if (appList.isEmpty()) {
            binding.root.post {
                binding.appInfoListView.adapter = null
                binding.statisticsContainer.isVisible = false
                binding.appInfoListView.isVisible = false
                binding.indeterminateBar.isVisible = false
            }
        } else {
            var filteredList = appList.toMutableList()
            if (eventFilter != EventType.ALL_EVENTS.ordinal) {
                filteredList.removeAll { it.eventType.ordinal != eventFilter }
            }
            filteredList = filteredList.sortedBy { it.appName }.toMutableList()
            filteredList.removeAll { it.packageName == DeviceDetailsApplication.instance.packageName }
            if (filteredList.isNotEmpty()) {
                binding.root.post {
                    binding.appInfoListView.adapter = null
                    binding.statisticsContainer.isVisible = true
                    binding.appInfoListView.isVisible = true
                    binding.appInfoListView.adapter =
                        AppInfoListAdapter(
                            context,
                            R.layout.appinfo_tile,
                            filteredList
                        )
                    AppInfoManager.justifyListViewHeightBasedOnChildren(
                        binding.appInfoListView,
                        filteredList.size
                    )
                }
            } else {
                binding.root.post {
                    binding.appInfoListView.isVisible = false
                }
            }

            val total = appList.size.toDouble()
            val enrolledAppCount =
                appList.groupingBy { it.eventType.ordinal == EventType.APP_ENROLL.ordinal }
                    .eachCount()
            val enrolled = ((enrolledAppCount[true] ?: 0).toDouble().div(total).times(100))

            val installedAppCount =
                appList.groupingBy { it.eventType.ordinal == EventType.APP_INSTALLED.ordinal }
                    .eachCount()
            val installed = ceil(((installedAppCount[true] ?: 0).toDouble().div(total).times(100)))

            val updateAppCount =
                appList.groupingBy { it.eventType.ordinal == EventType.APP_UPDATED.ordinal }
                    .eachCount()
            val updated = ceil(((updateAppCount[true] ?: 0).toDouble().div(total).times(100)))

            val uninstalledAppCount =
                appList.groupingBy { it.eventType.ordinal == EventType.APP_UNINSTALLED.ordinal }
                    .eachCount()
            val uninstalled =
                ceil(((uninstalledAppCount[true] ?: 0).toDouble().div(total).times(100)))

            binding.root.post {
                binding.updatedProgressBar.progress = (updated.toInt())
                binding.installedProgressBar.progress = (updated + installed).toInt()
                binding.enrollProgressbar.progress = (updated + installed + enrolled).toInt()
                binding.uninstalledProgressbar.progress =
                    (updated + installed + enrolled + uninstalled).toInt()
                binding.statisticsContainer.isVisible = true
                binding.statsMap.isVisible = true
                binding.enrollCount.text = (enrolledAppCount[true] ?: 0).toString()
                binding.installCount.text = (installedAppCount[true] ?: 0).toString()
                binding.updateCount.text = (updateAppCount[true] ?: 0).toString()
                binding.uninstallCount.text = (uninstalledAppCount[true] ?: 0).toString()
                binding.indeterminateBar.isVisible = false
            }
        }
    }

    /**
     * Filters [AppInfoManager.appList] based on given filter type
     *
     * Overrides : [onData] in [BaseViewModel]
     * @param [type] Type of filter
     */
    override fun filter(type: Int) {
        eventFilter = type
        onDone(AppInfoManager.appList)
    }
}