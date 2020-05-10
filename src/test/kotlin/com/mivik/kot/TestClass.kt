package com.mivik.kot

fun main() {
	kot {
		graph {
			name = "test"
			node("test") {
			}
			link("test", "dd") {
				color = "red"
			}
		}
		val file = renderFile(Kot.OutputFormat.SVG)
		Runtime.getRuntime().exec(arrayOf("gio", "open", file.absolutePath))
	}
}