package com.sagar.android.watchapp.ui.mainactivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.wearable.intent.RemoteIntent
import com.sagar.android.logutilmaster.LogUtil
import com.sagar.android.watchapp.core.KeyWordsAndConstants.APP_IN_PLAY_STORE
import com.sagar.android.watchapp.core.KeyWordsAndConstants.CAPABILITY_WEAR_APP
import com.sagar.android.watchapp.databinding.ActivityMainBinding
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class MainActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by kodein()

    private val logUtil: LogUtil by instance()
    private lateinit var binding: ActivityMainBinding
    private lateinit var capabilityChangedListener: CapabilityClient.OnCapabilityChangedListener
    private lateinit var nodesWithAppInstalled: Set<Node>
    private lateinit var nodesInContactMayNotHaveAppInstalled: List<Node>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prepareCapabilityChangeListener()

        binding.button.setOnClickListener {
            openPlayStoreWithAppInDevicesWithoutAppInstalled()
        }
    }

    private fun setStatusToScreen(status: String) {
        binding.textViewMessage.text = status
    }

    private fun prepareCapabilityChangeListener() {
        capabilityChangedListener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            logUtil.logV("capability changed....")

            logUtil.logV("number of devices with app installed ${capabilityInfo.nodes.size}")
            setStatusToScreen("found ${capabilityInfo.nodes.size} device(s) with app installed.")
            binding.button.visibility = View.GONE

            nodesWithAppInstalled = capabilityInfo.nodes
            capabilityInfo.nodes.forEach { node ->
                logUtil.logV("node >> Name- ${node.displayName}, Id- ${node.id}, IsNearBy- ${node.isNearby}")
            }

            findAllWearDevices()

            verifyNodes()
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

        setStatusToScreen("initiating... ")
        findAllWearDevicesWithAppInstalled()
        findAllWearDevices()
    }

    private fun findAllWearDevicesWithAppInstalled() {
        logUtil.logV("Finding all devices with app installed")

        Wearable.getCapabilityClient(this)
                .getCapability(
                        CAPABILITY_WEAR_APP,
                        CapabilityClient.FILTER_REACHABLE
                )
                .addOnCompleteListener { task ->
                    if (
                            task.isSuccessful
                    ) {
                        logUtil.logV("found list of devices with app installed")

                        task.result?.let {
                            val capabilityInfo: CapabilityInfo = it
                            if (capabilityInfo.nodes.isEmpty()) {
                                logUtil.logV("there is no device with app installed")
                            }

                            nodesWithAppInstalled = capabilityInfo.nodes
                            capabilityInfo.nodes.forEach { node ->
                                logUtil.logV("node >> Name- ${node.displayName}, Id- ${node.id}, IsNearBy- ${node.isNearby}")
                            }

                            verifyNodes()
                        } ?: kotlin.run {
                            logUtil.logV("there is no device with app installed")
                        }
                    } else {
                        logUtil.logV("failed to get device list with app installed")
                    }
                }
    }

    private fun findAllWearDevices() {
        logUtil.logV("Finding all wear devices")

        Wearable.getNodeClient(this).connectedNodes
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logUtil.logV("Finding wear devices successful")
                        task.result?.let { nodeList ->
                            logUtil.logV("Number of devices found ${nodeList.size}")
                            nodesInContactMayNotHaveAppInstalled = nodeList
                            nodeList.forEach { node ->
                                logUtil.logV("node >> Name- ${node.displayName}, Id- ${node.id}, IsNearBy- ${node.isNearby}")
                            }
                            verifyNodes()
                        }
                    } else {
                        logUtil.logV("Finding wear devices failed")
                    }
                }
    }

    private fun verifyNodes() {
        logUtil.logV("verifying the nodes")
        if (
                !this::nodesWithAppInstalled.isInitialized || !this::nodesInContactMayNotHaveAppInstalled.isInitialized
        ) {
            logUtil.logV("data not yet ready, will wait for that ...")
            return
        }

        if (
                nodesInContactMayNotHaveAppInstalled.isEmpty()
        ) {
            logUtil.logV("there are no devices to connect.")
            setStatusToScreen("there are no devices to connect.")
            binding.button.visibility = View.GONE
            return
        }

        if (
                nodesWithAppInstalled.isEmpty()
        ) {
            logUtil.logV("no device has the app installed.")
            setStatusToScreen("no device has the app installed.")
            return
        }

        if (
                nodesWithAppInstalled.size < nodesInContactMayNotHaveAppInstalled.size
        ) {
            logUtil.logV("there are few devices with app and few have not installed the app")
            setStatusToScreen("there are few devices with app and few have not installed the app")
            return
        }

        if (
                nodesWithAppInstalled.size == nodesInContactMayNotHaveAppInstalled.size
        ) {
            logUtil.logV("all device have the app installed. good to go...")
            setStatusToScreen("all device have the app installed. good to go...")
            binding.button.visibility = View.GONE
        }
    }

    private val resultReceiver: ResultReceiver = object : ResultReceiver(Handler()) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            if (resultCode == RemoteIntent.RESULT_OK) {
                val toast = Toast.makeText(
                        applicationContext,
                        "Play Store Request to Wear device successful.",
                        Toast.LENGTH_SHORT
                )
                toast.show()
            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                val toast = Toast.makeText(
                        applicationContext,
                        "Play Store Request Failed. Wear device(s) may not support Play Store, "
                                + " that is, the Wear device may be version 1.0.",
                        Toast.LENGTH_LONG
                )
                toast.show()
            } else {
                throw IllegalStateException("Unexpected result $resultCode")
            }
        }
    }

    private fun openPlayStoreWithAppInDevicesWithoutAppInstalled() {
        logUtil.logV("going to open play store in the devices without app installed")

        val devicesWithoutApp: ArrayList<Node> = ArrayList()
        nodesInContactMayNotHaveAppInstalled.forEach { nodeMayHaveAppInstalled ->
            if (!nodesWithAppInstalled.contains(nodeMayHaveAppInstalled))
                devicesWithoutApp.add(nodeMayHaveAppInstalled)
        }
        logUtil.logV("number of device that does not have the app installed ${devicesWithoutApp.size}")

        if (devicesWithoutApp.size == 0)
            return

        val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(APP_IN_PLAY_STORE))

        devicesWithoutApp.forEach { deviceWithoutApp ->
            RemoteIntent.startRemoteActivity(
                    applicationContext,
                    intent,
                    resultReceiver,
                    deviceWithoutApp.id
            )
        }
    }
}