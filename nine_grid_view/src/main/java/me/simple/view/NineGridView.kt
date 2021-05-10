package me.simple.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import kotlin.math.ceil

open class NineGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var spanCount = 3

    var maxCount = 9

    var childMargin = 0

    var adapter: Adapter? = null
        set(value) {
            field = value
            addViews()
        }

    open fun dp2px(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NineGridView)
            spanCount = typedArray.getInt(R.styleable.NineGridView_spanCount, spanCount)
            maxCount = typedArray.getInt(R.styleable.NineGridView_maxCount, maxCount)
            childMargin =
                typedArray.getDimension(R.styleable.NineGridView_childMargin, dp2px(1f)).toInt()
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (adapter == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val adapter = adapter!!
        //
        if (adapter.adaptSingleView() && adapter.getItemCount() == 1) {
            measureChildren(widthMeasureSpec, heightMeasureSpec)
            val adaptHeightMeasureSpec =
                MeasureSpec.makeMeasureSpec(getChildAt(0).measuredHeight, MeasureSpec.EXACTLY)
            super.onMeasure(widthMeasureSpec, adaptHeightMeasureSpec)
            return
        }

        //
        val lineCount = getLineCount()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = width / spanCount * lineCount

        val itemWidth = (width - (childMargin * (spanCount - 1))) / spanCount
        val childMeasureSpec = MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY)
        measureChildren(childMeasureSpec, childMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (adapter == null) return
        layoutChildren()
    }

    private fun layoutChildren() {

        var left = 0
        var top = 0
        var right = 0
        var bottom = 0

        val adapter = adapter!!

        //单itemView并且要适配的情况
        if (adapter.adaptSingleView() && childCount == 1) {
            val singleView = getChildAt(0)
            singleView.layout(0, 0, singleView.measuredWidth, measuredHeight)
            adapter.onBindSingleView(singleView, 0)
            return
        }

        //默认itemView的情况
        val displayCount = getDisplayCount()
        for (i in 0 until displayCount) {
            val child = getChildAt(i)
            right = left + child.measuredWidth
            bottom = top + child.measuredWidth

            child.layout(left, top, right, bottom)
            adapter.onBindItemView(child, i)

            if ((i + 1) % spanCount == 0) {//
                left = 0
                top = bottom + childMargin
            } else {
                left = right + childMargin
            }
        }

        //需要添加额外itemView的情况
        if (adapter.enableExtraView() && adapter.getItemCount() > maxCount) {
            val extraView = getChildAt(childCount - 1)
            right = width
            left = right - extraView.measuredWidth
            bottom = height
            top = bottom - extraView.measuredWidth
            extraView.layout(left, top, right, bottom)
            adapter.onBindExtraView(extraView, childCount - 1)
        }

    }

    private fun addViews() {
        removeAllViewsInLayout()

        if (adapter == null) {
            requestLayout()
            return
        }

        val adapter = adapter!!
        val displayCount = getDisplayCount()
        var itemViewType = adapter.getItemViewType(0)

        //要适配单个View的情况
        val singleView = adapter.onCreateSingleView(this, itemViewType)
        if (adapter.adaptSingleView() && singleView != null && adapter.getItemCount() == 1) {
            addViewInLayout(singleView, 0, singleView.layoutParams, true)
            requestLayout()
            return
        }

        //默认itemView的情况
        for (position in 0 until displayCount) {
            itemViewType = adapter.getItemViewType(position)
            val itemView = adapter.onCreateItemView(this, itemViewType)
            addViewInLayout(itemView, position, createItemViewLayoutParams(), true)
        }

        //添加额外的itemView
        itemViewType = adapter.getItemViewType(displayCount)
        val extraView = adapter.onCreateExtraView(this, itemViewType)
        if (adapter.enableExtraView() && extraView != null && adapter.getItemCount() > maxCount) {
            addViewInLayout(extraView, displayCount, createItemViewLayoutParams(), true)
        }

        //
        requestLayout()
    }

    /**
     * 创建itemView的LayoutParams
     */
    private fun createItemViewLayoutParams() =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    /**
     * 真实要显示的itemCount
     */
    private fun getDisplayCount(): Int {
        if (adapter == null) return 0
        return if (adapter!!.getItemCount() > maxCount) maxCount else adapter!!.getItemCount()
    }

    /**
     * 获取行数
     */
    private fun getLineCount(): Int {
        return ceil(getDisplayCount().toDouble() / spanCount).toInt()
    }

    abstract class Adapter {

        abstract fun getItemCount(): Int

        fun getItemViewType(position: Int): Int {
            return 0
        }

        //-------默认的ItemView

        abstract fun onCreateItemView(parent: ViewGroup, viewType: Int): View

        abstract fun onBindItemView(itemView: View, position: Int)

        //-------单个View适配

        open fun adaptSingleView(): Boolean = false

        open fun onCreateSingleView(parent: ViewGroup, viewType: Int): View? = null

        open fun onBindSingleView(singleView: View, position: Int) {

        }

        //-------额外的View

        open fun enableExtraView(): Boolean = false

        open fun onCreateExtraView(parent: ViewGroup, viewType: Int): View? = null

        open fun onBindExtraView(extraView: View, position: Int) {

        }
    }
}