//
//  ImageUtil.swift
//  ImageSelector
//
//  Created by Dongho Choi on 2020/11/04.
//  Copyright © 2020 Facebook. All rights reserved.
//

import UIKit

class ImageUtil: NSObject {
    static func createCacheFile(imageData: Data) -> [String: Any] {
        let fileName: String = "react-native-image-selector_\(UUID().uuidString).png"
        let paths = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true)
        let path = paths.first ?? ""
        let filePath = "\(path)/\(fileName)"
        if !FileManager.default.fileExists(atPath: filePath) {
            FileManager.default.createFile(atPath: filePath, contents: imageData, attributes: nil)
        }
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
