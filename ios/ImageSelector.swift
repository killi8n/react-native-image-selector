import AVKit
import Photos
import PhotosUI

@objc(ImageSelector)
class ImageSelector: NSObject, UINavigationControllerDelegate {
    let pickerController: UIImagePickerController = UIImagePickerController()
    var phConfiguration: Any?
    var phPickerViewController: Any?
    var openLibraryResolve: RCTPromiseResolveBlock?
    var openLibraryReject: RCTPromiseRejectBlock?
    let fetchImagesDispatchGroup: DispatchGroup = DispatchGroup()
    let openLibraryDipsatchGroup: DispatchGroup = DispatchGroup()
    
    private var fetchedImages: [[String: Any]] = []
    
    private var fetchedAssets: PHFetchResult<PHAsset> = PHFetchResult<PHAsset>()
    private var beginIndex: Int = 0
    private var endIndex: Int = 9
    private var limit: Int = 10
    private var page: Int = 1
    private var isReachedEnd: Bool = false
    private var isLoading: Bool = false
    
    func setPickerControllerDelegate() -> Void {
        if #available(iOS 14.0, *) {
            var phConfiguration: PHPickerConfiguration = PHPickerConfiguration()
            phConfiguration.selectionLimit = 0
            phConfiguration.filter = .any(of: [.images, .livePhotos])
            self.phConfiguration = phConfiguration
            
            let phPickerViewController: PHPickerViewController = PHPickerViewController(configuration: self.phConfiguration as! PHPickerConfiguration)
            self.phPickerViewController = phPickerViewController
            (self.phPickerViewController as! PHPickerViewController).delegate = self
        } else {
            self.pickerController.delegate = self
        }
    }
    
    func getRootViewController() -> UIViewController? {
        guard let keyWindow = UIApplication.shared.keyWindow else {
            return nil
        }
        guard let rootViewController = keyWindow.rootViewController else {
            return nil
        }
        return rootViewController
    }
    
    func presentPickerController(pickerController: UIImagePickerController) -> Void {
        guard let rootViewController = self.getRootViewController() else {
            return
        }
        rootViewController.present(pickerController, animated: true, completion: nil)
    }
    
    @available(iOS 14, *)
    func presentPHPickerViewController(phPickerViewController: PHPickerViewController) -> Void {
        guard let rootViewController = self.getRootViewController() else {
            return
        }
        DispatchQueue.main.async {
            rootViewController.present(phPickerViewController, animated: true, completion: nil)
        }
    }
    
    @available(iOS 14, *)
    func presentSharedLibrary() -> Void {
        let library = PHPhotoLibrary.shared()
        guard let rootViewController = self.getRootViewController() else { return }
        library.presentLimitedLibraryPicker(from: rootViewController)
    }
    
    func checkOpenLibraryPermission() -> PHAuthorizationStatus {
        let authorizationStatus = PHPhotoLibrary.authorizationStatus()
        return authorizationStatus
    }
    
    func fetchImages() -> Void {
        let fetchOptions = PHFetchOptions()
        fetchOptions.includeAssetSourceTypes = .typeUserLibrary
        self.fetchedAssets = PHAsset.fetchAssets(with: .image, options: fetchOptions)
        if self.fetchedAssets.count < self.limit {
            self.endIndex = self.fetchedAssets.count - 1
        }
        
        let fetchIndexSet = IndexSet(self.beginIndex...self.endIndex)
        self.fetchedImages = []
        self.fetchedAssets.enumerateObjects(at: fetchIndexSet, options: .concurrent) { (asset: PHAsset, index: Int, stop: UnsafeMutablePointer<ObjCBool>) in
            var imageInfo: [String: Any] = [:]
            
            let resource = PHAssetResource.assetResources(for: asset)
            if let imageSizeByte = resource.first?.value(forKey: "fileSize") as? Float {
                imageInfo["fileSize"] = imageSizeByte
//                if MB: imageSizeByte / (1024.0 * 1024.0)
            }
            if let fileName = resource.first?.originalFilename {
                imageInfo["fileName"] = fileName
                imageInfo["type"] = (fileName as NSString).pathExtension
            }
            
            self.fetchImagesDispatchGroup.enter()

            asset.requestContentEditingInput(with: nil) { (input: PHContentEditingInput?, _: [AnyHashable : Any]) in
                if let assetInput = input {
                    if let fulleSizeImageURL = assetInput.fullSizeImageURL {
                        imageInfo["uri"] = fulleSizeImageURL.absoluteString
                    }
                }
                self.fetchImagesDispatchGroup.leave()
            }
            
//            PHImageManager.default().requestImageData(for: asset, options: nil) { (data: Data?, _: String?, _: UIImage.Orientation, info: [AnyHashable : Any]?) in
//                if let imageData = data {
//                    imageInfo["data"] = imageData.base64EncodedString())
//                }
//                self.fetchImagesDispatchGroup.leave()
//            }
            self.fetchImagesDispatchGroup.notify(queue: .main) {
                self.fetchedImages.append(imageInfo)
                if index == self.endIndex {
                    if self.endIndex >= self.fetchedAssets.count - 1 {
                        self.isReachedEnd = true
                    }
                    self.beginIndex += self.limit
                    if self.endIndex + self.limit >= self.fetchedAssets.count - 1 {
                        self.endIndex = self.fetchedAssets.count - 1
                    } else {
                        self.endIndex += self.limit
                    }
                    self.page += 1
                    self.openLibraryDipsatchGroup.leave()
                }
            }
        }
    }
    
    func requestOpenLibraryPermission() -> Void {
        PHPhotoLibrary.requestAuthorization { (status: PHAuthorizationStatus) in
            switch status {
                case .authorized:
                    self.resolveAssets()
                    break
                case .denied, .limited, .notDetermined, .restricted:
                    break
                default:
                    break
            }
        }
    }
    
    @objc
    func checkOpenCameraPermission() -> Void {
        
    }
    
    func resolveAssets() {
        guard let resolve = self.openLibraryResolve else { return }
        if self.isReachedEnd || self.isLoading {
            resolve([])
        } else {
            DispatchQueue.global(qos: .background).async {
                self.isLoading = true
                self.openLibraryDipsatchGroup.enter()
                self.fetchImages()
                self.openLibraryDipsatchGroup.notify(queue: .main) {
                    self.isLoading = false
                    resolve(self.fetchedImages)
                }
            }
        }
    }
    
    @objc
    func getPhotos(_ limit: Int, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
        if self.page == 1 {
            self.limit = limit
            self.endIndex = limit - 1
        }
        PHPhotoLibrary.shared().register(self)
        self.openLibraryResolve = resolve
        self.openLibraryReject = reject
        let authorizationStatus = self.checkOpenLibraryPermission()
        switch authorizationStatus {
            case .authorized:
                self.resolveAssets()
                break
            case .denied, .limited, .notDetermined, .restricted:
                self.requestOpenLibraryPermission()
                break
            default:
                break
        }
    }
    
    @objc
    func initializePhotos(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
        self.fetchedAssets = PHFetchResult<PHAsset>()
        self.fetchedImages = []
        self.beginIndex = 0
        self.endIndex = 9
        self.limit = 10
        self.page = 1
        self.isReachedEnd = false
        self.isLoading = false
        
        resolve(nil)
    }
    
    @objc
    func openCamera() -> Void {
        self.setPickerControllerDelegate()
        if UIImagePickerController.isSourceTypeAvailable(.camera) {
            self.pickerController.sourceType = .camera
            self.presentPickerController(pickerController: self.pickerController)
        }
    }
}


extension ImageSelector: UIImagePickerControllerDelegate {
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        picker.dismiss(animated: true, completion: nil)
        guard let _ = info[UIImagePickerController.InfoKey.originalImage] else {
            return
        }
        guard let resolve = self.openLibraryResolve, let _ = self.openLibraryReject else {
            return
        }
        resolve("success")
    }
}

extension ImageSelector: PHPickerViewControllerDelegate {
    @available(iOS 14, *)
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true, completion: nil)
    }
}

extension ImageSelector: PHPhotoLibraryChangeObserver {
    func photoLibraryDidChange(_ changeInstance: PHChange) {
        if let changedDetails = changeInstance.changeDetails(for: self.fetchedAssets) {
            self.fetchedAssets = changedDetails.fetchResultAfterChanges
        }
    }
}
