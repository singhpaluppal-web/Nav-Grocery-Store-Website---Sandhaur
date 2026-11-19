package com.example

import com.example.data.model.FirestoreOrderItem
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.Product
import com.example.data.security.OrderStatusChange
import com.example.data.security.OrderStatusTracker
import com.example.ui.components.formatOrderStatusNotificationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusAndReorderTest {

    @Test
    fun trackerDoesNotEmitOnInitialLoadAndEmitsOnTransition() {
        val tracker = OrderStatusTracker()
        val uid = "uid_1"

        val initial = listOf(
            FirestoreOrderRecord(id = "NAV-1001", orderId = "NAV-1001", status = "Packing"),
            FirestoreOrderRecord(id = "NAV-1002", orderId = "NAV-1002", status = "Order Placed")
        )
        assertTrue(tracker.onOrdersSnapshot(uid, initial).isEmpty())

        // Duplicate identical snapshot -> no notification
        assertTrue(tracker.onOrdersSnapshot(uid, initial).isEmpty())

        // Real-time transition Packing -> Out for Delivery on NAV-1001
        val updated = listOf(
            FirestoreOrderRecord(id = "NAV-1001", orderId = "NAV-1001", status = "Out for Delivery"),
            FirestoreOrderRecord(id = "NAV-1002", orderId = "NAV-1002", status = "Order Placed")
        )
        val changes = tracker.onOrdersSnapshot(uid, updated)
        assertEquals(1, changes.size)
        assertEquals("NAV-1001", changes[0].orderId)
        assertEquals("Packing", changes[0].previousStatus)
        assertEquals("Out for Delivery", changes[0].newStatus)

        // Switching UID resets baseline without emitting spurious alerts
        val otherUidChanges = tracker.onOrdersSnapshot("uid_2", updated)
        assertTrue(otherUidChanges.isEmpty())
    }

    @Test
    fun resolveMergedStatusPrefersHigherLifecycleStage() {
        assertEquals(
            "Out for Delivery",
            OrderStatusTracker.resolveMergedStatus("Out for Delivery", "Order Placed")
        )
        assertEquals(
            "Out for Delivery",
            OrderStatusTracker.resolveMergedStatus("Packing", "Out for Delivery")
        )
        assertEquals(
            "Delivered",
            OrderStatusTracker.resolveMergedStatus("Out for Delivery", "Delivered")
        )
        assertEquals(
            "Packing",
            OrderStatusTracker.resolveMergedStatus("Packing", "")
        )
    }

    @Test
    fun formatNotificationTextSupportsEnglishAndPunjabi() {
        val change = OrderStatusChange(
            id = "chg_1",
            orderId = "NAV-1001",
            previousStatus = "Packing",
            newStatus = "Out for Delivery"
        )
        assertEquals(
            "Order NAV-1001: Packing → Out for Delivery",
            formatOrderStatusNotificationText(change, isPunjabi = false)
        )
        assertEquals(
            "ਆਰਡਰ NAV-1001: Packing → Out for Delivery",
            formatOrderStatusNotificationText(change, isPunjabi = true)
        )
    }

    @Test
    fun reorderStockFilterAddsOnlyInStockCatalogItems() {
        val catalog = listOf(
            Product(id = "NAV001", name = "Aashirvaad Atta 10kg", mrp = 500, sale = 440, stock = 12),
            Product(id = "NAV002", name = "Verka Desi Ghee 1L", mrp = 650, sale = 572, stock = 0),
            Product(id = "NAV003", name = "Tata Salt 1kg", mrp = 28, sale = 25, stock = 30)
        ).associateBy { it.id }

        val pastItems = listOf(
            FirestoreOrderItem(productId = "NAV001", name = "Aashirvaad Atta 10kg", qty = 2),
            FirestoreOrderItem(productId = "NAV002", name = "Verka Desi Ghee 1L", qty = 1),
            FirestoreOrderItem(productId = "NAV003", name = "Tata Salt 1kg", qty = 3)
        )

        val reordered = pastItems.filter { item ->
            val p = catalog[item.productId]
            p != null && p.stock > 0
        }
        assertEquals(2, reordered.size)
        assertEquals(listOf("NAV001", "NAV003"), reordered.map { it.productId })
    }
}
