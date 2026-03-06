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
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var mainImageView: ImageView
    private lateinit var canvasLayout: ConstraintLayout
    private val random = Random()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
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
    }

    private fun showCaptionDialog(uri: Uri) {
        val editText = EditText(this).apply {
            hint = "Adiciona uma legenda..."
        }
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
    private fun addPhotoToCanvas(uri: Uri, caption: String) {
        val card = MaterialCardView(this).apply {
            radius = 16f
            elevation = 12f
            setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.WHITE))
            layoutParams = ConstraintLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(12, 12, 12, 24)
        }

        val imageView = ImageView(this).apply {
            setImageURI(uri)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 350
            )
        }

        val textView = TextView(this).apply {
            text = caption
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.BLACK)
            textSize = 14f
            setPadding(0, 12, 0, 0)
            visibility = if (caption.isEmpty()) View.GONE else View.VISIBLE
        }

        container.addView(imageView)
        container.addView(textView)

        // Adicionar botões para Aumentar e Rodar
        val buttonPanel = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        val btnScale = MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
            text = "+"
            textSize = 14f
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                card.scaleX += 0.1f
                card.scaleY += 0.1f
            }
        }

        val btnRotate = MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
            text = "↻"
            textSize = 14f
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                card.rotation += 15f
            }
        }

        buttonPanel.addView(btnScale, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        buttonPanel.addView(btnRotate, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(buttonPanel)

        card.addView(container)

        val params = ConstraintLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            setMargins(random.nextInt(300), 800 + random.nextInt(300), 0, 0)
        }

        card.layoutParams = params
        card.rotation = (random.nextInt(30) - 15).toFloat()

        // --- Lógica de Arrastar e ELIMINAR ---
        setupTouchListener(card)

        canvasLayout.addView(card)
        findViewById<View>(R.id.hintText)?.visibility = View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(view: View) {
        var dX = 0f
        var dY = 0f

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                showDeleteDialog(view)
            }
        })

        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    v.animate().scaleX(v.scaleX * 1.05f).scaleY(v.scaleY * 1.05f).setDuration(100).start()
                    v.bringToFront()
                }
                MotionEvent.ACTION_MOVE -> {
                    v.x = event.rawX + dX
                    v.y = event.rawY + dY
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(v.scaleX / 1.05f).scaleY(v.scaleY / 1.05f).setDuration(100).start()
                }
            }
            true
        }
    }

    private fun showDeleteDialog(view: View) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Memória")
            .setMessage("Tens a certeza que queres remover esta foto do teu mural?")
            .setPositiveButton("Sim, eliminar") { _, _ ->
                canvasLayout.removeView(view)
                // Se não houver mais fotos, mostrar o texto de ajuda novamente
                if (canvasLayout.childCount <= 4) { // Assumindo que os outros elementos são estáticos
                     findViewById<View>(R.id.hintText)?.visibility = View.VISIBLE
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun applyFilter(filter: ColorMatrixColorFilter?) {
        mainImageView.colorFilter = filter
    }

    private fun getGreyFilter() = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

    private fun getSepiaFilter(): ColorMatrixColorFilter {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val sepiaMatrix = ColorMatrix()
        sepiaMatrix.setScale(1f, 0.95f, 0.82f, 1.0f)
        matrix.postConcat(sepiaMatrix)
        return ColorMatrixColorFilter(matrix)
    }

    private fun getInvertFilter(): ColorMatrixColorFilter {
        val matrix = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f,
            0f, -1.0f, 0f, 0f, 255f,
            0f, 0f, -1.0f, 0f, 255f,
            0f, 0f, 0f, 1.0f, 0f
        )
        return ColorMatrixColorFilter(ColorMatrix(matrix))
    }

    private fun showMap() {
        val gmmIntentUri = Uri.parse("geo:38.7567,-9.1171?q=38.7567,-9.1171(ISEL)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
        }
    }
}