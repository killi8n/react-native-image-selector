//
//  ImageUtil.swift
//  ImageSelector
//
//  Created by Dongho Choi on 2020/11/04.
//  Copyright © 2020 Facebook. All rights reserved.
//

import UIKit

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
}
