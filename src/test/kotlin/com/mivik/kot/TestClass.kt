package com.mivik.kot

fun main() {
	kot {
		graph {
			name = "test"
			node("\"qwe") {
			}
			link("test", "dd") {
				color = "red"
			}
		}
		val file = renderFile(Kot.OutputFormat.SVG)
		Runtime.getRuntime().exec(arrayOf("gio", "open", file.absolutePath))
	}
}