package labs.android.lb5

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CompassViewModel : ViewModel() {

    private val _azimuth = MutableLiveData<Float>()
    val azimuth: LiveData<Float> get() = _azimuth

    fun updateAzimuth(newAzimuth: Float) {
        _azimuth.value = newAzimuth
    }
}