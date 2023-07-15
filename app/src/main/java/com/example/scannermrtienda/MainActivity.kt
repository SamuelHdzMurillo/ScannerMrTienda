package com.example.scannermrtienda

import android.Manifest
import android.text.InputType
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Intent

import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    private var productData: HashMap<String, Int> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Solicitar permiso para acceder a la cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            startCamera()
        }

        val exportButton: Button = findViewById(R.id.export_button)
        exportButton.setOnClickListener {
            exportDataToTxtFile()
        }

        val shareButton: Button = findViewById(R.id.share_button)
        shareButton.setOnClickListener {
            shareExportedFile()
        }

        val addProductButton: Button = findViewById(R.id.add_product_button)
        addProductButton.setOnClickListener {
            addProductManually()
        }
    }

    private fun startCamera() {
        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)
        codeScanner = CodeScanner(this, scannerView)

        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            // Listener para manejar los resultados del escaneo
            setDecodeCallback { result ->
                runOnUiThread {
                    handleScanResult(result.text)
                }
            }

            // Manejar errores
            setErrorCallback { error ->
                runOnUiThread {
                    Log.e(TAG, "Camera initialization error: ${error.message}")
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Iniciar la cámara
        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    private fun handleScanResult(code: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cantidad de productos")
        builder.setMessage("Ingrese la cantidad de productos con el código $code:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setRawInputType(InputType.TYPE_CLASS_NUMBER)
        builder.setView(input)

        builder.setPositiveButton("Aceptar") { _, _ ->
            val quantityText = input.text.toString()
            if (quantityText.isNotEmpty()) {
                val quantity = quantityText.toIntOrNull()

                if (quantity != null) {
                    val currentCount = productData[code] ?: 0
                    val totalCount = currentCount + quantity
                    productData[code] = totalCount

                    Toast.makeText(this, "Productos encontrados: $totalCount", Toast.LENGTH_SHORT).show()

                    val continueBuilder = AlertDialog.Builder(this)
                    continueBuilder.setTitle("Continuar escaneando")
                    continueBuilder.setMessage("¿Desea continuar escaneando?")
                    continueBuilder.setPositiveButton("Sí") { _, _ ->
                        codeScanner.startPreview()
                    }
                    continueBuilder.setNegativeButton("No") { _, _ ->
                        // El usuario no desea continuar escaneando, hacer algo aquí si es necesario
                    }
                    continueBuilder.show()
                } else {
                    Toast.makeText(this, "Ingrese una cantidad válida", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Ingrese una cantidad válida", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
            codeScanner.startPreview()
        }

        builder.show()
    }

    private fun addProductManually() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar producto sin código")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val codeInput = EditText(this)
        codeInput.hint = "Código del producto"
        layout.addView(codeInput)

        val quantityInput = EditText(this)
        quantityInput.hint = "Cantidad"
        layout.addView(quantityInput)

        builder.setView(layout)

        builder.setPositiveButton("Agregar") { _, _ ->
            val code = codeInput.text.toString()
            val quantity = quantityInput.text.toString().toIntOrNull() ?: 0

            // Actualizar la cantidad de productos en productData
            val currentCount = productData[code] ?: 0
            val totalCount = currentCount + quantity
            productData[code] = totalCount

            // Mostrar mensaje con la cantidad de productos actualizada
            Toast.makeText(this, "Productos encontrados: $totalCount", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun exportDataToTxtFile() {
        if (productData.isNotEmpty()) {
            try {
                val fileName = "product_data.txt"
                val file = File(getExternalFilesDir(null), fileName)
                val outputStream = FileOutputStream(file)
                val writer = OutputStreamWriter(outputStream)

                for ((code, count) in productData) {
                    val line = "$code,$count"
                    writer.append(line)
                    writer.append("\n")
                }

                writer.flush()
                writer.close()
                outputStream.close()
                Toast.makeText(this, "Datos exportados a $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al exportar los datos", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareExportedFile() {
        val fileName = "product_data.txt"
        val file = File(getExternalFilesDir(null), fileName)

        if (file.exists()) {
            val fileUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooserIntent = Intent.createChooser(shareIntent, "Compartir archivo")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Agregar esta línea

            startActivity(chooserIntent)
        } else {
            Toast.makeText(this, "No se encontró el archivo exportado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Se requiere permiso para acceder a la cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST = 123
    }
}
