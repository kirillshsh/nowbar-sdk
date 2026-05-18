package com.nowbar.demo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nowbar.api.NowBarDiagnostics
import com.nowbar.api.notification.LiveUpdateDiagnostics

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        setContentView(buildContent())
        handleActionIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleActionIntent(intent)
    }

    private fun buildContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        root.addView(TextView(this).apply {
            textSize = 22f
            text = getString(R.string.app_name)
        })
        root.addView(TextView(this).apply {
            textSize = 14f
            text = buildStatusText()
            setPadding(0, 12, 0, 24)
        })

        addActionButton(root, "Timer countdown", DemoNowBarService.ACTION_SHOW_TIMER)
        addActionButton(root, "Workout progress", DemoNowBarService.ACTION_SHOW_WORKOUT)
        addActionButton(root, "Navigation", DemoNowBarService.ACTION_SHOW_NAVIGATION)
        addActionButton(root, "Delivery journey", DemoNowBarService.ACTION_SHOW_DELIVERY)
        addActionButton(root, "MetricStyle template", DemoNowBarService.ACTION_SHOW_METRICS)
        addActionButton(root, "Media controls", DemoNowBarService.ACTION_SHOW_MEDIA)
        addActionButton(root, "Call actions", DemoNowBarService.ACTION_SHOW_CALL)
        addActionButton(root, "Call screening", DemoNowBarService.ACTION_SHOW_CALL_SCREENING)
        addActionButton(root, "BigTextStyle details", DemoNowBarService.ACTION_SHOW_BIG_TEXT)
        addActionButton(root, "Custom actions", DemoNowBarService.ACTION_SHOW_CUSTOM_ACTIONS)
        addActionButton(root, "Samsung dump style", DemoNowBarService.ACTION_SHOW_DUMP)
        addActionButton(root, "Samsung native style", DemoNowBarService.ACTION_SHOW_NATIVE_STYLE)
        addActionButton(root, "Pause action", DemoNowBarService.ACTION_PAUSE)
        addActionButton(root, "Resume action", DemoNowBarService.ACTION_RESUME)
        addActionButton(root, "Next action", DemoNowBarService.ACTION_NEXT)
        addActionButton(root, "Unpin current", DemoNowBarService.ACTION_UNPIN)
        addActionButton(root, "Dismiss Now Bar", DemoNowBarService.ACTION_DISMISS)
        addActionButton(root, "Stop and remove", DemoNowBarService.ACTION_STOP)

        root.addView(Button(this).apply {
            text = getString(R.string.now_bar_settings)
            setOnClickListener {
                runCatching {
                    val intent = NowBarDiagnostics.resolveNowBarSettingsIntent(this@MainActivity)
                    if (intent == null) {
                        Toast.makeText(this@MainActivity, "Settings screen is not available", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(intent)
                    }
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Settings screen is not available", Toast.LENGTH_SHORT).show()
                }
            }
        })

        root.addView(Button(this).apply {
            text = getString(R.string.live_update_settings)
            setOnClickListener {
                runCatching {
                    val intent = LiveUpdateDiagnostics.resolveManageAppPromotedNotificationsIntent(this@MainActivity)
                    if (intent == null) {
                        Toast.makeText(this@MainActivity, "Settings screen is not available", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(intent)
                    }
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Settings screen is not available", Toast.LENGTH_SHORT).show()
                }
            }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun buildStatusText(): String {
        return NowBarDiagnostics.inspect(this).toDisplayString()
    }

    private fun addActionButton(root: LinearLayout, label: String, action: String) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener {
                startDemoAction(action)
            }
        })
    }

    private fun handleActionIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (DemoNowBarService.isDemoAction(action)) {
            startDemoAction(action)
        }
    }

    private fun startDemoAction(action: String) {
        ContextCompat.startForegroundService(
            this,
            DemoNowBarService.intent(this, action)
        )
    }
}
