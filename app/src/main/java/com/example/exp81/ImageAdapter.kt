package com.example.exp81

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class ImageAdapter(
    private val context: Context,
    private val items: List<ImageItem>
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.grid_item, parent, false)

        val img = row.findViewById<ImageView>(R.id.imgItem)
        val label = row.findViewById<TextView>(R.id.txtLabel)
        val check = row.findViewById<CheckBox>(R.id.chkSelect)

        val item = items[position]
        label.text = item.label
        check.isChecked = item.selected
        row.setBackgroundColor(if (item.selected) 0x334CAF50 else 0x00000000)

        when (item.type) {
            ImageItem.Type.DRAWABLE ->
                Glide.with(context).load(item.drawableResId).centerCrop().into(img)

            ImageItem.Type.PHONE_STORAGE -> {
                val uriString = item.storageUri
                if (uriString.isNullOrEmpty()) {
                    // Not loaded yet (permission pending or no photos found) - show a placeholder
                    Glide.with(context).load(android.R.drawable.ic_menu_report_image).centerCrop().into(img)
                } else {
                    Glide.with(context).load(Uri.parse(uriString)).centerCrop().into(img)
                }
            }

            ImageItem.Type.URL ->
                Glide.with(context).load(item.url).centerCrop().into(img)
        }

        return row
    }
}