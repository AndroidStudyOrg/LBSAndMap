package org.shop.lbsandmap

import android.Manifest
import org.shop.lbsandmap.R
import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource
import org.shop.lbsandmap.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding

    private lateinit var locationSource: FusedLocationSource
    private lateinit var nMap: NaverMap
    private lateinit var adapter: RVLocationAdapter
    private val locationList = ArrayList<Location>()

    /**
     * Fused Location Provider Api 에서
     * 위치 업데이트를위한 서비스 품질등 다양한요청을
     * 설정하는데 사용하는 객체.
     */
    private lateinit var mLocationRequest: LocationRequest

    /**
     * 현재 위치정보를 나타내는 객체
     */
    private lateinit var mCurrentLocation: Location

    /**
     * 현재 위치제공자(Provider)와 상호작용하는 진입점
     */
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    /**
     * 현재 단말기에 설정된 위치 Provider
     */
    private lateinit var currentProvider: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        currentProvider = intent.getStringExtra("provider")!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkMyPermissionLocation()
        } else {
            initGoogleMapLocation()
        }

        adapter = RVLocationAdapter(locationList)
        binding.rvLocation.adapter = adapter
    }

    private val permissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            initGoogleMapLocation()
        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            Toast.makeText(applicationContext, "위치제공 허락을 해야 앱이 정상적으로 작동합니다", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    private fun checkMyPermissionLocation() {
        TedPermission.create()
            .setPermissionListener(permissionListener)
            .setRationaleMessage("지도를 사용하기 위해서는 위치제공 허락이 필요합니다")
            .setPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).check()
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        /**
         *  성공적으로 위치정보와 넘어왔을때를 동작하는 Call back 함수
         */
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            mCurrentLocation = locationResult.locations[0]
            locationList.add(mCurrentLocation)
            adapter.notifyDataSetChanged()
        }

        /**
         * 현재 콜백이 동작가능한지에 대한 여부
         */
        override fun onLocationAvailability(availability: LocationAvailability) {
            val message = if (availability.isLocationAvailable) {
                "위치 정보 획득이 가능합니다"
            } else {
                "현재 위치 정보를 가져올 수 없네요! 잠시 후 다시 시도하세요"
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun initGoogleMapLocation() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_naver_map) as MapFragment
        /**
         * 비동기 방식으로 GoogleMap 초기설정을 진행한다
         */
        mapFragment.getMapAsync { googleMap: NaverMap ->
            nMap = googleMap
            nMap.uiSettings.isZoomControlEnabled = true
            nMap.uiSettings.isLocationButtonEnabled = true

            /**
             * FusedLocationProviderApi 에서
             * 위치 업데이트를위한 서비스 품질등 다양한요청을
             * 설정하는데 사용하는 데이터객체인
             * LocationRequest 를 획득
             */
            val locationIntervalTime = 1000L
            val priorityType =
                if (currentProvider.equals(LocationManager.GPS_PROVIDER, ignoreCase = true)) {
                    Priority.PRIORITY_HIGH_ACCURACY
                } else {
                    //배터리와 정확도의 밸런스를 고려하여 위치정보를 획득(정확도 다소 높음)
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                }
            mLocationRequest = LocationRequest.Builder(priorityType, locationIntervalTime)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(locationIntervalTime)
                .setIntervalMillis(3000.toLong())       //위치가 update 되는 주기
                .setMaxUpdateDelayMillis(locationIntervalTime)
                .build()

            /**
             * 위치서비스 설정 정보를 저장하기 위한 빌더객체획득
             */
            val builder = LocationSettingsRequest.Builder().apply {
                /**
                 * 현재 위치정보 Setting 정보가 저장된 LocationRequest
                 * 객체를 등록
                 */
                addLocationRequest(mLocationRequest)
            }

            /**
             * 위치정보 요청을 수행하기 위해 단말기에서
             * 관련 시스템 설정(Gps,Network)이 활성화되었는지 확인하는 클래스인
             * SettingClient 를 획득한다
             */
            val mSettingsClient = LocationServices.getSettingsClient(this)

            /**
             * 위치 서비스 유형을 저장하고
             * 위치 설정에도 사용하기위해
             * LocationSettingsRequest 객체를 획득
             */
            val mLocationSettingsRequest = builder.build()
            val locationResponse = mSettingsClient.checkLocationSettings(mLocationSettingsRequest)

            /**
             * 현재 위치제공자(Provider)와 상호작용하는 진입점인
             * FusedLocationProviderClient 객체를 획득
             */
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            /**
             * 정상적으로 위치정보가 설정되었다면
             * 위치업데이트를 요구하고, 설정이 잘못되었다면
             * onFailure 를 처리한다
             */
            with(locationResponse) {
                addOnSuccessListener {
                    Toast.makeText(applicationContext, "위치 받기 성공", Toast.LENGTH_SHORT).show()
                    mFusedLocationClient.requestLocationUpdates(
                        mLocationRequest,
                        mLocationCallback,
                        Looper.getMainLooper()
                    )
                }
                addOnFailureListener { e ->
                    val exception = e as ApiException
                    Toast.makeText(applicationContext, "위치 받기 실패: $exception", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    /**
     * 현재 화면을 나갈때 반드시 등록된
     * 위치정보 알림을 제거
     */
    override fun onStop() {
        super.onStop()
        if (this::mFusedLocationClient.isInitialized) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }

    override fun onMapReady(p0: NaverMap) {

    }
}