package dev.oneuiproject.oneui.popover

enum class PopOverPosition(val flag: Int) {
    @JvmField
    CENTER(68),
    @JvmField
    TOP_LEFT(1/*top*/ or 16/*left*/),
    @JvmField
    TOP_CENTER(1 or 64/*center horizontally*/),
    @JvmField
    TOP_RIGHT(1 or 32/*right*/),
    @JvmField
    BOTTOM_LEFT(2/*bottom*/ or 16),
    @JvmField
    BOTTOM_CENTER(2 or 64),
    @JvmField
    BOTTOM_RIGHT(2 or 32),
    @JvmField
    CENTER_RIGHT(4/*center vertically*/ or 32),
    @JvmField
    CENTER_LEFT(4 or 16)
}