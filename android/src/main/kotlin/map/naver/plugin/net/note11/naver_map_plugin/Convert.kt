package map.naver.plugin.net.note11.naver_map_plugin

import android.content.Context
import android.graphics.*
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.overlay.OverlayImage
import io.flutter.view.FlutterMain
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import map.naver.plugin.net.lbstech.naver_map_plugin.R
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

object Convert {
    fun carveMapOptions(sink: NaverMapOptionSink, options: Map<String, Any>) = sink.run {
        val keyFunList: Map<String, (Any) -> Unit> = mapOf(
            /* Boolean */
            "liteModeEnable" to { setLiteModeEnable(it as Boolean) },
            "nightModeEnable" to { setNightModeEnable(it as Boolean) },
            "indoorEnable" to { setIndoorEnable(it as Boolean) },
            "locationButtonEnable" to { setLocationButtonEnable(it as Boolean) },
            "scrollGestureEnable" to { setScrollGestureEnable(it as Boolean) },
            "zoomGestureEnable" to { setZoomGestureEnable(it as Boolean) },
            "rotationGestureEnable" to { setRotationGestureEnable(it as Boolean) },
            "tiltGestureEnable" to { setTiltGestureEnable(it as Boolean) },
            "locationButtonEnable" to { setLocationButtonEnable(it as Boolean) },
            "logoClickEnabled" to { setLogoClickEnable(it as Boolean) },
            /* Int */
            "mapType" to { setMapType(it as Int) },
            "locationTrackingMode" to { setLocationTrackingMode(it as Int) },
            /* Double */
            "buildingHeight" to { setBuildingHeight(it as Double) },
            "symbolScale" to { setSymbolScale(it as Double) },
            "symbolPerspectiveRatio" to { setSymbolPerspectiveRatio(it as Double) },
            "maxZoom" to { setMaxZoom(it as Double) },
            "minZoom" to { setMinZoom(it as Double) },
            /* List */
            "activeLayers" to { setActiveLayers(it.toIntList()) },
            "contentPadding" to { setContentPadding(it.toDoubleList()) }
        )

        keyFunList.forEach { (k, f) -> if (options.containsKey(k)) f.invoke(options[k]!!) }
    }

    private fun Any.toDoubleList(): List<Double> {
        val list = this as List<Any?>
        val doubleList = list.filterIsInstance<Double>()
        return doubleList.ifEmpty {
            val floatList = list.filterIsInstance<Float>().map { it.toDouble() }
            require(floatList.isNotEmpty())
            floatList
        }
    }

    private fun Any.toIntList(): List<Int> {
        val list = this as List<Any?>
        val intList = list.filterIsInstance<Int>()
        return intList.ifEmpty {
            val longList = list.filterIsInstance<Long>().map { it.toInt() }
            require(longList.isNotEmpty())
            longList
        }
    }

    fun Any.toLatLng(): LatLng {
        val data = toDoubleList()
        return LatLng(data[0], data[1])
    }

    fun Any.toPoint(): PointF {
        val data = toDoubleList()
        return PointF(data[0].toFloat(), data[1].toFloat())
    }

    private fun Any.toLatLngBounds(): LatLngBounds {
        require(this is List<Any?>)
        val data: List<Any> = requireNoNulls()
        return LatLngBounds(data[0].toLatLng(), data[1].toLatLng())
    }

    fun Map<String, Any?>.toCameraPosition(): CameraPosition {
        val bearing: Double = get("bearing") as Double
        val tilt: Double = get("tilt") as Double
        val zoom: Double = get("zoom") as Double
        val target = get("target") as List<Any?>
        assert(target.size == 2) // target size 가 2가 아니면, 선언 실패(assert) 에러
        val lat = target[0] as Double
        val lng = target[1] as Double
        return CameraPosition(LatLng(lat, lng), zoom, tilt, bearing)
    }

    fun Map<String, Any>.toCameraUpdate(density: Float): CameraUpdate {
        require(isNotEmpty()) { "Cannot interpret $this as CameraUpdate" } // map 이 비어있다면 error

        // position
        get("newCameraPosition")?.let { p ->
            return CameraUpdate.toCameraPosition((p as Map<String, Any?>).toCameraPosition())
        }

        // scroll
        get("scrollTo")?.let { l ->
            val latLng = l.toLatLng()
            val zoomTo = get("zoomTo") as Double? ?: 0.0
            return if (zoomTo == 0.0) CameraUpdate.scrollTo(latLng)
            else CameraUpdate.scrollAndZoomTo(latLng, zoomTo)
        }

        /* zoom start */
        if (containsKey("zoomIn")) return CameraUpdate.zoomIn()
        if (containsKey("zoomOut")) return CameraUpdate.zoomOut()

        val zoomBy = get("zoomBy") as Double?
        if (zoomBy != null && zoomBy != 0.0) return CameraUpdate.zoomBy(zoomBy)

        val zoomTo = get("zoomTo") as Double?
        if (zoomTo != null && zoomTo != 0.0) return CameraUpdate.zoomTo(zoomTo)
        /* zoom end */

        val fitBounds = get("fitBounds") as List<Any?>

        val dp = fitBounds[1] as Int
        val px = (dp * density).roundToInt()
        return CameraUpdate.fitBounds(fitBounds[0]!!.toLatLngBounds(), px)
    }

    fun List<Any?>.toCoords(): List<LatLng> {
        val filteredData = filterIsInstance<List<Double>>().ifEmpty {
            filterIsInstance<List<Float>>().map { it.toDoubleList() }
        }
        return mutableListOf<LatLng>().apply {
            for (point in filteredData) add(LatLng(point[0], point[1]))
        }
    }

    fun CameraPosition.toJson(): Any = mapOf(
        "bearing" to bearing,
        "target" to target.toJson(),
        "tilt" to tilt,
        "zoom" to zoom
    )

    fun LatLngBounds.toJson(): Any = mapOf(
        "southwest" to southWest.toJson(),
        "northeast" to northEast.toJson()
    )

    fun LatLng.toJson(): Any = listOf(latitude, longitude)

    fun toHoles(data: List<Any?>): List<List<LatLng>> {
        val holes = mutableListOf<List<LatLng>>()
        for (ob in data) holes.add((ob as List<Any?>).toCoords())
        return holes
    }

    fun toColorInt(value: Any?): Int =
        if (value is Long || value is Int) Color.parseColor(String.format("#%08x", value)) else 0

    fun toOverlayImage(o: Any?): OverlayImage { // todo : bitmap support
        val assetName = o as String
        val key = FlutterMain.getLookupKeyForAsset(assetName)
        return OverlayImage.fromAsset(key)
    }

    fun toOverlayImageFromPath(o: Any?): OverlayImage {
        val imagePath = o as String
        return OverlayImage.fromPath(imagePath)
    }

    fun toOverlayImageFromURL(context:Context,url:String) : OverlayImage?{

        //        val image = Glide.with(context).asBitmap().load(url).circleCrop()
//        image.override(40,40)

        val backBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.markerback)
//        val markerBackResizeImage = resizeBitmap(context,backBitmap,50,60)
        val contentBitmap = getBtimapConvertFromURL(url)
//        val markerBackResizeImage = resizeBitmap(backBitmap,50,60)
//        val markerContentsResizeImage = resizeBitmap(resource,40,40)

        if (contentBitmap != null){
//            val markerBackResizeImage = resizeBitmap(context,backBitmap,50,60)
//        val markerContentsResizeImage = resizeBitmap(context,contentBitmap,40,40)
            Log.d("승하 위드",backBitmap.width.toString())
            val tempBitmap = resizeBitmap(context,contentBitmap,139,139)
//            val markerContentsResizeImage = resizeBitmap(context,contentBitmap,139,139)
            val markerContentsResizeImage = setCircleBitmap(tempBitmap)

            Log.d("승하 위드2",markerContentsResizeImage.width.toString())
            val resultBitmap = Bitmap.createBitmap(backBitmap.width,backBitmap.height,backBitmap.config)
            val resultCanvas = Canvas(resultBitmap)
            resultCanvas.drawBitmap(backBitmap, Matrix(),null)
            resultCanvas.drawBitmap(markerContentsResizeImage,dpToPxInt(5,context).toFloat(),dpToPxInt(4,context).toFloat(),null)
            return OverlayImage.fromBitmap(resultBitmap)
        }

        return null
    }

    fun resizeBitmap(context:Context,resource : Bitmap,width : Int, height : Int) : Bitmap{
        return Bitmap.createScaledBitmap(resource,dpToPxInt(width,context),dpToPxInt(height,context),true)
    }
    fun dpToPxInt(dp : Int,context :Context) : Int{
        val density = context.resources.displayMetrics.density
        Log.d("승하 위드3",density.toString())
        return (dp * density).toInt()
    }
    private fun getBtimapConvertFromURL(url:String) : Bitmap?{
        val url = URL(url)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        val bitmap = BitmapFactory.decodeStream(input)
        return bitmap
//        try{
//            GlobalScope.launch {
//                val url = URL(url)
//                val connection = url.openConnection() as HttpURLConnection
//                connection.doInput = true
//                connection.connect()
//                val input = connection.inputStream
//                val bitmap = BitmapFactory.decodeStream(input)
//
//
//            }
//        }catch (e: Exception){
//            Log.d("ImageException","UserMarker Image Load Fail : ${e}")
//        }



        /*
        public static Bitmap GetBitmapClippedCircle(Bitmap bitmap) {

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);

        final Path path = new Path();
        path.addCircle(
                  (float)(width / 2)
                , (float)(height / 2)
                , (float) Math.min(width, (height / 2))
                , Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return outputBitmap;
    }
         */
    }
    fun setCircleBitmap(bitmap : Bitmap) : Bitmap{
        val width = bitmap.width
        val height = bitmap.height
        val outputBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val path = Path()
        path.addCircle(
            (width/2).toFloat(),
            (height/2).toFloat(),
            Math.min(width,(height/2)).toFloat(),
            Path.Direction.CCW
        )
        val canvas = Canvas(outputBitmap)
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap,(0).toFloat(),(0).toFloat(),null)
        return outputBitmap
    }
}