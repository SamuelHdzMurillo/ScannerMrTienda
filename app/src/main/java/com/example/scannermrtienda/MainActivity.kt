package com.example.scannermrtienda

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.*

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    private var productCount: Int = 0
    private var productData: StringBuilder = StringBuilder()

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
        // Contar productos con el mismo código
        // Aquí debes implementar tu lógica para contar los productos
        productCount++

        // Preguntar si desea continuar o no
        // Aquí puedes mostrar un diálogo de confirmación o utilizar cualquier otra interfaz de usuario
        // En este ejemplo, se utiliza un simple Toast para mostrar el mensaje
        Toast.makeText(this, "Productos encontrados: $productCount", Toast.LENGTH_SHORT).show()

        // Guardar datos del producto en el StringBuilder
        productData.append("Código: $code\n")

        // Si no desea continuar, exportar la información a un archivo TXT
        // Aquí debes implementar tu lógica para exportar la información
        // En este ejemplo, simplemente se muestra un log con la información
        if (!continueScanning()) {
            Log.d(TAG, "Información exportada:\nCantidad de productos: $productCount")
        }
    }

    private fun continueScanning(): Boolean {
        // Aquí debes implementar tu lógica para preguntar si desea continuar o no
        // En este ejemplo, siempre se continuará escaneando
        return true
    }

    private fun exportDataToTxtFile() {
        if (productData.isNotEmpty()) {
            try {
                val fileName = "product_data.txt"
                val file = File(getExternalFilesDir(null), fileName)
                val outputStream = FileOutputStream(file)
                val writer = OutputStreamWriter(outputStream)
                writer.append(productData.toString())
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
