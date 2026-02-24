package labs.android.lb5

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kotlin.math.roundToInt

class CompassFragment : Fragment(R.layout.fragment_compass), SensorEventListener {

    private lateinit var azimuthTextView: TextView
    private lateinit var compassImageView: ImageView

    private val viewModel: CompassViewModel by viewModels()
    private lateinit var sensorManager: SensorManager

    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        azimuthTextView = view.findViewById(R.id.tvAzimuth)
        compassImageView = view.findViewById(R.id.ivCompass)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        viewModel.azimuth.observe(viewLifecycleOwner) { azimuth ->
            val compassDirections = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = ((azimuth + 22.5f) / 45f).toInt() % 8
            val directionText = compassDirections[index]

            azimuthTextView.text = getString(R.string.azimuth_format, azimuth.roundToInt(), directionText)
            compassImageView.rotation = -azimuth
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, event.values.size)
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, event.values.size)
            hasGeomagnetic = true
        }

        if (hasGravity && hasGeomagnetic) {
            val rotationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                gravityValues,
                geomagneticValues
            )

            if (success) {
                val displayRotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    requireContext().display.rotation
                } else {
                    @Suppress("DEPRECATION")
                    requireActivity().windowManager.defaultDisplay.rotation
                }

                val remappedMatrix = FloatArray(9)
                when (displayRotation) {
                    android.view.Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remappedMatrix)
                    android.view.Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, remappedMatrix)
                    android.view.Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remappedMatrix)
                    else -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, remappedMatrix)
                }

                val orientation = FloatArray(3)
                SensorManager.getOrientation(remappedMatrix, orientation)

                var azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360f
                }

                viewModel.updateAzimuth(azimuthInDegrees)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}