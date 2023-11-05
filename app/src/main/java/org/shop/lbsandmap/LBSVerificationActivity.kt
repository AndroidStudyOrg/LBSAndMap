package org.shop.lbsandmap

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task

class LBSVerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isNetworkAvailable()) {
            checkLocationCurrentDevice()
        } else {
            Log.e(LBS_CHECK_TAG, "네트웍연결되지 않음!")
            Toast.makeText(
                applicationContext,
                "네트웍이 연결되지 않아 종료합니다", Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = cm.activeNetwork ?: return false
            val networkCapabilities = cm.getNetworkCapabilities(nw) ?: return false
            return when {
                //현재 단말기의 연결유무(Wifi, Data 통신)
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                //단말기가 아닐경우(ex:: IoT 장비등)
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //블루투스 인터넷 연결유뮤
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            return cm.activeNetworkInfo?.isConnected ?: false
        }
    }

    // 현재 코드를 알아내는 정보
    private fun checkLocationCurrentDevice() {
        val locationIntervalTime = 60000L
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationIntervalTime)
                /**
                 * 정확한 위치를 기다림: true 일시 지하, 이동 중일 경우 늦어질 수 있음
                 */
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(locationIntervalTime) //위치 획득 후 update 되는 최소 주기
                .setMaxUpdateDelayMillis(locationIntervalTime).build() //위치 획득 후 update delay 최대 주기


        val lbsSettingsRequest: LocationSettingsRequest =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        val settingClient: SettingsClient = LocationServices.getSettingsClient(this)
        val taskLBSSettingResponse: Task<LocationSettingsResponse> =
            settingClient.checkLocationSettings(lbsSettingsRequest)
        /**
         * 위치 정보설정이 On 일 경우
         */
        taskLBSSettingResponse.addOnSuccessListener {
            with(Intent(this, MainActivity::class.java)) {
                putExtra("provider", LocationManager.GPS_PROVIDER)
                startActivity(this)
            }
            finish()
        }
        /**
         * 위치 정보설정이 OFF 일 경우
         */
        taskLBSSettingResponse.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    Toast.makeText(
                        applicationContext,
                        "위치정보 설정은 반드시 On 상태여야 해요!",
                        Toast.LENGTH_SHORT
                    ).show()
                    /**
                     * 위치 설정이 되어있지 않을 시 대응방안을 정의
                     * 여기선 onActivityResult 를 이용해 대응한다
                     */
                    exception.startResolutionForResult(
                        this@LBSVerificationActivity,
                        LBS_CHECK_CODE
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(LBS_CHECK_TAG, sendEx.message.toString())
                }
            }
        }
    }

    // Launcher로 바뀜. 비동기 방식으로...
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == LBS_CHECK_CODE) {
            checkLocationCurrentDevice()
        }
    }
}