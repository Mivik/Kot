@file:Suppress("NOTHING_TO_INLINE")

package com.mivik.kot

import java.io.File

private inline fun <T : Any> T?.nonnull(block: (T) -> Unit) {
	this ?: return
	block(this)
}

class DocumentBuilder(val builder: StringBuilder) {
	val endl: Unit
		get() {
			builder.append('\n')
		}

	inline fun line(line: Any?) {
		builder.append(line, '\n')
	}

	inline operator fun String.minus(value: Any?) {
		value ?: return
		builder.append(this, '=', value, ';')
	}

	inline operator fun String.div(value: Any?) {
		value ?: return
		builder.append(this, '=', value, ";\n")
	}

	inline fun append(name: Any?) {
		builder.append(name)
	}

	fun append(vararg all: Any?) {
		for (i in all) append(i)
	}
}

abstract class Document {
	fun build(builder: StringBuilder) {
		build(DocumentBuilder(builder))
	}

	abstract fun build(builder: DocumentBuilder)

	override fun toString(): String =
		StringBuilder().also {
			build(it)
		}.toString()
}

class Kot : Document() {
	private val elements = mutableListOf<Document>()

	class Node(val name: String) : Document() {
		var width: Float? = null
		var height: Float? = null
		var label: String? = null
		var color: String? = null
		var style: String? = null
		var shape: String? = null
		var sides: Int? = null
		var peripheries: String? = null
		var skew: Float? = null

		override fun build(builder: DocumentBuilder) {
			builder.run {
				append(name, " [")
				"width" - width
				"height" - height
				"label" - label
				"color" - color
				"style" - style
				"shape" - shape
				"sides" - sides
				"peripheries" - peripheries
				"skew" - skew
				append("]")
				endl
			}
		}
	}

	abstract class Edge(
		val from: Node,
		val to: Node
	) : Document() {
		var width: Float? = null
		var height: Float? = null
		var label: String? = null
		var color: String? = null
		var style: String? = null

		override fun build(builder: DocumentBuilder) {
			builder.run {
				append(" [")
				"width" - width
				"height" - height
				"label" - label
				"color" - color
				"style" - style
				append("]")
				endl
			}
		}
	}

	class DirectedEdge(from: Node, to: Node) : Edge(from, to) {
		override fun build(builder: DocumentBuilder) {
			builder.append(from.name, "->", to.name)
			super.build(builder)
		}
	}

	class UndirectedEdge(from: Node, to: Node) : Edge(from, to) {
		override fun build(builder: DocumentBuilder) {
			builder.append(from.name, "--", to.name)
			super.build(builder)
		}
	}

	class GraphBuilder(
		val directed: Boolean = false
	) : Document() {
		var name: String? = null
		val size: String? = null
		var strict: Boolean = false

		private val nodes = mutableMapOf<String, Node>()
		private val edges = mutableListOf<Edge>()

		fun node(name: String): Node = nodes.getOrPut(name) { Node(name) }
		fun node(name: String, block: Node.() -> Unit) = node(name).apply(block)

		fun link(from: Node, to: Node, block: (Edge.() -> Unit)? = null) =
			(if (directed) DirectedEdge(from, to) else UndirectedEdge(from, to)).also {
				edges.add(it)
				block?.invoke(it)
			}

		fun link(from: String, to: String, block: (Edge.() -> Unit)? = null) = link(node(from), node(to), block)

		infix fun Node.to(other: Node) = link(this, other)
		infix fun String.to(other: String) = link(node(this), node(other))

		override fun build(builder: DocumentBuilder) {
			builder.run {
				if (strict) append("strict ")
				if (directed) append("di")
				append("graph ")
				name.nonnull { append(name, ' ') }
				line("{")
				"size" - size
				nodes.forEach { it.value.build(builder) }
				edges.forEach { it.build(builder) }
				line("}")
			}
		}
	}

	fun graph(directed: Boolean = true, block: GraphBuilder.() -> Unit): GraphBuilder =
		GraphBuilder(directed).apply(block).also { elements.add(it) }

	enum class OutputFormat(val suffix: String) {
		SVG("svg"), PNG("png"), JPG("jpg")
	}

	fun renderFile(
		format: OutputFormat
	): File =
		File.createTempFile("kot", format.suffix).also { it.writeBytes(render(format)) }

	fun render(
		format: OutputFormat
	): ByteArray {
		val pro = Runtime.getRuntime().exec(arrayOf("dot", "-T${format.suffix}"))
		val output = pro.outputStream
		output.write(toString().toByteArray())
		output.close()
		return pro.inputStream.readBytes()
	}

	override fun build(builder: DocumentBuilder) {
		elements.forEach { it.build(builder) }
	}
}

inline fun kot(block: Kot.() -> Unit) = Kot().apply(block)