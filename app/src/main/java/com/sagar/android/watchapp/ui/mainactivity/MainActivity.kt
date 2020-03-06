package com.sagar.android.watchapp.ui.mainactivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.wearable.*
import com.google.android.wearable.intent.RemoteIntent
import com.sagar.android.logutilmaster.LogUtil
import com.sagar.android.watchapp.core.KeyWordsAndConstants.APP_IN_PLAY_STORE
import com.sagar.android.watchapp.core.KeyWordsAndConstants.CAPABILITY_WEAR_APP
import com.sagar.android.watchapp.core.KeyWordsAndConstants.HANDSHAKE
import com.sagar.android.watchapp.core.MessageHandlerThread
import com.sagar.android.watchapp.databinding.ActivityMainBinding
import com.sagar.android.watchapp.ui.mainactivity.adapter.Adapter
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask


class MainActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by kodein()

    private val logUtil: LogUtil by instance()
    private lateinit var binding: ActivityMainBinding
    private lateinit var capabilityChangedListener: CapabilityClient.OnCapabilityChangedListener
    private lateinit var messageReceivedListener: MessageClient.OnMessageReceivedListener
    private lateinit var nodesWithAppInstalled: Set<Node>
    private lateinit var nodesInContactMayNotHaveAppInstalled: List<Node>
    private var nodeAvailableForHandShake: ArrayList<Node> = ArrayList()
    private lateinit var adapter: Adapter
    private var handshakeTimer: TimerTask = timerTask {
        sendEmergencyHandShakeReq()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpRecyclerView()
        prepareCapabilityChangeListener()
        prepareMessageReceivedListener()

        binding.buttonInstall.setOnClickListener {
            openPlayStoreWithAppInDevicesWithoutAppInstalled()
        }

        binding.buttonRefresh.setOnClickListener {
            findAllWearDevices()
        }
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        Adapter(
            nodeAvailableForHandShake,
            object : Adapter.Callback {
                override fun clicked(node: Node) {
                    handShake(node)
                }
            }
        ).apply {
            binding.recyclerView.adapter = this
            adapter = this
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
            binding.buttonInstall.visibility = View.GONE

            nodesWithAppInstalled = capabilityInfo.nodes
            capabilityInfo.nodes.forEach { node ->
                logUtil.logV("node >> Name- ${node.displayName}, Id- ${node.id}, IsNearBy- ${node.isNearby}")
            }

            findAllWearDevices()

            verifyNodes()

            availableNodeListChanged()
        }
    }

    private fun prepareMessageReceivedListener() {
        messageReceivedListener = MessageClient.OnMessageReceivedListener { messageEvent ->
            logUtil.logV(
                "new message received >> ${messageEvent.requestId} path >> ${messageEvent.path} || message >> ${String(
                    messageEvent.data
                )}"
            )

            if (messageEvent.path == HANDSHAKE) {
                logUtil.logV("got handshake reply.")
                handshakeTimer.cancel()
                Toast.makeText(
                    this,
                    "Handshake Complete",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        Wearable.getCapabilityClient(this).removeListener(
            capabilityChangedListener,
            CAPABILITY_WEAR_APP
        )

        Wearable.getMessageClient(this)
            .removeListener(messageReceivedListener)
    }

    override fun onResume() {
        super.onResume()

        Wearable.getCapabilityClient(this)
            .addListener(
                capabilityChangedListener,
                CAPABILITY_WEAR_APP
            )

        Wearable.getMessageClient(this)
            .addListener(messageReceivedListener)

        logUtil.logV("Initialising...")
        setStatusToScreen(
            """
                Initialising...
                Please make sure your smart watch is paired to your smartphone.
            """.trimIndent()
        )

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
                        availableNodeListChanged()
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
            logUtil.logV("there are no devices to connect")
            setStatusToScreen(
                """
                    there are no devices to connect.
                    Please make sure that your smart watch is paired and connected to your device.
                """.trimIndent()
            )
            binding.buttonInstall.visibility = View.GONE
            return
        }

        if (
            nodesWithAppInstalled.isEmpty()
        ) {
            logUtil.logV("no device has the app installed.")
            setStatusToScreen(
                """
                    no device has the app installed.
                    Click button to open prompt to install app in watch.
                """.trimIndent()
            )
            return
        }

        if (
            nodesWithAppInstalled.size < nodesInContactMayNotHaveAppInstalled.size
        ) {
            logUtil.logV("there are few devices with app and few have not installed the app")
            setStatusToScreen(
                """
                    there are few devices with app and few have not installed the app.
                    Click button to open prompt to install app in watch. 
                """.trimIndent()
            )
            return
        }

        if (
            nodesWithAppInstalled.size == nodesInContactMayNotHaveAppInstalled.size
        ) {
            logUtil.logV("all device have the app installed. good to go...")
            setStatusToScreen("all device have the app installed. good to go...")
            binding.buttonInstall.visibility = View.GONE
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

    private fun availableNodeListChanged() {
        nodeAvailableForHandShake.apply {
            clear()
            addAll(this)
        }
        adapter.notifyDataSetChanged()
    }

    private fun handShake(node: Node) {
        MessageHandlerThread(
            this,
            node,
            OnCompleteListener {
                logUtil.logV("sending message complete.")
                waitForHandShakeReply(node)
            }
        ).start()
    }

    private fun waitForHandShakeReply(node: Node) {
        logUtil.logV("waiting for handshake reply from ${node.displayName}")

        Timer().schedule(
            handshakeTimer,
            2000
        )
    }

    private fun sendEmergencyHandShakeReq() {}
}