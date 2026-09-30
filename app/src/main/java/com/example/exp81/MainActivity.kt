package com.example.exp81

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val reqStoragePermission = 100

    private lateinit var gridView: GridView
    private lateinit var tvStatus: TextView
    private lateinit var adapter: ImageAdapter
    private val imageItems = mutableListOf<ImageItem>()

    // Position that was long-pressed, used by the context menu callbacks
    private var contextMenuPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        gridView = findViewById(R.id.gridView)
        tvStatus = findViewById(R.id.tvStatus)

        buildDrawableAndUrlItems()
        adapter = ImageAdapter(this, imageItems)
        gridView.adapter = adapter

        // ---- Tap = toggle selection (used for multi-select -> Vertical Bottom Menu) ----
        gridView.setOnItemClickListener { _, _, position, _ ->
            val item = imageItems[position]
            item.selected = !item.selected
            updateSelectionState()
            val selectedCount = countSelected()
            tvStatus.text = "$selectedCount image(s) selected"
        }

        // Vertical popup menu button clicks (stays at bottom vertically until unselected)
        findViewById<View>(R.id.btnPopShare).setOnClickListener {
            Toast.makeText(this, "Sharing ${countSelected()} image(s)", Toast.LENGTH_SHORT).show()
            clearSelection()
        }
        findViewById<View>(R.id.btnPopDelete).setOnClickListener {
            Toast.makeText(this, "Deleted ${countSelected()} image(s) (demo)", Toast.LENGTH_SHORT).show()
            clearSelection()
        }
        findViewById<View>(R.id.btnPopCancel).setOnClickListener {
            clearSelection()
        }

        // ---- Long press = Context Menu (only meaningful for URL images per the spec) ----
        registerForContextMenu(gridView)

        // Load the 3 phone-storage images (needs runtime permission on Android 6+)
        requestStoragePermissionAndLoad()

        // Bottom buttons
        findViewById<View>(R.id.btnGrid).setOnClickListener {
            Toast.makeText(this, "Already showing the grid", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnWebView).setOnClickListener { openWebView() }
    }

    /** Adds the 3 drawable-based items and the 3 URL-based items to the list (3+3+3 layout). */
    private fun buildDrawableAndUrlItems() {
        // 3 images bundled with the app (replace these system icons with your own
        // res/drawable images, e.g. R.drawable.img1, img2, img3)
        imageItems.add(
            ImageItem(ImageItem.Type.DRAWABLE, "Drawable 1", drawableResId = R.drawable.img1)
        )
        imageItems.add(
            ImageItem(ImageItem.Type.DRAWABLE, "Drawable 2", drawableResId = R.drawable.img2)
        )
        imageItems.add(
            ImageItem(ImageItem.Type.DRAWABLE, "Drawable 3", drawableResId = R.drawable.img3)
        )

        // Placeholders for the 3 phone-storage slots; filled once the MediaStore query returns
        imageItems.add(ImageItem(ImageItem.Type.PHONE_STORAGE, "Storage 1"))
        imageItems.add(ImageItem(ImageItem.Type.PHONE_STORAGE, "Storage 2"))
        imageItems.add(ImageItem(ImageItem.Type.PHONE_STORAGE, "Storage 3"))

        // 3 images loaded from the internet via Glide
        imageItems.add(ImageItem(ImageItem.Type.URL, "URL 1", url = "https://picsum.photos/id/1015/300/300"))
        imageItems.add(ImageItem(ImageItem.Type.URL, "URL 2", url = "https://picsum.photos/id/1025/300/300"))
        imageItems.add(ImageItem(ImageItem.Type.URL, "URL 3", url = "https://picsum.photos/id/1035/300/300"))
    }

    // ------------------------------------------------------------------
    // Phone storage images (MediaStore) with runtime permission handling
    // ------------------------------------------------------------------
    private fun requestStoragePermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadPhoneStorageImages()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), reqStoragePermission)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == reqStoragePermission &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadPhoneStorageImages()
        } else {
            Toast.makeText(this, "Storage permission denied, keeping placeholders", Toast.LENGTH_SHORT).show()
        }
    }

    /** Queries the last 3 images from the device gallery and swaps them into the PHONE_STORAGE slots. */
    private fun loadPhoneStorageImages() {
        val uris = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext() && uris.size < 3) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    uris.add(uri.toString())
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not read gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Storage items sit at index 3,4,5 (after the 3 drawable items)
        uris.forEachIndexed { i, uriString ->
            imageItems[3 + i].storageUri = uriString
        }
        adapter.notifyDataSetChanged()
    }

    // ------------------------------------------------------------------
    // Options Menu (Toolbar / overflow) -> Home, Select All, Show, Exit
    // ------------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home -> {
                clearSelection()
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_select_all -> {
                imageItems.forEach { it.selected = true }
                updateSelectionState()
                tvStatus.text = "All ${imageItems.size} images selected"
                true
            }
            R.id.menu_show -> {
                Toast.makeText(this, "${countSelected()} image(s) currently selected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_exit -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ------------------------------------------------------------------
    // Vertical Popup Menu state updater -> shown at bottom vertically when
    // multiple images are selected, stays there until items are unselected
    // ------------------------------------------------------------------
    private fun updateSelectionState() {
        adapter.notifyDataSetChanged()
        val selectedCount = countSelected()
        val verticalMenu = findViewById<View>(R.id.layoutVerticalMenu)
        verticalMenu.visibility = if (selectedCount > 1) View.VISIBLE else View.GONE
    }

    // ------------------------------------------------------------------
    // Context Menu -> long press, intended for the URL images (index 6,7,8)
    // ------------------------------------------------------------------
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        contextMenuPosition = info.position
        val item = imageItems[contextMenuPosition]

        menuInflater.inflate(R.menu.context_menu, menu)
        menu.setHeaderTitle(item.label)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (contextMenuPosition == -1) return super.onContextItemSelected(item)
        val target = imageItems[contextMenuPosition]

        return when (item.itemId) {
            R.id.ctx_edit -> {
                Toast.makeText(this, "Edit: ${target.label}", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.ctx_save -> {
                Toast.makeText(this, "Save: ${target.label}", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.ctx_delete -> {
                imageItems.removeAt(contextMenuPosition)
                updateSelectionState()
                Toast.makeText(this, "Deleted: ${target.label}", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // ------------------------------------------------------------------
    // WebView launch (bottom-right icon)
    // ------------------------------------------------------------------
    private fun openWebView() {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.EXTRA_URL, "https://developer.android.com")
        startActivity(intent)
    }

    private fun countSelected(): Int = imageItems.count { it.selected }

    private fun clearSelection() {
        imageItems.forEach { it.selected = false }
        updateSelectionState()
        tvStatus.text = "Selection cleared"
    }
}