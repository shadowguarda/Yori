package eu.kanade.tachiyomi.ui.source.globalsearch

import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.core.view.isVisible
import coil3.dispose
import com.google.android.material.color.MaterialColors
import eu.kanade.tachiyomi.databinding.SourceGlobalSearchControllerCardItemBinding
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.makeShapeCorners
import eu.kanade.tachiyomi.util.view.setCards
import yori.domain.manga.models.cover
import yori.util.coil.loadManga

class GlobalSearchMangaHolder(view: View, private val adapter: GlobalSearchCardAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    private val binding = SourceGlobalSearchControllerCardItemBinding.bind(view)

    init {
        itemView.setOnClickListener {
            val item = adapter.getItem(flexibleAdapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaClick(item.manga)
            }
        }
        val bottom = 2.dpToPx
        val others = 5.dpToPx
        (binding.constraintLayout.foreground as? RippleDrawable)?.apply {
            setLayerSize(1, 0, 0)
            for (i in 0 until numberOfLayers) {
                setLayerInset(i, others, others, others, bottom)
            }
        }
        binding.favoriteButton.shapeAppearanceModel =
            binding.card.makeShapeCorners(binding.card.radius, binding.card.radius)
        itemView.setOnLongClickListener {
            adapter.mangaClickListener.onMangaLongClick(flexibleAdapterPosition, adapter)
            true
        }
        setCards(adapter.showOutlines, binding.card, binding.favoriteButton)
    }

    fun bind(manga: Manga) {
        binding.title.text = highlightedTitle(manga.title, adapter.controller.presenter.query)
        binding.favoriteButton.isVisible = manga.favorite
        setImage(manga)
    }

    private fun highlightedTitle(title: String, query: String): CharSequence {
        val matches = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (matches.isEmpty()) return title

        val spannable = SpannableString(title)
        val accentColor = MaterialColors.getColor(itemView, android.R.attr.colorAccent)
        matches.forEach { match ->
            var start = 0
            while (start < title.length) {
                val index = title.indexOf(match, start, ignoreCase = true)
                if (index < 0) break
                val end = index + match.length
                spannable.setSpan(
                    ForegroundColorSpan(accentColor),
                    index,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    index,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                start = end
            }
        }
        return spannable
    }

    fun setImage(manga: Manga) {
        binding.itemImage.dispose()
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            binding.itemImage.loadManga(manga.cover(), binding.progress)
        }
    }
}
