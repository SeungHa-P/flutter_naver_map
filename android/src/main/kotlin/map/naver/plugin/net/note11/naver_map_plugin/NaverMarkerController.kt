package map.naver.plugin.net.note11.naver_map_plugin

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import map.naver.plugin.net.note11.naver_map_plugin.Convert.toLatLng
import map.naver.plugin.net.note11.naver_map_plugin.Convert.toPoint
import map.naver.plugin.net.note11.naver_map_plugin.Convert.toColorInt
import map.naver.plugin.net.note11.naver_map_plugin.Convert.toOverlayImage
import map.naver.plugin.net.note11.naver_map_plugin.Convert.toOverlayImageFromPath
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Overlay
import android.os.Looper
import com.naver.maps.map.overlay.InfoWindow
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.get
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.naver.maps.map.overlay.InfoWindow.DefaultTextAdapter
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import map.naver.plugin.net.lbstech.naver_map_plugin.R
import map.naver.plugin.net.note11.naver_map_plugin.Convert.toOverlayImageFromURL
import java.util.HashMap
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class NaverMarkerController(
    private val naverMap: NaverMap, private val onClickListener: Overlay.OnClickListener,
    private val density: Float, private val context: Context
) {
    private val idToController = HashMap<String?, MarkerController?>()
    private val handler = Handler(Looper.getMainLooper())
    private val infoWindow = InfoWindow()
    private var markerIdOfInfoWindow: String? = null
    fun add(jsonArray: List<Any?>?) {
        if (jsonArray == null || jsonArray.isEmpty()) return
        val service = Executors.newCachedThreadPool()
        service.execute {
            for (json in jsonArray) {
                val data = json as HashMap<String, Any>
                val marker = MarkerController(data)
                marker.setOnClickListener(onClickListener)
                idToController[marker.id] = marker
            }
            handler.post {
                val markers: List<MarkerController?> = idToController.values.toList()
                for (marker in markers) {
                    marker!!.setMap(naverMap)
                }
            }
        }
        service.shutdown()
    }

    fun remove(jsonArray: List<Any?>?) {
        if (jsonArray == null || jsonArray.isEmpty()) return
        for (json in jsonArray) {
            val id = json as String?
            val marker = idToController[id]
            marker!!.setOnClickListener(null)
            marker.setMap(null)
            idToController.remove(id)
        }
    }

    fun modify(jsonArray: List<Any?>?) {
        if (jsonArray == null || jsonArray.isEmpty()) return
        for (json in jsonArray) {
            val data = json as HashMap<String, Any>
            val id = data["markerId"] as String?
            if (idToController.containsKey(id) && idToController[id] != null) idToController[id]!!
                .interpret(data)
        }
    }

    inner class MarkerController(jsonArray: HashMap<String, Any>) {
        val id: String = (jsonArray["markerId"] as String?)!!
        val marker: Marker = Marker()
        private var infoWindowText: String? = null

        init {
            marker.tag = this // onClickListener 에서 controller 객체 참조하기 위함.
            interpret(jsonArray)
        }

        fun interpret(json: HashMap<String, Any>) {
            val metrics = DisplayMetrics()
            val mgr = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            mgr.defaultDisplay.getMetrics(metrics)
            val density = metrics.density
            val position = json["position"]
            if (position != null) marker.position = position.toLatLng()
            val alpha = json["alpha"]
            if (alpha != null) marker.alpha = (alpha as Double).toFloat()
            val flat = json["flat"]
            if (flat != null) marker.isFlat = flat as Boolean
            val anchor = json["anchor"]
            if (anchor != null) marker.anchor = anchor.toPoint()
            val captionText = json["captionText"]
            if (captionText != null) marker.captionText = (captionText as String?)!!
            val captionTextSize = json["captionTextSize"]
            if (captionTextSize != null) marker.captionTextSize =
                (captionTextSize as Double).toFloat()
            val captionColor = json["captionColor"]
            if (captionColor != null) marker.captionColor = toColorInt(captionColor)
            val captionHaloColor = json["captionHaloColor"]
            if (captionHaloColor != null) marker.captionHaloColor = toColorInt(captionHaloColor)
            val width = json["width"]
            if (width != null) marker.width =
                (width as Int * this@NaverMarkerController.density).roundToInt()
            val height = json["height"]
            if (height != null) marker.height =
                (height as Int * this@NaverMarkerController.density).roundToInt()
            val maxZoom = json["maxZoom"]
            if (maxZoom != null) marker.maxZoom = maxZoom as Double
            val minZoom = json["minZoom"]
            if (minZoom != null) marker.minZoom = minZoom as Double
            val angle = json["angle"]
            if (angle != null) marker.angle = (angle as Double).toFloat()
            val captionRequestedWidth = json["captionRequestedWidth"]
            if (captionRequestedWidth != null) marker.captionRequestedWidth =
                captionRequestedWidth as Int
            val captionMaxZoom = json["captionMaxZoom"]
            if (captionMaxZoom != null) marker.captionMaxZoom = captionMaxZoom as Double
            val captionMinZoom = json["captionMinZoom"]
            if (captionMinZoom != null) marker.captionMinZoom = captionMinZoom as Double
            val captionOffset = json["captionOffset"]
            if (captionOffset != null) marker.captionOffset =
                (captionOffset as Int * density).roundToInt()
            val captionPerspectiveEnabled = json["captionPerspectiveEnabled"]
            if (captionPerspectiveEnabled != null) marker.isCaptionPerspectiveEnabled =
                captionPerspectiveEnabled as Boolean
            val zIndex = json["zIndex"]
            if (zIndex != null) marker.zIndex = zIndex as Int
            val globalZIndex = json["globalZIndex"]
            if (globalZIndex != null) marker.globalZIndex = globalZIndex as Int
            val iconTintColor = json["iconTintColor"]
            if (iconTintColor != null) marker.iconTintColor = toColorInt(iconTintColor)
            val subCaptionText = json["subCaptionText"]
            if (subCaptionText != null) marker.subCaptionText = (subCaptionText as String?)!!
            val subCaptionTextSize = json["subCaptionTextSize"]
            if (subCaptionTextSize != null) marker.subCaptionTextSize =
                (subCaptionTextSize as Double).toFloat()
            val subCaptionColor = json["subCaptionColor"]
            if (subCaptionColor != null) marker.subCaptionColor = toColorInt(subCaptionColor)
            val subCaptionHaloColor = json["subCaptionHaloColor"]
            if (subCaptionHaloColor != null) marker.subCaptionHaloColor =
                toColorInt(subCaptionHaloColor)
            val subCaptionRequestedWidth = json["subCaptionRequestedWidth"]
            if (subCaptionRequestedWidth != null) marker.subCaptionRequestedWidth =
                (subCaptionRequestedWidth as Int * this@NaverMarkerController.density).roundToInt()
            val icon = json["icon"]
            if (icon != null) marker.icon = toOverlayImage(icon)
            val iconImagePath = json["iconFromPath"]
            if (iconImagePath != null) marker.icon = toOverlayImageFromPath(iconImagePath)
            val iconImageUrl = json["iconFromUrl"]
            if (iconImageUrl != null) {
                val markerImage = toOverlayImageFromURL(context,iconImageUrl.toString())
                if(markerImage != null){
                        marker.icon = markerImage
                }
//                Glide.with(context).asBitmap().load(iconImageUrl)
////                    .apply(RequestOptions.circleCropTransform())
//                    .circleCrop()
//                    .into(
//                        object : CustomTarget<Bitmap>(){
//                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                                val backBitmap = BitmapFactory.decodeResource(context.resources,R.drawable.markerback)
////                                val markerBackResizeImage = resizeBitmap(backBitmap,50,60)
////                                val markerContentsResizeImage = resizeBitmap(resource,40,40)
////                                val resultBitmap = Bitmap.createBitmap(markerBackResizeImage.width,markerBackResizeImage.height,markerBackResizeImage.config)
//                                val resultBitmap = Bitmap.createBitmap(backBitmap.width,backBitmap.height,backBitmap.config)
//                                val resultCanvas = Canvas(resultBitmap)
////                                resultCanvas.drawBitmap(markerBackResizeImage, Matrix(),null)
////                                resultCanvas.drawBitmap(markerContentsResizeImage,dpToPxInt(5).toFloat(),dpToPxInt(4).toFloat(),null)
//                                resultCanvas.drawBitmap(backBitmap, Matrix(),null)
//                                resultCanvas.drawBitmap(resource,dpToPxInt(5).toFloat(),dpToPxInt(4).toFloat(),null)
////                                backBitmap.recycle()
////                                resource.recycle()
//                                marker.icon = OverlayImage.fromBitmap(resultBitmap)
//
//                            }
//                            override fun onLoadCleared(placeholder: Drawable?) {
//
//                            }
//                        }
//                    )

            }

            val infoWindow = json["infoWindow"]
            infoWindowText = if (infoWindow != null) infoWindow as String? else null
        }

        fun setMap(naverMap: NaverMap?) {
            marker.map = naverMap
        }

        fun setOnClickListener(onClickListener: Overlay.OnClickListener?) {
            marker.onClickListener = onClickListener
        }

        fun toggleInfoWindow() {
            if (infoWindowText == null) return
            if (markerIdOfInfoWindow != null && markerIdOfInfoWindow == id) {
                infoWindow.close()
                markerIdOfInfoWindow = null
            } else {
                infoWindow.adapter = object : DefaultTextAdapter(context) {
                    override fun getText(infoWindow: InfoWindow): CharSequence {
                        return infoWindowText as String
                    }
                }
                markerIdOfInfoWindow = id
                infoWindow.open(marker)
            }
        }
    }

    fun resizeBitmap(resource : Bitmap,width : Int, height : Int) : Bitmap{
        return Bitmap.createScaledBitmap(resource,dpToPxInt(width),dpToPxInt(height),true)
    }

    fun dpToPxInt(dp : Int) : Int{
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}