// CHECK_TYPESCRIPT_DECLARATIONS
// IGNORE_ANALYSIS_API_BASED_TYPESCRIPT_EXPORT: JS_IR, JS_IR_ES6
// JS_IR, JS_IR_ES6 KT-86648
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: JS_TESTS
// FILE: companion-extensions.kt

package foo

@JsExport
class ExportedWithCompanionExtensions

@JsExport
companion fun ExportedWithCompanionExtensions.append(value: String = "K"): String {
    return "O" + value
}
