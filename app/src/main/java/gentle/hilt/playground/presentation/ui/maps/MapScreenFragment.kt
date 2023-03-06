package gentle.hilt.playground.presentation.ui.maps

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.mapkit.transport.masstransit.Session.RouteListener
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R
import gentle.hilt.playground.databinding.FragmentMapScreenBinding
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.abs

@AndroidEntryPoint
class MapScreenFragment :
    Fragment(),
    Session.SearchListener,
    SuggestSession.SuggestListener,
    CameraListener,
    DrivingSession.DrivingRouteListener,
    RouteListener,
    UserLocationObjectListener,
    InputListener,
    GeoObjectTapListener {

    private lateinit var binding: FragmentMapScreenBinding

    private val viewModel: MapScreenViewModel by viewModels()

    private lateinit var searchManager: SearchManager
    private lateinit var searchSession: Session
    private lateinit var resultAdapter: ArrayAdapter<*>
    private val suggestResult: MutableList<String> = ArrayList()

    private var firstPointName: String = ""
    private var firstPointLongitude: Double = 0.0
    private var firstPointLatitude: Double = 0.0

    private var secondPointName: String = ""
    private var secondPointLongitude: Double = 0.0
    private var secondPointLatitude: Double = 0.0

    private var userCameraPositionLatitude: Double = 0.0
    private var userCameraPositionLongitude: Double = 0.0

    private var userPreloadLocationChoice: Int = 0
    private var userRouteTypeChoice: Int = 0
    private var zoomChange: Float = 14f

    private var userTracking: Boolean = false
    private var locationPermissionIsAllowed: Boolean = false

    private lateinit var drivingRouter: DrivingRouter
    private lateinit var pedestrianRouter: PedestrianRouter
    private lateinit var busRouter: MasstransitRouter
    private lateinit var drivingSession: DrivingSession
    private val drivingOptions = DrivingOptions()
    private val busOptions = TransitOptions(FilterVehicleTypes.BUS.value, TimeOptions())
    private val vehicleOptions = VehicleOptions()

    private val requestPoints: ArrayList<RequestPoint> = ArrayList()

    private lateinit var userLocationLayer: UserLocationLayer

    private val center = Point(DEFAULT_CENTER_POSITION_LATITUDE, DEFAULT_CENTER_POSITION_LONGITUDE)
    private val boxSize = BOX_SIZE
    private val boundingBox = BoundingBox(
        Point(center.latitude - boxSize, center.longitude - boxSize),
        Point(center.latitude + boxSize, center.longitude + boxSize)
    )
    private val searchTypes = SuggestOptions().setSuggestTypes(
        SearchType.GEO.value or SearchType.BIZ.value or SearchType.NONE.value
    )

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                refreshFragment()
                Timber.tag("permissions").e("Success, granted")
            } else {
                Timber.tag("permissions").e("Fail to get permission")
            }
        }

    private fun submitQuery(query: String) {
        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(binding.mapKit.map.visibleRegion),
            SearchOptions(),
            this
        )
    }

    companion object {
        const val START = 1
        const val DESTINATION = 2
        const val PREVIOUS_CAMERA_POSITION = 3
        const val DO_NOT_SAVE_LOCATIONS = 4

        const val BUS = 1
        const val CAR = 2
        const val WALK = 3

        const val MAX_ZOOM_CHANGE = 21f
        const val MIN_ZOOM_CHANGE = 0f

        const val WAIT_10_SECONDS = 10000L
        const val WAIT_3_SECONDS = 3000L

        const val DEFAULT_CENTER_POSITION_LONGITUDE = 37.62
        const val DEFAULT_CENTER_POSITION_LATITUDE = 55.75
        const val BOX_SIZE = 0.2

        const val DEFAULT_ANCHOR_VALUE_083 = 0.83
        const val DEFAULT_ANCHOR_VALUE_05 = 0.5

        const val DEFAULT_ARROW_SIZE = 0.5f
        const val ZERO = 0.0
    }

    private fun resizeMapInLandscapeMode() {
        val widthAndHeightMapOptions = requireContext().resources
            .getDimensionPixelSize(R.dimen.map_options_in_landscape_mode)
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val constrainLayout: ConstraintLayout = binding.mapScreenCl
            val constrainSet = ConstraintSet()
            constrainSet.clone(constrainLayout)
            constrainSet.connect(
                R.id.tilFirstPoint, ConstraintSet.END,
                R.id.verticalHelper, ConstraintSet.END
            )
            constrainSet.connect(
                R.id.tilSecondPoint, ConstraintSet.END,
                R.id.verticalHelper, ConstraintSet.END
            )
            constrainSet.applyTo(constrainLayout)

            val paramsForMapOptions = ConstraintLayout.LayoutParams(
                widthAndHeightMapOptions,
                widthAndHeightMapOptions
            )
            val mapOptionsEndMargin = requireContext()
                .resources.getDimensionPixelSize(R.dimen.margin_end_map_option_button_in_landscape_mode)
            val mapOptionsTopMargin = requireContext()
                .resources.getDimensionPixelSize(R.dimen.margin_top_map_option_button_in_landscape_mode)

            paramsForMapOptions.marginEnd = mapOptionsEndMargin
            paramsForMapOptions.topMargin = mapOptionsTopMargin
            paramsForMapOptions.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            paramsForMapOptions.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

            binding.apply {
                searchSecondPoint.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                searchFirstPoint.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                searchFirstPoint.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
                searchSecondPoint.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN

                mapOptions.layoutParams = paramsForMapOptions
            }
        }
    }

    private fun requestLocationPermissions() {
        val checkSelfPermission =
            ContextCompat.checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION)
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            createUserOnMap()
        } else {
            requestPermission.launch(ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SearchFactory.initialize(requireContext())
        MapKitFactory.initialize(requireContext())
        TransportFactory.initialize(requireContext())

        resizeMapInLandscapeMode()
        mapDarkMode()

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
        pedestrianRouter = TransportFactory.getInstance().createPedestrianRouter()
        busRouter = TransportFactory.getInstance().createMasstransitRouter()
        binding.mapKit.map.addCameraListener(this)
        binding.mapKit.map.addTapListener(this)
        binding.mapKit.map.addInputListener(this)

        requestLocationPermissions()

        preloadUserCameraCoordinates()
        preloadFirstPointCoordinates()
        preloadSecondPointCoordinates()

        observeUser()
        observeRoutes()

        suggestAdapter(binding.lvSuggestResult)
        userRouteMenu(binding.routeOptions)
        userSettingsMenu(binding.mapOptions)
        routeImplementation(binding.ibMakeRoute)
        suggestImplementation(binding.lvSuggestResult)
        searchImplementation(binding.searchFirstPoint, binding.searchSecondPoint)
        zoomImplementation(binding.zoomIn, binding.zoomOut)
        userTrackingImplementation(binding.turnUserTracking)
    }

    private fun observeUser() {
        viewModel.readUserZoomChange.observe(viewLifecycleOwner) { userZoomChoice ->
            if (zoomChange != userZoomChoice) {
                moveCameraToSearchedPoint(binding.mapKit.map.cameraPosition.target, userZoomChoice)
            }
            zoomChange = userZoomChoice
        }

        viewModel.readRouteType.observe(viewLifecycleOwner) { userPreloadRouteType ->
            if (userRouteTypeChoice != userPreloadRouteType) {
                userRouteTypeChoice = userPreloadRouteType
                recreateRoute()
            }

            when (userPreloadRouteType) {
                BUS -> {
                    binding.routeOptions.setImageResource(
                        R.drawable.ic_baseline_directions_bus_filled_24
                    )
                }
                CAR -> {
                    binding.routeOptions.setImageResource(R.drawable.ic_baseline_directions_car_24)
                }
                WALK -> {
                    binding.routeOptions.setImageResource(R.drawable.ic_baseline_directions_walk_24)
                }
            }
        }

        viewModel.readUserPreloadLocationChoice.observe(viewLifecycleOwner) { userPreloadChoice ->
            if (userPreloadLocationChoice != userPreloadChoice) {
                setUserPreloadingChoice(userPreloadChoice)
            }
            userPreloadLocationChoice = userPreloadChoice
        }
        viewModel.readUserTrackingChoice.observe(viewLifecycleOwner) { userTrackingChoice ->
            userTracking = userTrackingChoice
            viewsBehaviorWhileTrackingUser()
            userTrackingEnabled()
        }
        saveUserCameraCoordinates()
    }

    private fun observeRoutes() {
        viewModel.readFirstPoint.observe(viewLifecycleOwner) { FirstPoint ->
            if (FirstPoint != firstPointName &&
                userPreloadLocationChoice != DO_NOT_SAVE_LOCATIONS
            ) {
                firstPointName = FirstPoint
                binding.searchFirstPoint.setText(FirstPoint)
                binding.lvSuggestResult.visibility = View.INVISIBLE
                if (binding.searchFirstPoint.isFocused) {
                    submitQuery(FirstPoint)
                }
            }
        }
        viewModel.readSecondPoint.observe(viewLifecycleOwner) { SecondPoint ->
            if (SecondPoint != secondPointName &&
                userPreloadLocationChoice != DO_NOT_SAVE_LOCATIONS
            ) {
                secondPointName = SecondPoint
                binding.searchSecondPoint.setText(SecondPoint)
                binding.lvSuggestResult.visibility = View.INVISIBLE
                if (binding.searchSecondPoint.isFocused) {
                    submitQuery(SecondPoint)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun suggestAdapter(suggestResultView: ListView) {
        resultAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_2,
            android.R.id.text1,
            suggestResult
        )
        suggestResultView.adapter = resultAdapter

        suggestResultView.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }
    }

    private fun userTrackingImplementation(turnUserTracking: ImageView) {
        turnUserTracking.setOnClickListener {
            if (locationPermissionIsAllowed) {
                when (userTracking) {
                    true -> {
                        viewModel.saveUserTrackingChoice(false)
                    }
                    false -> {
                        viewModel.saveUserTrackingChoice(true)
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Allow location permissions to track user",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun routeImplementation(moveTo: ImageButton) {
        moveTo.setOnClickListener {
            val first = (firstPointLatitude + firstPointLongitude)
            val second = (secondPointLatitude + secondPointLongitude)
            if (abs(first + second) != ZERO) {
                clearSearchFocus()
                showBothMarkersFromPreviousLocations()
                drivingRouteRequest(userRouteTypeChoice)
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.error_make_route,
                    Toast.LENGTH_LONG
                ).show()
            }

            if (userPreloadLocationChoice == DO_NOT_SAVE_LOCATIONS) {
                Toast.makeText(
                    requireContext(),
                    R.string.error_make_route,
                    Toast.LENGTH_LONG
                ).show()
                refreshFragment()
            }
        }
    }

    private fun zoomImplementation(zoomIn: ImageButton, zoomOut: ImageButton) {
        zoomIn.setOnClickListener {
            viewModel.saveUserTrackingChoice(false)
            if (zoomChange < MAX_ZOOM_CHANGE) {
                viewModel.saveUserZoomChange(zoomChange.inc())
            }
            clearSearchFocus()
        }
        zoomOut.setOnClickListener {
            viewModel.saveUserTrackingChoice(false)
            if (zoomChange != MIN_ZOOM_CHANGE) {
                viewModel.saveUserZoomChange(zoomChange.dec())
            }
            clearSearchFocus()
        }
    }

    private fun suggestImplementation(lvSuggestResult: ListView) {
        lvSuggestResult.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                selectionInSuggestionImplementation(
                    position,
                    firstPointText = binding.searchFirstPoint.text.toString(),
                    secondPointText = binding.searchSecondPoint.text.toString()
                )
            }
    }

    private fun searchImplementation(searchFirstPoint: EditText, searchSecondPoint: EditText) {
        searchFirstPoint.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(editable: Editable?) {
                requestSuggest(editable.toString())
            }
        })

        searchSecondPoint.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(editable: Editable?) {
                requestSuggest(editable.toString())
            }
        })
    }

    private fun userRouteMenu(routeOptions: ImageView) {
        routeOptions.setOnClickListener {
            clearSearchFocus()
            val routeMenu = PopupMenu(requireContext(), routeOptions)
            routeMenu.menuInflater.inflate(R.menu.route_type_menu, routeMenu.menu)

            binding.mapKit.map.mapObjects.clear()
            requestPoints.clear()

            when (userRouteTypeChoice) {
                BUS -> {
                    routeMenu.menu.findItem(R.id.Bus).isChecked = true
                }
                CAR -> {
                    routeMenu.menu.findItem(R.id.Car).isChecked = true
                }
                WALK -> {
                    routeMenu.menu.findItem(R.id.Walk).isChecked = true
                }
            }

            routeMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.Bus -> {
                        viewModel.saveRouteType(BUS)
                    }
                    R.id.Car -> {
                        viewModel.saveRouteType(CAR)
                    }
                    R.id.Walk -> {
                        viewModel.saveRouteType(WALK)
                    }
                }
                true
            }
            showBothMarkersFromPreviousLocations()
            routeMenu.show()
        }
    }

    private fun userSettingsMenu(mapOptions: ImageView) {
        mapOptions.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), mapOptions)
            popupMenu.menuInflater.inflate(R.menu.map_settings_menu, popupMenu.menu)

            when (userPreloadLocationChoice) {
                START -> {
                    popupMenu.menu.findItem(R.id.firstPointTracking).isChecked = true
                }
                DESTINATION -> {
                    popupMenu.menu.findItem(R.id.secondPointTracking).isChecked = true
                }

                PREVIOUS_CAMERA_POSITION -> {
                    popupMenu.menu.findItem(R.id.userCameraPosition).isChecked = true
                }

                DO_NOT_SAVE_LOCATIONS -> {
                    popupMenu.menu.findItem(R.id.noPointTracking).isChecked = true
                }
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.firstPointTracking -> {
                        binding.searchFirstPoint.requestFocus()
                        viewModel.savePreviouslyChosenLocation(START)
                    }
                    R.id.secondPointTracking -> {
                        binding.searchSecondPoint.requestFocus()
                        viewModel.savePreviouslyChosenLocation(DESTINATION)
                    }
                    R.id.userCameraPosition -> {
                        clearSearchFocus()
                        viewModel.savePreviouslyChosenLocation(PREVIOUS_CAMERA_POSITION)
                    }

                    R.id.noPointTracking -> {
                        viewModel.savePreviouslyChosenLocation(DO_NOT_SAVE_LOCATIONS)
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun drivingRouteRequest(userChoice: Int) {
        val start = Point(firstPointLatitude, firstPointLongitude)
        val destination = Point(secondPointLatitude, secondPointLongitude)
        requestPoints.add(RequestPoint(start, RequestPointType.WAYPOINT, null))
        requestPoints.add(RequestPoint(destination, RequestPointType.WAYPOINT, null))

        when (userChoice) {
            BUS -> {
                busRouter.requestRoutes(requestPoints, busOptions, this)
            }
            CAR -> {
                drivingSession =
                    drivingRouter.requestRoutes(requestPoints, drivingOptions, vehicleOptions, this)
            }
            WALK -> {
                pedestrianRouter.requestRoutes(requestPoints, TimeOptions(), this)
            }
        }
    }

    private fun requestSuggest(query: String) {
        binding.lvSuggestResult.visibility = View.INVISIBLE
        searchManager.createSuggestSession().suggest(query, boundingBox, searchTypes, this)
    }

    private fun selectionInSuggestionImplementation(
        position: Int,
        firstPointText: String,
        secondPointText: String
    ) {
        val result = suggestResult[position]

        if (binding.searchFirstPoint.isFocused) {
            lifecycleScope.launch {
                viewModel.saveFirstPoint(result)
            }
            binding.searchFirstPoint.setText(result)
            submitQuery(firstPointText)
        }

        if (binding.searchSecondPoint.isFocused) {
            lifecycleScope.launch {
                viewModel.saveSecondPoint(result)
            }
            binding.searchSecondPoint.setText(result)
            submitQuery(secondPointText)
        }
        binding.lvSuggestResult.visibility = View.INVISIBLE
    }

    private fun saveFirstPointCoordinates(resultLocation: Point) = lifecycleScope.launch {
        launch {
            viewModel.saveFirstPointLongitude(resultLocation.longitude)
        }
        launch {
            viewModel.saveFirstPointLatitude(resultLocation.latitude)
        }
    }

    private fun saveSecondPointCoordinates(resultLocation: Point) = lifecycleScope.launch {
        launch {
            viewModel.saveSecondPointLongitude(resultLocation.longitude)
        }
        launch {
            viewModel.saveSecondPointLatitude(resultLocation.latitude)
        }
    }

    private fun saveUserCameraCoordinates() = lifecycleScope.launch {
        saveCameraCoordinatesJob.start()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val saveCameraCoordinatesJob = applicationScope.launch(start = CoroutineStart.LAZY) {
        waitTillOtherCoroutinesStart()
        while (true) {
            viewModel.saveUserCameraLocationLongitude(
                binding.mapKit.map.cameraPosition.target.longitude
            )
            viewModel.saveUserCameraLocationLatitude(
                binding.mapKit.map.cameraPosition.target.latitude
            )
            delay(WAIT_3_SECONDS)
        }
    }

    private suspend fun waitTillOtherCoroutinesStart() {
        withContext(Dispatchers.Default) {
            delay(WAIT_10_SECONDS)
        }
    }

    private fun moveCameraToSearchedPoint(resultLocation: Point, zoomChange: Float) {
        binding.mapKit.map.move(
            CameraPosition(resultLocation, zoomChange, ZERO.toFloat(), ZERO.toFloat()),
            Animation(Animation.Type.LINEAR, 1f),
            null
        )
    }

    private fun placeMarkerToPreviousLocation(previousLocation: Point) {
        binding.mapKit.map.mapObjects.addPlacemark(
            previousLocation,
            ImageProvider.fromResource(requireContext(), R.drawable.search_result)
        )
    }

    private fun preloadUserCameraCoordinates() {
        viewModel.readUserCameraLocationLatitude.observe(viewLifecycleOwner) { latitude ->
            if (userCameraPositionLatitude != latitude) {
                userCameraPositionLatitude = latitude
                hideKeyboard()
            }
        }
        viewModel.readUserCameraLocationLongitude.observe(viewLifecycleOwner) { longitude ->
            if (userCameraPositionLongitude != longitude) userCameraPositionLongitude = longitude
        }
    }

    private fun preloadFirstPointCoordinates() {
        viewModel.readFirstPointLongitude.observe(viewLifecycleOwner) { longitude ->
            if (firstPointLongitude != longitude) firstPointLongitude = longitude
        }
        viewModel.readFirstPointLatitude.observe(viewLifecycleOwner) { latitude ->
            if (firstPointLatitude != latitude) firstPointLatitude = latitude
        }
    }

    private fun preloadSecondPointCoordinates() {
        viewModel.readSecondPointLongitude.observe(viewLifecycleOwner) { longitude ->
            if (secondPointLongitude != longitude) secondPointLongitude = longitude
        }
        viewModel.readSecondPointLatitude.observe(viewLifecycleOwner) { latitude ->
            if (secondPointLatitude != latitude) secondPointLatitude = latitude
        }
    }

    private fun viewsBehaviorWhileTrackingUser() {
        binding.apply {
            when (userTracking) {
                true -> {
                    turnUserTracking.setBackgroundResource(R.drawable.user_location_track_red)
                    tilSecondPoint.visibility = View.INVISIBLE
                    tilFirstPoint.visibility = View.INVISIBLE
                    ibMakeRoute.visibility = View.INVISIBLE
                    mapOptions.visibility = View.INVISIBLE
                    routeOptions.visibility = View.INVISIBLE
                }
                false -> {
                    turnUserTracking.setBackgroundResource(R.drawable.user_location_track)
                    turnUserTracking.visibility = View.VISIBLE
                    tilSecondPoint.visibility = View.VISIBLE
                    tilFirstPoint.visibility = View.VISIBLE
                    ibMakeRoute.visibility = View.VISIBLE
                    mapOptions.visibility = View.VISIBLE
                    routeOptions.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun userTrackingEnabled() {
        when (userTracking) {
            true -> {
                userLocationLayer.setAnchor(
                    PointF(
                        (binding.mapKit.width * DEFAULT_ANCHOR_VALUE_05).toFloat(),
                        (binding.mapKit.height * DEFAULT_ANCHOR_VALUE_05).toFloat()
                    ),
                    PointF(
                        (binding.mapKit.width * DEFAULT_ANCHOR_VALUE_05).toFloat(),
                        (binding.mapKit.height * DEFAULT_ANCHOR_VALUE_083).toFloat()
                    )
                )

                userLocationLayer.isHeadingEnabled = true
                userLocationLayer.isAutoZoomEnabled = false
            }
            false -> {
                if (locationPermissionIsAllowed) {
                    userLocationLayer.isHeadingEnabled = false
                    userLocationLayer.isAutoZoomEnabled = false
                    userLocationLayer.resetAnchor()
                }
            }
        }
    }

    private fun setUserPreloadingChoice(userChoice: Int) {
        when (userChoice) {
            START -> {
                if (abs(firstPointLatitude + firstPointLongitude) != ZERO) {
                    val firstPoint = Point(firstPointLatitude, firstPointLongitude)
                    moveCameraToSearchedPoint(firstPoint, zoomChange)
                    showBothMarkersFromPreviousLocations()
                    recreateRoute()
                } else {
                    moveCameraToSearchedPoint(Point(ZERO, ZERO), 0f)
                }
            }
            DESTINATION -> {
                if (abs(secondPointLatitude + secondPointLongitude) != ZERO) {
                    val secondPoint = Point(secondPointLatitude, secondPointLongitude)
                    moveCameraToSearchedPoint(secondPoint, zoomChange)
                    showBothMarkersFromPreviousLocations()
                    recreateRoute()
                } else {
                    moveCameraToSearchedPoint(Point(ZERO, ZERO), 0f)
                }
            }
            PREVIOUS_CAMERA_POSITION -> {
                if (abs(userCameraPositionLatitude + userCameraPositionLongitude) != ZERO) {
                    val cameraPoint = Point(userCameraPositionLatitude, userCameraPositionLongitude)
                    moveCameraToSearchedPoint(cameraPoint, zoomChange)
                    showBothMarkersFromPreviousLocations()
                    recreateRoute()
                } else {
                    moveCameraToSearchedPoint(Point(ZERO, ZERO), 0f)
                }
            }
            DO_NOT_SAVE_LOCATIONS -> {
                moveCameraToSearchedPoint(Point(ZERO, ZERO), 0f)
                readOnlyMapMode()
            }
        }
    }

    fun recreateRoute(){
        val first = (firstPointLatitude + firstPointLongitude)
        val second = (secondPointLatitude + secondPointLongitude)
        if (abs(first + second) != ZERO) {
            drivingRouteRequest(userRouteTypeChoice)
        }
    }

    private fun readOnlyMapMode() {
        binding.searchFirstPoint.text?.clear()
        binding.searchSecondPoint.text?.clear()
        clearSearchFocus()
        binding.mapKit.map.mapObjects.clear()

        viewModel.deleteFirstPoint()
        viewModel.deleteFirstPointLatitude()
        viewModel.deleteFirstPointLongitude()

        viewModel.deleteSecondPoint()
        viewModel.deleteSecondPointLatitude()
        viewModel.deleteSecondPointLongitude()

        viewModel.deleteUserCameraLocationLatitude()
        viewModel.deleteUserCameraLocationLongitude()
    }

    private fun showBothMarkersFromPreviousLocations() {
        binding.mapKit.map.mapObjects.addPlacemark(
            Point(firstPointLatitude, firstPointLongitude),
            ImageProvider.fromResource(requireContext(), R.drawable.search_result)
        )
        binding.mapKit.map.mapObjects.addPlacemark(
            Point(secondPointLatitude, secondPointLongitude),
            ImageProvider.fromResource(requireContext(), R.drawable.search_result)
        )
    }

    private fun hideViewsWhileSearching() {
        binding.apply {
            ibMakeRoute.visibility = View.INVISIBLE
            mapOptions.visibility = View.INVISIBLE
            routeOptions.visibility = View.INVISIBLE
            zoomIn.visibility = View.INVISIBLE
            zoomOut.visibility = View.INVISIBLE
            turnUserTracking.visibility = View.INVISIBLE
        }
    }

    private fun showViewsWhenCameraIsMoving() {
        binding.apply {
            ibMakeRoute.visibility = View.VISIBLE
            mapOptions.visibility = View.VISIBLE
            routeOptions.visibility = View.VISIBLE
            zoomIn.visibility = View.VISIBLE
            zoomOut.visibility = View.VISIBLE
            turnUserTracking.visibility = View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireActivity()
            .getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun clearSearchFocus() {
        binding.searchFirstPoint.clearFocus()
        binding.searchSecondPoint.clearFocus()
    }

    private fun refreshFragment() {
        val navController = findNavController()
        navController.run {
            popBackStack()
            navigate(R.id.mapScreenFragment)
        }
    }

    // DRAW ROUTE LINES ON A MAP
    private fun drawColoredLinesOnMap(
        geometry: Polyline
    ) {
        val drawLineOnMap = binding.mapKit.map.mapObjects.addPolyline(geometry)

        when (userRouteTypeChoice) {
            BUS -> drawLineOnMap.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.bus))
            CAR -> drawLineOnMap.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.car))
            WALK -> drawLineOnMap.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.walk))
        }
    }

    private fun createUserOnMap() {
        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(binding.mapKit.mapWindow)
        userLocationLayer.setObjectListener(this)
        userLocationLayer.isVisible = true
        locationPermissionIsAllowed = true
    }

    // USER SHOW ON MAP
    override fun onObjectAdded(userLoacation: UserLocationView) {
        userLoacation.arrow.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.user_arrow))

        val userPin: CompositeIcon = userLoacation.pin.useCompositeIcon()
        userPin.setIcon(
            "pin",
            ImageProvider.fromResource(
                requireContext(),
                R.drawable.user_arrow
            ),
            IconStyle().setAnchor(PointF(DEFAULT_ARROW_SIZE, DEFAULT_ARROW_SIZE))
                .setRotationType(RotationType.ROTATE)
                .setZIndex(1f)
                .setScale(DEFAULT_ARROW_SIZE)
        )

        userLoacation.accuracyCircle.fillColor = Color.TRANSPARENT
    }

    override fun onObjectRemoved(userLocation: UserLocationView) = Unit
    override fun onObjectUpdated(userLocation: UserLocationView, event: ObjectEvent) = Unit

    // CAMERA MOVING
    override fun onCameraPositionChanged(
        p0: Map,
        position: CameraPosition,
        p2: CameraUpdateReason,
        cameraIsNotMoving: Boolean
    ) {
        // cameraIsNotMoving updates very fast do not do anything heavy here
        if (!cameraIsNotMoving) {
            if (!userTracking) {
                showViewsWhenCameraIsMoving()
            }
        }
    }

    // SEARCH error
    override fun onSearchError(error: Error) {
        commonError(error, "Search")
    }

    // SEARCH response
    override fun onSearchResponse(response: Response) {
        val mapObjects: MapObjectCollection = binding.mapKit.map.mapObjects
        mapObjects.clear()
        for (searchResult in response.collection.children) {
            val resultLocation = searchResult.obj!!.geometry[0].point
            if (resultLocation != null) {
                mapObjects.addPlacemark(
                    resultLocation,
                    ImageProvider.fromResource(requireContext(), R.drawable.search_result)
                )

                // Save and Move only if 1 instance of such location exist
                // for example will not save multiple caffe instances
                if (response.collection.children.size == 1) {
                    if (binding.searchFirstPoint.isFocused) {
                        saveFirstPointCoordinates(resultLocation)
                        moveCameraToSearchedPoint(resultLocation, zoomChange)
                        placeMarkerToPreviousLocation(Point(secondPointLatitude, secondPointLongitude))
                        requestPoints.clear()
                    }
                    if (binding.searchSecondPoint.isFocused) {
                        saveSecondPointCoordinates(resultLocation)
                        moveCameraToSearchedPoint(resultLocation, zoomChange)
                        placeMarkerToPreviousLocation(Point(firstPointLatitude, firstPointLongitude))
                        requestPoints.clear()
                    }
                } else {
                    showViewsWhenCameraIsMoving()
                }
            }
        }
    }

    // PEDESTRIAN - BUS errors
    override fun onMasstransitRoutesError(error: Error) {
        commonError(error, "PEDESTRIAN + BUS")
    }

    // PEDESTRIAN - BUS
    override fun onMasstransitRoutes(routes: MutableList<Route>) {
        if (routes.size > 0) {
            for (route in routes[0].sections) {
                drawColoredLinesOnMap(SubpolylineHelper.subpolyline(routes[0].geometry, route.geometry))
            }
        }
    }

    // Driving error
    override fun onDrivingRoutesError(error: Error) {
        commonError(error, "Driving")
    }

    // Driving
    override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
        if (routes.size > 0) {
            for (route in routes[0].sections) {
                drawColoredLinesOnMap(SubpolylineHelper.subpolyline(routes[0].geometry, route.geometry))
            }
        }
    }

    // SUGGEST error
    override fun onError(error: Error) {
        commonError(error, "Suggest")
    }

    // SUGGEST response
    override fun onResponse(suggest: MutableList<SuggestItem>) {
        suggestResult.clear()
        for (i in 0 until 10.coerceAtMost(suggest.size)) {
            suggest[i].displayText?.let { suggestResult.add(it) }
        }

        resultAdapter.notifyDataSetChanged()

        if (binding.searchFirstPoint.text.toString() != firstPointName) {
            binding.lvSuggestResult.visibility = View.VISIBLE
            hideViewsWhileSearching()
        }

        if (binding.searchSecondPoint.text.toString() != secondPointName) {
            binding.lvSuggestResult.visibility = View.VISIBLE
            hideViewsWhileSearching()
        }
    }

    // DESELECT object on a map
    override fun onMapTap(p0: Map, p1: Point) {
        binding.mapKit.map.deselectGeoObject()
    }

    override fun onMapLongTap(p0: Map, p1: Point) = Unit

    // SELECT  object on a map
    override fun onObjectTap(geoObjectTapEvent: GeoObjectTapEvent): Boolean {
        val selectionMetadata: GeoObjectSelectionMetadata = geoObjectTapEvent
            .geoObject
            .metadataContainer
            .getItem(GeoObjectSelectionMetadata::class.java)

        binding.mapKit.map.selectGeoObject(selectionMetadata.id, selectionMetadata.layerId)

        return true
    }

    private fun mapDarkMode() = lifecycleScope.launch {
        viewModel.dataStore.darkModeEnabled.collect {
            binding.mapKit.map.isNightModeEnabled = it
        }
    }

    private fun commonError(error: Error, errorType: String) {
        var errorMessage = getString(R.string.unknown_error_message)
        Timber.i("errorType: $errorType")
        if (error is RemoteError) {
            errorMessage = getString(R.string.remote_error_message)
        } else if (error is NetworkError) {
            errorMessage = getString(R.string.network_error_message)
        }

        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapScreenBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onStop() {
        binding.mapKit.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapKit.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCameraCoordinatesJob.cancel("canceling to not stuck jobs when rotating the device")
    }

    override fun onPause() {
        super.onPause()
        saveCameraCoordinatesJob.cancel("canceling to not do saving on background")
    }
}
