package com.example.data.model

object MarketplaceDepartment {
    const val ALL = "All Departments"
    const val CLOTHING = "Clothing & Apparel"
    const val ELECTRONICS = "Electronics & Mobiles"
    const val FOOTWEAR = "Footwear & Shoes"
    const val HOME = "Home & Lifestyle"
    const val GROCERY = "Packaged Groceries"

    val DEPARTMENTS = listOf(
        ALL,
        CLOTHING,
        ELECTRONICS,
        FOOTWEAR,
        HOME,
        GROCERY
    )

    fun getDepartmentPunjabi(dept: String): String {
        return when (dept) {
            ALL -> "ਸਾਰੇ ਵਿਭਾਗ"
            CLOTHING -> "ਕੱਪੜੇ ਤੇ ਫੈਸ਼ਨ"
            ELECTRONICS -> "ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਤੇ ਮੋਬਾਈਲ"
            FOOTWEAR -> "ਜੁੱਤੀਆਂ ਤੇ ਸ਼ੂਜ਼"
            HOME -> "ਘਰੇਲੂ ਤੇ ਰਸੋਈ ਸਮਾਨ"
            GROCERY -> "ਸੀਲਬੰਦ ਗ੍ਰੋਸਰੀ ਤੇ ਫ੍ਰੋਜ਼ਨ"
            else -> dept
        }
    }
}
