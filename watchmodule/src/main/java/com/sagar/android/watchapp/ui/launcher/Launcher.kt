package com.sagar.android.watchapp.ui.launcher

import android.content.Context
import android.os.Bundle
import android.support.wearable.phone.PhoneDeviceType
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.ambient.AmbientModeSupport
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sagar.android.logutilmaster.LogUtil
import com.sagar.android.watchapp.core.KeyWordsAndConstants.CAPABILITY_PHONE_APP
import com.sagar.android.watchapp.databinding.ActivityLauncherBinding
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class Launcher : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider, KodeinAware {

    override val kodein: Kodein by kodein()

    private val logUtil: LogUtil by instance()
    private lateinit var binding: ActivityLauncherBinding
    private lateinit var capabilityChangedListener: CapabilityClient.OnCapabilityChangedListener
    private lateinit var nodeWithAppInstalled: Node

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AmbientModeSupport.attach(
            this
        )

        prepareCapabilityChangeListener()

        binding.button.setOnClickListener {
            putDataToDataLayer()
        }
    }

    private fun setMessageToUI(message: String) {
        binding.message.text = message
    }

    private fun prepareCapabilityChangeListener() {
        capabilityChangedListener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            logUtil.logV("capability changed....")

            logUtil.logV("number of devices with app installed ${capabilityInfo.nodes.size}")

            pickBestNode(capabilityInfo.nodes)?.let { node ->
                nodeWithAppInstalled = node
                logUtil.logV("node >> Name- ${node.displayName}, Id- ${node.id}, IsNearBy- ${node.isNearby}")
                verifyAppAndUpdateUI()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Wearable.getCapabilityClient(this).addListener(
            capabilityChangedListener,
            CAPABILITY_PHONE_APP
        )

        checkIfPhoneHasAppInstalled()
    }

    override fun onPause() {
        super.onPause()

        Wearable.getCapabilityClient(this).removeListener(
            capabilityChangedListener,
            CAPABILITY_PHONE_APP
        )
    }

    private fun checkIfPhoneHasAppInstalled() {
        setMessageToUI("Initialising...")

        Wearable.getCapabilityClient(this)
            .getCapability(
                CAPABILITY_PHONE_APP,
                CapabilityClient.FILTER_ALL
            )
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { capabilityInfo ->
                        pickBestNode(capabilityInfo.nodes)?.let { node ->
                            nodeWithAppInstalled = node
                            logUtil.logV("node >> Name- ${node.displayName}, Id- ${node.id}, IsNearBy- ${node.isNearby}")
                            verifyAppAndUpdateUI()
                        } ?: run {
                            logUtil.logV("dint get any node")
                        }
                    }
                } else {
                    logUtil.logV("task failed")

                    setMessageToUI("Task failed")
                }
            }
    }

    private fun pickBestNode(nodes: Set<Node>): Node? {
        logUtil.logV("picking the best node.")

        var nodeToReturn: Node? = null

        for (node in nodes) {
            nodeToReturn = node
        }

        return nodeToReturn
    }

    private fun verifyAppAndUpdateUI() {
        if (this::nodeWithAppInstalled.isInitialized) {
            logUtil.logV("node is ready. ")

            when (
                PhoneDeviceType.getPhoneDeviceType(applicationContext)
                ) {
                PhoneDeviceType.DEVICE_TYPE_ANDROID -> {
                    logUtil.logV("the device is android device.")
                    setMessageToUI("device is android.")

                    sendMessage()
                }
                else -> {
                    logUtil.logV("the device is not an android device")
                    setMessageToUI("device is not android.")
                }
            }
        } else {
            logUtil.logV("node not initialised yet, wait for some time ...")
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)

                logUtil.logV("entered the ambient mode")
            }

            override fun onExitAmbient() {
                super.onExitAmbient()

                logUtil.logV("exit the ambient mode")
            }
        }
    }

    private fun sendMessage() {
        MessageHandlerThread(
            this,
            logUtil
        ).start()
    }

    class MessageHandlerThread(private val context: Context, private val logUtil: LogUtil) :
        Thread() {

        override fun run() {
            super.run()

            Wearable.getNodeClient(context)
                .connectedNodes
                .addOnCompleteListener { p0 ->
                    if (p0.isSuccessful) {
                        p0.result?.forEach { node ->
                            Wearable.getMessageClient(context)
                                .sendMessage(
                                    node.id,
                                    "/path",
                                    "message...".toByteArray()
                                )
                                .addOnCompleteListener { p0 -> logUtil.logV("message result >> $p0") }
                        }
                    }
                }
        }
    }

    private fun putDataToDataLayer() {
        val putDataMapRequest = PutDataMapRequest.create("/dataa")
        putDataMapRequest.dataMap.putString("message", System.currentTimeMillis().toString())
        val request = putDataMapRequest.asPutDataRequest()
        request.setUrgent()

        Wearable.getDataClient(applicationContext).putDataItem(request)
            .addOnSuccessListener {
                logUtil.logV("success")
            }
            .addOnFailureListener {
                logUtil.logV("failed")
            }
    }
}