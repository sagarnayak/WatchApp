package com.sagar.android.watchapp.ui.mainactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.sagar.android.logutilmaster.LogUtil
import com.sagar.android.watchapp.databinding.ActivityMainBinding
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class MainActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by kodein()

    companion object {
        const val CAPABILITY_WEAR_APP = "verifyWatchAppInstallation"
    }

    private val logUtil: LogUtil by instance()
    private lateinit var binding: ActivityMainBinding
    private lateinit var capabilityChangedListener: CapabilityClient.OnCapabilityChangedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prepareCapabilityChangeListener()
    }

    private fun prepareCapabilityChangeListener() {
        capabilityChangedListener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
        }
    }

    override fun onPause() {
        super.onPause()

        Wearable.getCapabilityClient(this).removeListener(
                capabilityChangedListener,
            CAPABILITY_WEAR_APP
        )
    }

    override fun onResume() {
        super.onResume()

        Wearable.getCapabilityClient(this)
                .addListener(
                        capabilityChangedListener,
                    CAPABILITY_WEAR_APP
                )
    }
}