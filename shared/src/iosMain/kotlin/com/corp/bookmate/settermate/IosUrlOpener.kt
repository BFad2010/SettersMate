package com.corp.bookmate.settermate

// Exported to ObjC as a protocol so Swift can implement it with a plain class.
// Kotlin lambda types are NOT bridged as ObjC blocks, so passing a Swift
// closure directly as a (String) -> Unit parameter doesn't work.
interface IosUrlOpener {
    fun openUrl(url: String)
}
