package com.ambient.launcher.tts

import org.jsoup.Jsoup
import org.jsoup.nodes.Entities

/**
 * Wraps each sentence of an article body in `<span class="tts-s" data-sidx="N">` so that:
 *  - the WebView JS bridge can register tap handlers per-sentence (tap-to-seek)
 *  - the highlight engine can toggle `.tts-active` on the currently-spoken sentence
 *
 * Paragraph-level identity is preserved via `TtsChunk.paragraphIndex` so that the
 * "double-tap prev/next" transport jumps to paragraph boundaries rather than sentences.
 *
 * We process only block-level text containers (p, h1-h4, li, blockquote). Inline
 * formatting (bold/italic/link) inside those blocks is flattened to plain text —
 * an acceptable trade-off for the MVP: TTS only reads plain text anyway, and
 * preserving per-sentence tap targets requires a flat text layout.
 */
internal object ArticleTtsPrep {
    private const val BLOCK_SELECTOR = "p, h1, h2, h3, h4, li, blockquote"

    data class Result(val html: String, val chunks: List<TtsChunk>)

    fun wrap(bodyHtml: String): Result {
        val doc = Jsoup.parseBodyFragment(bodyHtml)
        val chunks = mutableListOf<TtsChunk>()
        var paraIdx = 0

        for (block in doc.body().select(BLOCK_SELECTOR)) {
            val text = block.text().replace(Regex("""\s+"""), " ").trim()
            if (text.isEmpty()) continue
            val sentences = TtsChunker.splitSentences(text)
            if (sentences.isEmpty()) continue

            val sb = StringBuilder()
            for ((i, s) in sentences.withIndex()) {
                val idx = chunks.size
                chunks += TtsChunk(s, paraIdx)
                sb.append("<span class=\"tts-s\" data-sidx=\"").append(idx).append("\">")
                sb.append(Entities.escape(s))
                sb.append("</span>")
                if (i < sentences.lastIndex) sb.append(' ')
            }
            block.html(sb.toString())
            paraIdx++
        }

        return Result(doc.body().html(), chunks)
    }
}
