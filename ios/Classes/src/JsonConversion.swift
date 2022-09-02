//
//  JsonConversion.swift
//  naver_map_plugin
//
//  Created by Maximilian on 2020/08/20.
//

import Foundation
import NMapsMap
import Flutter

public func toLatLng(json: Any) -> NMGLatLng{
    let data = json as! Array<Double>
    return NMGLatLng(lat: data[0], lng: data[1])
}

public func toLatLngBounds(json: Any) -> NMGLatLngBounds{
    let data = json as! Array<Any>
    return NMGLatLngBounds(southWest: toLatLng(json: data[0]), northEast: toLatLng(json: data[1]))
}

public func toCameraPosition(json: Any) -> NMFCameraPosition{
    let data = json as! NSDictionary
    return NMFCameraPosition(toLatLng(json: data["target"]!),
                             zoom: data["zoom"] as! Double,
                             tilt: data["tilt"] as! Double,
                             heading: data["bearing"] as! Double)
}

public func toCameraUpdate(json: Any) -> NMFCameraUpdate{
    let data = json as! NSDictionary
    print(data)
    var cameraUpdate: NMFCameraUpdate?
    
    if let position = data["newCameraPosition"] as? Dictionary<String,Any?>{
        cameraUpdate = .init(position: toCameraPosition(json: position))
    }

    if let scrollTo = data["scrollTo"] as? Array<Double>{
        if let zoomTo = data["zoomTo"] as? Double{
            cameraUpdate = .init(scrollTo: toLatLng(json: scrollTo), zoomTo: zoomTo)
        } else {
            cameraUpdate = .init(scrollTo: toLatLng(json: scrollTo))
        }
    }

    if data["zoomIn"] != nil{ cameraUpdate = .withZoomIn() }
    if data["zoomOut"] != nil{ cameraUpdate = .withZoomOut() }

    if let fitBounds = data["fitBounds"] as? Array<Any>{
        let pt = fitBounds[1] as! Int
        cameraUpdate = .init(fit: toLatLngBounds(json: fitBounds[0] as Any), padding: CGFloat(pt))
    }

    cameraUpdate?.animation = .easeOut
    if data["duration"] != nil{
        cameraUpdate?.animationDuration = data["duration"] as? Double ?? 0.0
    }

    return cameraUpdate ?? NMFCameraUpdate()
}

public func toColor(colorNumber: NSNumber) -> UIColor {
    let value = colorNumber.uint64Value
    let red = CGFloat(exactly: (value & 0xFF0000) >> 16)! / 255.0
    let green = CGFloat(exactly: (value & 0xFF00) >> 8)! / 255.0
    let blue = CGFloat(exactly: (value & 0xFF))! / 255.0
    let alpha = CGFloat(exactly: (value & 0xFF000000) >> 24)! / 255.0
    return UIColor(red: red, green: green, blue: blue, alpha: alpha)
}

public func ptFromPx(_ px: NSNumber) -> CGFloat {
    let resolutionFactor = UIScreen.main.nativeBounds.width / UIScreen.main.bounds.width
    return CGFloat(truncating: px) / resolutionFactor
}

public func pxFromPt(_ pt: CGFloat) -> Int {
    let resolutionFactor = UIScreen.main.nativeBounds.width / UIScreen.main.bounds.width
    return Int(pt * resolutionFactor)
}

public func toOverlayImage(assetName: String, registrar: FlutterPluginRegistrar) -> NMFOverlayImage? {
    let assetPath = registrar.lookupKey(forAsset: assetName)
    return NMFOverlayImage(name: assetPath)
}

public func toOverlayImageFromFile(imagePath: String) -> NMFOverlayImage? {
    if let image = UIImage.init(contentsOfFile: imagePath) {
        return NMFOverlayImage(image: image, reuseIdentifier: imagePath)
    }
    return nil
}
public func toOverlayImageFromUrl(imageUrl: String) -> NMFOverlayImage? {
    var tempImg : UIImage
         if let ImageData = try? Data(contentsOf: URL(string: imageUrl)!) {
             tempImg = UIImage(data: ImageData)!.circle!
             let customView : UIView = {
                 let view = UIView(frame: .init(x: 0, y: 0, width: 49, height: 59))
                 view.backgroundColor = UIColor(patternImage: UIImage(named: "markerBack")!)
                 view.contentMode = .scaleAspectFill
                 let imageView = UIImageView(frame: .init(x: 4, y: 5, width: 40, height: 40))
                 imageView.image = tempImg
                 view.addSubview(imageView)
                 return view
             }()
             return NMFOverlayImage(image: customView.asImage()!)
     }
    return nil
}
// ============================= 객체를 json 으로 =================================


public func cameraPositionToJson(position: NMFCameraPosition) -> Dictionary<String, Any>{
    return [
        "tilt" : position.tilt,
        "target" : latlngToJson(latlng: position.target),
        "bearing" : position.heading,
        "zoom" : position.zoom
    ]
}

public func latlngToJson(latlng: NMGLatLng) -> Array<Double> {
    return [latlng.lat, latlng.lng]
}

public func latlngBoundToJson(bound: NMGLatLngBounds) -> Dictionary<String, Any>{
    return [
        "southwest" : latlngToJson(latlng: bound.southWest),
        "northeast" : latlngToJson(latlng: bound.northEast)
    ]
}


extension UIImage {
var circle: UIImage? {
        let square = CGSize(width: min(size.width, size.height), height: min(size.width, size.height))
        let imageView = UIImageView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: square))
        imageView.contentMode = .scaleAspectFill
        imageView.image = self
        imageView.layer.cornerRadius = square.width/2
        imageView.layer.masksToBounds = true
        UIGraphicsBeginImageContext(imageView.bounds.size)
        guard let context = UIGraphicsGetCurrentContext() else { return nil }
        imageView.layer.render(in: context)
        let result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return result
    }
}
extension UIView {

    // Using a function since `var image` might conflict with an existing variable
    // (like on `UIImageView`)
    func asImage() -> UIImage? {
        if #available(iOS 10.0, *) {
            let renderer = UIGraphicsImageRenderer(bounds: bounds)
            return renderer.image { rendererContext in
                layer.render(in: rendererContext.cgContext)
            }
        } else {
            return nil
        }
            
    }
}
