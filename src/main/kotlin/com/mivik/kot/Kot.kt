@file:Suppress("NOTHING_TO_INLINE")

package com.mivik.kot

import org.apache.commons.text.StringEscapeUtils
import java.io.File

private inline fun <T : Any> T?.nonnull(block: (T) -> Unit) {
	this ?: return
	block(this)
}

inline fun String.escape(): String = "\"${StringEscapeUtils.escapeJava(this)}\""

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
		builder.append(
			this, '=', when (value) {
				null -> "null"
				is String -> value.escape()
				else -> value.toString()
			}, ';'
		)
	}

	inline fun append(value: Any?) {
		builder.append(value)
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

object Kot {
	class Node internal constructor(val name: String) : Document() {
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
				append(name.escape(), " [")
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

	abstract class Edge internal constructor(
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

	class DirectedEdge internal constructor(from: Node, to: Node) : Edge(from, to) {
		override fun build(builder: DocumentBuilder) {
			builder.append(from.name.escape(), "->", to.name.escape())
			super.build(builder)
		}
	}

	class UndirectedEdge internal constructor(from: Node, to: Node) : Edge(from, to) {
		override fun build(builder: DocumentBuilder) {
			builder.append(from.name.escape(), "--", to.name.escape())
			super.build(builder)
		}
	}

	class Graph internal constructor(
		val directed: Boolean = false,
		val subGraph: Boolean = false
	) : Document() {
		var name: String? = null
		val size: String? = null
		var strict: Boolean = false

		val nodes = mutableMapOf<String, Node>()
		val edges = mutableListOf<Edge>()
		val subGraphs = mutableListOf<Graph>()

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

		fun subgraph(block: Graph.() -> Unit): Graph =
			Graph(directed).apply(block).also { subGraphs.add(it) }

		override fun build(builder: DocumentBuilder) {
			builder.run {
				if (strict) append("strict ")
				if (directed) append("di")
				if (subGraph) append("sub")
				append("graph ")
				name.nonnull { append(it.escape(), ' ') }
				line("{")
				"size" - size
				nodes.forEach { it.value.build(builder) }
				edges.forEach { it.build(builder) }
				subGraphs.forEach {
					append("sub")
				}
				line("}")
			}
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
	}

	fun graph(block: Graph.() -> Unit): Graph =
		Graph(false).apply(block)

	fun digraph(block: Graph.() -> Unit): Graph =
		Graph(true).apply(block)

	enum class OutputFormat(val suffix: String) {
		SVG("svg"), PNG("png"), JPG("jpg")
	}
}

inline fun kot(block: Kot.() -> Unit) = Kot.apply(block)