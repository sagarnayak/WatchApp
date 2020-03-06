package com.sagar.android.watchapp.ui.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.ambient.AmbientModeSupport
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.sagar.android.logutilmaster.LogUtil
import com.sagar.android.watchapp.R
import com.sagar.android.watchapp.core.KeyWordsAndConstants.CAPABILITY_PHONE_APP
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class Launcher : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider, KodeinAware {

    override val kodein: Kodein by kodein()

    private val logUtil: LogUtil by instance()
    private lateinit var capabilityChangedListener: CapabilityClient.OnCapabilityChangedListener
    private lateinit var nodeWithAppInstalled: Node

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        AmbientModeSupport.attach(
                this
        )

        prepareCapabilityChangeListener()
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
                            }
                        }
                    } else {
                        logUtil.logV("task failed")
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
}