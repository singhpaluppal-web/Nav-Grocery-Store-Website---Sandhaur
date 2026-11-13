package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TaxInvoiceGenerator {

    fun generateInvoiceText(order: Order, transaction: PaymentTransaction?): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        val invoiceDate = dateFormat.format(Date(order.timestamp))
        val invoiceNo = "INV-" + order.orderId.replace("NAV-", "")

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("         TAX INVOICE / CASH MEMO         \n")
        sb.append("       NAVO HUB / NAV GROCERY STORE       \n")
        sb.append("  Main Bazaar, Sandhaur, Malerkotla, Punjab\n")
        sb.append("       GSTIN: ${OwnerSecureLedger.GSTIN}        \n")
        sb.append("       State Code: 03 (Punjab, India)    \n")
        sb.append("  Helpline / WhatsApp: +91 8847370905    \n")
        sb.append("=========================================\n")
        sb.append("Invoice No:   $invoiceNo\n")
        sb.append("Order Ref:    ${order.orderId}\n")
        sb.append("Date & Time:  $invoiceDate\n")
        sb.append("-----------------------------------------\n")
        sb.append("BILL TO:\n")
        sb.append("Customer:     ${order.customerName.ifBlank { "Valued Customer" }}\n")
        sb.append("Phone:        ${order.customerPhone.ifBlank { "N/A" }}\n")
        sb.append("Address:      ${order.address}\n")
        sb.append("Place of Supply: Punjab (03)\n")
        sb.append("-----------------------------------------\n")
        sb.append(String.format("%-18s %3s %6s %7s\n", "Item Name", "Qty", "Rate", "Total"))
        sb.append("-----------------------------------------\n")

        order.items.forEach { item ->
            val p = item.product
            val itemName = if (p.name.length > 18) p.name.take(16) + ".." else p.name
            sb.append(String.format("%-18s %3d %6d %7d\n", itemName, item.quantity, p.wholesalePrice, item.totalSalePrice))
            if (item.variantSummary.isNotBlank()) {
                sb.append("  (${item.variantSummary})\n")
            }
        }

        sb.append("-----------------------------------------\n")
        sb.append(String.format("%-28s %8d\n", "Gross Total MRP:", order.totalMrp))
        sb.append(String.format("%-28s -%7d\n", "Platform Wholesale Discount:", order.totalDiscount))
        sb.append(String.format("%-28s %8s\n", "Estimated Taxes (CGST+SGST):", "INCLUDED"))
        sb.append(String.format("%-28s %8s\n", "Delivery / Shipping Fee:", "FREE"))
        sb.append("=========================================\n")
        sb.append(String.format("%-28s ₹%7d\n", "NET PAYABLE / PAID:", order.finalTotal))
        sb.append("=========================================\n")
        sb.append("PAYMENT INFORMATION:\n")
        sb.append("Payment Mode:    ${order.paymentMethod}\n")
        sb.append("Payment Status:  ${order.paymentStatus}\n")
        if (transaction != null) {
            sb.append("Transaction ID:  ${transaction.transactionId}\n")
            sb.append("Bank UTR / Ref:  ${transaction.utrOrRef}\n")
        }
        sb.append("Central Merchant: ${CommissionConfig.CENTRAL_MERCHANT_NAME}\n")
        sb.append("-----------------------------------------\n")
        sb.append("TERMS & CONDITIONS:\n")
        sb.append("1. All items 100% genuine factory sealed.\n")
        sb.append("2. Cold-chain items packed in sub-zero thermal packaging.\n")
        sb.append("3. Central platform escrow guarantee by Nav Grocery Store.\n")
        sb.append("Thank you for shopping at Navo Hub (ਨਵੋ ਹੱਬ)!\n")
        sb.append("=========================================\n")

        return sb.toString()
    }
}
