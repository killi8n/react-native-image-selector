//
//  ImageUtil.swift
//  ImageSelector
//
//  Created by Dongho Choi on 2020/11/04.
//  Copyright © 2020 Facebook. All rights reserved.
//

import UIKit
import Photos

class ImageUtil: NSObject {
    static func createCacheFile(imageData: Data, pathDirectory: String?, callback: RCTResponseSenderBlock?) -> [String: Any]? {
        let fileManager = FileManager.default
        if let documentDirectoryPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first {
            let path = (documentDirectoryPath as NSString).appendingPathComponent(pathDirectory ?? "")
            do {
                try fileManager.createDirectory(atPath: path, withIntermediateDirectories: true, attributes: nil)
            } catch {
                if let callback = callback {
                    let callbackResponse: [[String: Any]?] = [["code": ErrorCode.fileCreateError, "message": ErrorMessage.fileCreateError]]
                    callback(callbackResponse as [Any])
                }
                return nil
            }
            let uuid: String = UUID().uuidString
            let fileName = (uuid as NSString).appendingPathExtension("png") ?? "\(uuid).png"
            let filePath = (path as NSString).appendingPathComponent(fileName)
            fileManager.createFile(atPath: filePath, contents: imageData, attributes: nil)
            let base64EncodedString: String = imageData.base64EncodedString()
            return [
                "data": base64EncodedString,
                "path": filePath,
                "uri": "file://\(filePath)",
                "fileName": fileName,
                "type": "image/png",
                "fileSize": imageData.count
            ]
        }
        return nil
    }
    
    static func rotateImage(image: UIImage) -> UIImage? {
        if image.imageOrientation == .up {
            return image
        }
        UIGraphicsBeginImageContext(image.size)
        image.draw(in: CGRect(origin: CGPoint.zero, size: image.size))
        let copy = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return copy
    }
    
    static func requestImage(asset: PHAsset, deliveryMode: PHImageRequestOptionsDeliveryMode, resizeMode: PHImageRequestOptionsResizeMode?, targetSize: CGSize, contentMode: PHImageContentMode, completion: @escaping (_ image: UIImage) -> Void, exceptionCompletion: (() -> Void)?) -> Void {
        let manager = PHImageManager.default()
        let options = PHImageRequestOptions()
        options.isNetworkAccessAllowed = true
        options.deliveryMode = deliveryMode
        options.isSynchronous = false
        if let resizeMode = resizeMode {
            options.resizeMode = resizeMode
        }
        manager.requestImage(for: asset, targetSize: targetSize, contentMode: contentMode, options: options) { (image: UIImage?, info: [AnyHashable : Any]?) in
            if let image = image {
                completion(image)
            } else {
                if let exceptionCompletion = exceptionCompletion {
                    exceptionCompletion()
                }
            }
        }
    }
    
    static func requestImageData(asset: PHAsset, resizeMode: PHImageRequestOptionsResizeMode?, completion: @escaping (_ imageData: Data) -> Void, exceptionCompletion: @escaping () -> Void) -> Void {
        let manager = PHImageManager.default()
        let options = PHImageRequestOptions()
        // options.progressHandler = .some({ (progressPercent: Double, error: Error?, stop: UnsafeMutablePointer<ObjCBool>, _: [AnyHashable : Any]?) in
        //     print(progressPercent)
        // })
        options.deliveryMode = .opportunistic
        options.isSynchronous = false
        options.isNetworkAccessAllowed = true
        if let resizeMode = resizeMode {
            options.resizeMode = resizeMode
        }
        
        manager.requestImageData(for: asset, options: options) { (imageData: Data?, _: String?, _: UIImage.Orientation, _: [AnyHashable : Any]?) in
            if let imageData = imageData {
                completion(imageData)
            } else {
                exceptionCompletion()
            }
        }
    }
}
