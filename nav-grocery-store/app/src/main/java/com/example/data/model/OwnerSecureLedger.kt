package com.example.data.model

/**
 * SECURE BACKEND & ADMIN IDENTIFIERS
 *
 * CRITICAL PRIVACY RULES:
 * This file and its constants are used EXCLUSIVELY by:
 * 1. The dedicated NavKart Admin Controller App (com.navgrocerystore.navkartadmin)
 * 2. Automated server-side / escrow payment settlement workflows
 * 3. Formal post-purchase tax invoices (B2C/B2B GST compliant receipts)
 *
 * It is STRICTLY FORBIDDEN to render or reference these owner personal identifiers
 * on public buyer storefront screens, footers, public help centers, or public customer profiles.
 */
object OwnerSecureLedger {
    const val SUPER_ADMIN_PIN = "148020"

    // Owner Private Identifiers (Admin Only / Post-Purchase Tax Invoices)
    const val OWNER_LEGAL_NAME = "Amrit Pal Singh Uppal"
    const val OWNER_ADMIN_EMAIL = "singhpaluppal@gmail.com"
    const val GSTIN = "03AHPPU6185N1ZF"
    const val REGISTERED_TAX_ADDRESS = "Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020, India"

    // Central Settlement Escrow Account
    const val CENTRAL_MERCHANT_VPA = "8847370905@upi"
    const val CENTRAL_MERCHANT_SECONDARY_VPA = "singhpaluppal@okaxis"
    const val CENTRAL_MERCHANT_BIZ_VPA = "8847370905@okbizaxis"

    // Dedicated Admin Web Console
    const val ADMIN_WEB_CONSOLE_URL = "https://www.navgrocerystore.com/admin"
}
