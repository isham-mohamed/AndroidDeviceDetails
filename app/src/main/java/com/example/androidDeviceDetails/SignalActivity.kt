package com.example.androidDeviceDetails

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.androidDeviceDetails.controllers.Controller
import com.example.androidDeviceDetails.databinding.ActivitySignalStrengthBinding
import com.example.androidDeviceDetails.models.Signal
import com.example.androidDeviceDetails.models.TimeInterval

class SignalActivity : AppCompatActivity() {
    private lateinit var signalBinding: ActivitySignalStrengthBinding
    private lateinit var viewModel: SignalViewModel
    private var mainController: MainController = MainController()
    private lateinit var controller: Controller
    private var signal = Signal.CELLULAR.ordinal
    private lateinit var signalUtil: SignalUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        signalBinding = DataBindingUtil.setContentView(this, R.layout.activity_signal_strength)
        controller = Controller(signalBinding, this)
        viewModel = SignalViewModel(signalBinding, this)
        mainController.observeSignal(Signal.CELLULAR.ordinal, viewModel, lifecycleOwner = this)
        signalUtil = SignalUtil(signalBinding, this)
        signalBinding.list.isVisible = false

        signalBinding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.cellularStrength -> {
                    signal = Signal.CELLULAR.ordinal
                    viewModel.updateGauge(-50f, -150f, signal)
                    mainController.observeSignal(
                        Signal.CELLULAR.ordinal,
                        viewModel,
                        lifecycleOwner = this
                    )
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.wifiStrength -> {
                    signal = Signal.WIFI.ordinal
                    viewModel.updateGauge(0f, -100f, signal)
                    mainController.observeSignal(
                        Signal.WIFI.ordinal,
                        viewModel,
                        lifecycleOwner = this
                    )
                    return@setOnNavigationItemSelectedListener true
                }
            }
            return@setOnNavigationItemSelectedListener false
        }
        signalUtil.onCreate()
        signalBinding.filter.setOnClickListener()
        {
            signalUtil.onCreate()
            val startTime = signalUtil.getStartTimestamp()
            val endTime = signalUtil.getEndTimestamp()
            Log.d("neena", "onCreate: $startTime $endTime")
            controller.cook(signal,TimeInterval(startTime, endTime))
        }
    }
}