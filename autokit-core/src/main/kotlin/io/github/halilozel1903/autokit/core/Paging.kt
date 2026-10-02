package io.github.halilozel1903.autokit.core

/**
 * One page of a longer list.
 *
 * @property index zero based page number.
 * @property start index of the first item in the full list.
 * @property hasMore whether another page follows (show a "More" row).
 */
public data class Page<T>(
    val index: Int,
    val pageCount: Int,
    val start: Int,
    val items: List<T>,
) {
    val hasMore: Boolean get() = index < pageCount - 1
    val hasPrevious: Boolean get() = index > 0

    /** "Page 2 of 3". */
    val label: String get() = "Page ${index + 1} of $pageCount"
}

/**
 * Splits long content into pages that fit a template.
 *
 * Android Auto shows a limited number of rows per list (often 6 while driving). The usual pattern is
 * to show as many rows as fit, minus one, and use the last slot for a "More" row that opens the next
 * page. [pages] does that math; [CarList.paged] applies it to a list.
 */
public object CarPaging {

    /** Plain chunks of at most [size] items. An empty list gives no chunks. */
    public fun <T> chunk(items: List<T>, size: Int): List<List<T>> {
        require(size >= 1) { "size must be at least 1, was $size" }
        return items.chunked(size)
    }

    /**
     * Pages of at most [maxItemsPerPage] slots. With [reserveMoreSlot], every page except the last
     * keeps one slot free for a "More" row, so it holds `maxItemsPerPage - 1` items; the last page
     * uses every slot. An empty list gives one empty page, so screens always have something to show.
     */
    public fun <T> pages(items: List<T>, maxItemsPerPage: Int, reserveMoreSlot: Boolean = true): List<Page<T>> {
        require(maxItemsPerPage >= if (reserveMoreSlot) 2 else 1) {
            "maxItemsPerPage must be at least ${if (reserveMoreSlot) 2 else 1}, was $maxItemsPerPage"
        }
        val perPage = if (reserveMoreSlot) maxItemsPerPage - 1 else maxItemsPerPage
        val chunks = mutableListOf<IntRange>()
        var start = 0
        while (items.size - start > maxItemsPerPage) {
            chunks += start until start + perPage
            start += perPage
        }
        chunks += start until items.size
        return chunks.mapIndexed { index, range ->
            Page(index = index, pageCount = chunks.size, start = range.first, items = items.subList(range.first, range.last + 1))
        }
    }

    /** The page that contains the item at [itemIndex]. */
    public fun pageIndexOf(itemIndex: Int, totalItems: Int, maxItemsPerPage: Int, reserveMoreSlot: Boolean = true): Int {
        require(itemIndex in 0 until totalItems) { "itemIndex $itemIndex is outside 0 until $totalItems" }
        return pages(List(totalItems) { it }, maxItemsPerPage, reserveMoreSlot).first { itemIndex - it.start in it.items.indices }.index
    }

    /** The id of the "More" row that [CarList.paged] adds to page [pageIndex]. */
    public fun moreRowId(pageIndex: Int): String = "$MORE_ROW_PREFIX$pageIndex"

    /** The page that a "More" row opens, or `null` when [rowId] is not a "More" row. */
    public fun nextPageFor(rowId: String): Int? =
        rowId.removePrefix(MORE_ROW_PREFIX).takeIf { rowId.startsWith(MORE_ROW_PREFIX) }?.toIntOrNull()?.plus(1)

    private const val MORE_ROW_PREFIX = "autokit:more:"
}

/**
 * Splits the list into lists that fit [maxItems] rows each. Every list except the last ends with a
 * browsable "More" row (id from [CarPaging.moreRowId]) that should open the next list; the page
 * number is added to the titles of later pages ("Audio guides (2/3)").
 */
public fun CarList.paged(maxItems: Int = CarLimits.DEFAULT_LIST_ITEMS, moreTitle: String = "More", moreText: ((remaining: Int) -> String)? = { "$it more" }): List<CarList> {
    val pages = CarPaging.pages(rows, maxItems, reserveMoreSlot = true)
    return pages.map { page ->
        val more = if (page.hasMore) {
            val remaining = rows.size - (page.start + page.items.size)
            listOf(
                CarRow(
                    id = CarPaging.moreRowId(page.index),
                    title = moreTitle,
                    texts = listOfNotNull(moreText?.invoke(remaining)),
                    browsable = true,
                ),
            )
        } else {
            emptyList()
        }
        copy(
            title = if (page.index == 0) title else "$title (${page.index + 1}/${page.pageCount})",
            rows = page.items + more,
        )
    }
}
