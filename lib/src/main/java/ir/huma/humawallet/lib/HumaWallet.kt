package ir.huma.humawallet.lib

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class HumaWallet {

    private var activity: Activity? = null
    private var paymentToken: String? = null
    private var isFastPayment = false
    private var onPayListener: OnPayListener? = null
    private var isReceiverRegistered = false

    constructor(activity: Activity?) {
        this.activity = activity
    }

    fun setPaymentToken(token: String?): HumaWallet {
        this.paymentToken = token
        return this
    }

    fun setFastPayment(isFast: Boolean): HumaWallet {
        this.isFastPayment = isFast
        return this
    }

    fun getContext(): Activity? = activity

    fun getOnPayListener(): OnPayListener? = onPayListener

    fun setOnPayListener(onPayListener: OnPayListener?): HumaWallet {
        this.onPayListener = onPayListener
        return this
    }

    fun send() {
        if (paymentToken.isNullOrEmpty()) {
            throw RuntimeException("please set paymentToken!!!")
        }
        if (onPayListener == null) {
            throw RuntimeException("please setOnPayListener in java code!!!")
        }

        val currentActivity = activity ?: return

        when {
            isNewWalletAvailable() -> {
                registerPaymentReceiver()
                currentActivity.startActivity(newWalletIntent())
            }

            isLegacyWalletInstalled() -> {
                registerPaymentReceiver()
                currentActivity.startActivity(legacyWalletIntent())
            }

            else -> {
                Toast.makeText(
                    activity,
                    "لطفا ابتدا برنامه هوما استور را نصب کنید.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun unregister() {
        if (!isReceiverRegistered) return
        try {
            activity?.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        isReceiverRegistered = false
    }

    @Deprecated("Use unregister()", ReplaceWith("unregister()"))
    fun unregiter() {
        unregister()
    }

    private fun isNewWalletAvailable(): Boolean {
        val currentActivity = activity ?: return false
        return newWalletIntent().resolveActivity(currentActivity.packageManager) != null
    }

    private fun isLegacyWalletInstalled(): Boolean {
        return try {
            activity!!.packageManager.getPackageInfo(LEGACY_WALLET_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun newWalletIntent(): Intent {
        val currentActivity = activity
            ?: throw IllegalStateException("Activity is required to launch payment")

        val uri = "$NEW_WALLET_SCHEME://$NEW_WALLET_HOST".toUri()
            .buildUpon()
            .path(NEW_WALLET_PAYMENT_PATH)
            .appendQueryParameter(QUERY_TOKEN, paymentToken)
            .appendQueryParameter(QUERY_PACKAGE, currentActivity.packageName)
            .build()

        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun legacyWalletIntent(): Intent {
        val currentActivity = activity
            ?: throw IllegalStateException("Activity is required to launch payment")

        return Intent(Intent.ACTION_VIEW, LEGACY_WALLET_URI.toUri()).apply {
            putExtra("token", paymentToken)
            putExtra("isFastPayment", isFastPayment)
            putExtra("package", currentActivity.packageName)
            setPackage(LEGACY_WALLET_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("taskId", currentActivity.taskId)
        }
    }

    private fun registerPaymentReceiver() {
        val currentActivity = activity ?: return
        if (isReceiverRegistered) return

        ContextCompat.registerReceiver(
            currentActivity,
            receiver,
            IntentFilter(PAYMENT_RESULT_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )
        isReceiverRegistered = true
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val listener = onPayListener ?: return
                val callerActivity = getContext() ?: return

                val isForThisApp = !intent.hasExtra(EXTRA_PACKAGE_NAME) ||
                        intent.getStringExtra(EXTRA_PACKAGE_NAME) == callerActivity.packageName

                if (!isForThisApp) return

                if (intent.getBooleanExtra(EXTRA_SUCCESS, false)) {
                    listener.onPayComplete(intent.getStringExtra(EXTRA_MESSAGE))
                } else {
                    listener.onPayFail(intent.getStringExtra(EXTRA_MESSAGE))
                }

                unregister()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    interface OnPayListener {
        fun onPayComplete(code: String?)
        fun onPayFail(message: String?)
    }

    private companion object {
        const val PAYMENT_RESULT_ACTION = "ir.huma.humawallet.paystatus"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_PACKAGE_NAME = "packageName"

        const val NEW_WALLET_SCHEME = "app"
        const val NEW_WALLET_HOST = "done.tech"
        const val NEW_WALLET_PAYMENT_PATH = "/payment"
        const val QUERY_TOKEN = "token"
        const val QUERY_PACKAGE = "package"

        const val LEGACY_WALLET_URI = "app://wallet.huma.ir"
        const val LEGACY_WALLET_PACKAGE = "ir.huma.humastore"
    }
}
