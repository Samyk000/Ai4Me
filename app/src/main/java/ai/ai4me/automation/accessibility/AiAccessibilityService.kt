package ai.ai4me.automation.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

data class ScreenNodeInfo(
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val isClickable: Boolean,
    val bounds: String
)

data class ScreenSnapshot(
    val packageName: String,
    val activeWindowText: List<String>,
    val nodes: List<ScreenNodeInfo>,
    val timestamp: Long = System.currentTimeMillis()
)

interface AccessibilityNavigator {
    val isConnected: Boolean
    fun captureScreenSnapshot(): ScreenSnapshot?
    fun clickNodeByText(text: String): Boolean
    fun clickNodeByViewId(viewId: String): Boolean
    fun performCoordinateClick(x: Float, y: Float, onComplete: (Boolean) -> Unit)
}

class AiAccessibilityService : AccessibilityService(), AccessibilityNavigator {

    companion object {
        var activeInstance: AiAccessibilityService? = null
            private set
    }

    override val isConnected: Boolean
        get() = activeInstance != null

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are processed on demand during agent task execution
    }

    override fun onInterrupt() {
        // Interruption handling
    }

    override fun captureScreenSnapshot(): ScreenSnapshot? {
        val rootNode = rootInActiveWindow ?: return null
        val texts = mutableListOf<String>()
        val nodes = mutableListOf<ScreenNodeInfo>()
        val packageName = rootNode.packageName?.toString().orEmpty()

        fun traverse(node: AccessibilityNodeInfo) {
            val text = node.text?.toString()
            val contentDesc = node.contentDescription?.toString()
            val viewId = node.viewIdResourceName
            val isClickable = node.isClickable

            if (!text.isNullOrBlank()) {
                texts.add(text)
            } else if (!contentDesc.isNullOrBlank()) {
                texts.add(contentDesc)
            }

            val rect = Rect()
            node.getBoundsInScreen(rect)
            nodes.add(
                ScreenNodeInfo(
                    text = text,
                    contentDescription = contentDesc,
                    viewId = viewId,
                    isClickable = isClickable,
                    bounds = "${rect.left},${rect.top},${rect.right},${rect.bottom}"
                )
            )

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    traverse(child)
                }
            }
        }

        traverse(rootNode)
        return ScreenSnapshot(
            packageName = packageName,
            activeWindowText = texts,
            nodes = nodes
        )
    }

    override fun clickNodeByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val matchedNodes = rootNode.findAccessibilityNodeInfosByText(text)
        for (node in matchedNodes) {
            var target: AccessibilityNodeInfo? = node
            while (target != null) {
                if (target.isClickable) {
                    val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                target = target.parent
            }
        }
        return false
    }

    override fun clickNodeByViewId(viewId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val matchedNodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        for (node in matchedNodes) {
            var target: AccessibilityNodeInfo? = node
            while (target != null) {
                if (target.isClickable) {
                    val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                target = target.parent
            }
        }
        return false
    }

    override fun performCoordinateClick(x: Float, y: Float, onComplete: (Boolean) -> Unit) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete(false)
            }
        }, null)
    }
}
