package dam_a51388.hellowordl

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.Serializable
import java.util.Random
import kotlin.math.atan2

class MainActivity : AppCompatActivity() {

    private lateinit var mainImageView: ImageView
    private lateinit var canvasLayout: ConstraintLayout
    private val random = Random()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            showCaptionDialog(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mainImageView = findViewById(R.id.mainImageView)
        canvasLayout = findViewById(R.id.canvasLayout)
        val btnFabMap = findViewById<FloatingActionButton>(R.id.btnFabMap)
        val btnAddMemory = findViewById<ExtendedFloatingActionButton>(R.id.btnAddMemory)
        val switchDarkMode = findViewById<MaterialSwitch>(R.id.switchDarkMode)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        switchDarkMode.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        findViewById<MaterialButton>(R.id.btnFilterNone).setOnClickListener { applyFilter(null) }
        findViewById<MaterialButton>(R.id.btnFilterGrey).setOnClickListener { applyFilter(getGreyFilter()) }
        findViewById<MaterialButton>(R.id.btnFilterSepia).setOnClickListener { applyFilter(getSepiaFilter()) }
        findViewById<MaterialButton>(R.id.btnFilterInvert).setOnClickListener { applyFilter(getInvertFilter()) }

        btnFabMap.setOnClickListener { showMap() }
        btnAddMemory.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Restaurar fotos após recriação (ex: mudança de tema)
        if (savedInstanceState != null) {
            val restoredList = savedInstanceState.getSerializable("photo_list") as? ArrayList<PhotoState>
            restoredList?.forEach { state ->
                addPhotoToCanvas(Uri.parse(state.uriString), state.caption, state)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentStates = ArrayList<PhotoState>()
        for (i in 0 until canvasLayout.childCount) {
            val child = canvasLayout.getChildAt(i)
            if (child is MaterialCardView && child.tag is PhotoState) {
                val state = child.tag as PhotoState
                state.x = child.x
                state.y = child.y
                state.rotation = child.rotation
                state.scaleX = child.scaleX
                state.scaleY = child.scaleY
                currentStates.add(state)
            }
        }
        outState.putSerializable("photo_list", currentStates)
    }

    private fun showCaptionDialog(uri: Uri) {
        val editText = EditText(this).apply { hint = "Adiciona uma legenda..." }
        AlertDialog.Builder(this)
            .setTitle("Nova Memória")
            .setMessage("Escreve uma legenda para a tua foto.")
            .setView(editText)
            .setPositiveButton("Adicionar") { _, _ ->
                addPhotoToCanvas(uri, editText.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addPhotoToCanvas(uri: Uri, caption: String, restoredState: PhotoState? = null) {
        val state = restoredState ?: PhotoState(
            uriString = uri.toString(),
            caption = caption,
            x = (random.nextInt(300)).toFloat(),
            y = (800 + random.nextInt(300)).toFloat(),
            rotation = (random.nextInt(20) - 10).toFloat()
        )

        val card = MaterialCardView(this).apply {
            radius = 4f
            elevation = 15f
            strokeWidth = 2
            strokeColor = Color.LTGRAY
            setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.WHITE))
            layoutParams = ConstraintLayout.LayoutParams(450, ViewGroup.LayoutParams.WRAP_CONTENT)
            tag = state
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(20, 20, 20, 50)
        }

        val imageView = ImageView(this).apply {
            setImageURI(uri)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400)
        }

        val textView = TextView(this).apply {
            text = caption
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.DKGRAY)
            textSize = 16f
            setPadding(0, 24, 0, 0)
            visibility = if (caption.isEmpty()) View.GONE else View.VISIBLE
            alpha = 0.9f
        }

        container.addView(imageView)
        container.addView(textView)
        card.addView(container)

        canvasLayout.addView(card)
        
        card.post {
            card.x = state.x
            card.y = state.y
            card.rotation = state.rotation
            card.scaleX = state.scaleX
            card.scaleY = state.scaleY
        }

        setupTouchListener(card)
        findViewById<View>(R.id.hintText)?.visibility = View.GONE
    }

    data class PhotoState(
        val uriString: String,
        val caption: String,
        var x: Float = 0f,
        var y: Float = 0f,
        var rotation: Float = 0f,
        var scaleX: Float = 1f,
        var scaleY: Float = 1f
    ) : Serializable

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(view: View) {
        var dX = 0f
        var dY = 0f
        var lastRotation = 0f

        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                view.scaleX *= detector.scaleFactor
                view.scaleY *= detector.scaleFactor
                return true
            }
        })

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                showDeleteDialog(view)
            }
        })

        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    v.bringToFront()
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) lastRotation = calculateRotation(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                        v.x = event.rawX + dX
                        v.y = event.rawY + dY
                    } else if (event.pointerCount == 2) {
                        val currentRotation = calculateRotation(event)
                        v.rotation += currentRotation - lastRotation
                        lastRotation = currentRotation
                    }
                }
            }
            true
        }
    }

    private fun calculateRotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        return Math.toDegrees(atan2(deltaY, deltaX)).toFloat()
    }

    private fun showDeleteDialog(view: View) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Memória")
            .setMessage("Tens a certeza que queres remover esta foto do teu mural?")
            .setPositiveButton("Sim, eliminar") { _, _ ->
                canvasLayout.removeView(view)
                if (canvasLayout.childCount <= 4) findViewById<View>(R.id.hintText)?.visibility = View.VISIBLE
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun applyFilter(filter: ColorMatrixColorFilter?) { mainImageView.colorFilter = filter }
    private fun getGreyFilter() = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    private fun getSepiaFilter(): ColorMatrixColorFilter {
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        val sepiaMatrix = ColorMatrix().apply { setScale(1f, 0.95f, 0.82f, 1.0f) }
        matrix.postConcat(sepiaMatrix)
        return ColorMatrixColorFilter(matrix)
    }
    private fun getInvertFilter() = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f
    )))

    private fun showMap() {
        val address = "Instituto Superior de Engenharia de Lisboa, R. Conselheiro Emídio Navarro 1, 1959-007 Lisboa"
        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) startActivity(mapIntent)
        else startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
    }
}