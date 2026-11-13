package com.example.data.model

import com.example.data.model.OwnerSecureLedger

object CommissionConfig {
    const val SUPER_ADMIN_PIN = OwnerSecureLedger.SUPER_ADMIN_PIN
    const val CENTRAL_MERCHANT_NAME = OwnerSecureLedger.OWNER_LEGAL_NAME
    const val CENTRAL_ACCOUNT_UPI = OwnerSecureLedger.CENTRAL_MERCHANT_BIZ_VPA
    const val CENTRAL_PHONE = StoreProfile.HELPLINE_PHONE
    const val CENTRAL_EMAIL = OwnerSecureLedger.OWNER_ADMIN_EMAIL
    const val CENTRAL_LOCATION = OwnerSecureLedger.REGISTERED_TAX_ADDRESS

    // Default Commission percentages per department
    var clothingCommissionRate: Double = 0.08    // 8%
    var electronicsCommissionRate: Double = 0.05 // 5%
    var footwearCommissionRate: Double = 0.08    // 8%
    var homeCommissionRate: Double = 0.08        // 8%
    var groceryCommissionRate: Double = 0.05     // 5%
    var defaultCommissionRate: Double = 0.08     // 8%

    fun getCommissionRateForDepartment(department: String): Double {
        return when (department) {
            MarketplaceDepartment.CLOTHING -> clothingCommissionRate
            MarketplaceDepartment.ELECTRONICS -> electronicsCommissionRate
            MarketplaceDepartment.FOOTWEAR -> footwearCommissionRate
            MarketplaceDepartment.HOME -> homeCommissionRate
            MarketplaceDepartment.GROCERY -> groceryCommissionRate
            else -> defaultCommissionRate
        }
    }

    fun calculateCommission(amount: Int, department: String): Int {
        val rate = getCommissionRateForDepartment(department)
        return kotlin.math.round(amount * rate).toInt()
    }
}
