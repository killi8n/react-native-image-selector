import AVKit
import Photos

//class EventEmitter {
//
//    /// Shared Instance.
//    public static var sharedInstance = EventEmitter()
//
//    // ReactNativeEventEmitter is instantiated by React Native with the bridge.
//    private static var eventEmitter: RCTEventEmitter!
//
//    private init() {}
//
//    // When React Native instantiates the emitter it is registered here.
//    func registerEventEmitter(eventEmitter: RCTEventEmitter) {
//        EventEmitter.eventEmitter = eventEmitter
//    }
//
//    func dispatch(name: String, body: Any?) {
//        EventEmitter.eventEmitter.sendEvent(withName: name, body: body)
//    }
//
//    /// All Events which must be support by React Native.
//    lazy var allEvents: [String] = {
//        var allEventNames: [String] = ["DidSelectItem"]
//
//        // Append all events here
//
//        return allEventNames
//    }()
//
//}

struct ErrorCode {
    static let cameraPermissionDenied: Int = 100
    static let libraryPermissionDenied: Int = 101
    static let simulatorError: Int = 102
    static let sourceTypeMismatch: Int = 103
    static let fileCreateError: Int = 104
}

struct ErrorMessage {
    static let cameraPermissionDenied: String = "CAMERA_PERMISSION_DENIED"
    static let libraryPermissionDenied: String = "LIBRARY_PERMISSION_DENIED"
    static let simulatorError: String = "SIMULATOR_ERROR"
    static let sourceTypeMismatch: String = "SOURCE_TYPE_MISMATCH"
    static let fileCreateError: String = "FILE_CREATE_ERROR"
}

@objc(ImageSelector)
class ImageSelector: NSObject, UINavigationControllerDelegate {
    
    let imagePickerController: UIImagePickerController = UIImagePickerController()
    
    private var fetchedAssets: PHFetchResult<PHAsset> = PHFetchResult<PHAsset>()
    private var globalCallback: RCTResponseSenderBlock?
    private var imageShowerViewController: ImageShowerViewController?
    private var options: [String: Any] = [:]
    
    override init() {
        super.init()
//        EventEmitter.sharedInstance.registerEventEmitter(eventEmitter: self)
    }
    
//    override func supportedEvents() -> [String]! {
//        return EventEmitter.sharedInstance.allEvents
//    }
    
    func requestCameraAuthorization() -> Void {
        AVCaptureDevice.requestAccess(for: .video) { [weak self] (isAuthorized: Bool) in
            guard let `self` = self else { return }
            guard let callback = self.globalCallback else { return }
            if isAuthorized {
                self.launchCamera()
            } else {
                let error: [String: Any] = ["code": ErrorCode.cameraPermissionDenied, "message": ErrorMessage.cameraPermissionDenied]
                callback([
                    error
                ])
                self.globalCallback = nil
            }
        }
    }
    
    func requestLibraryAuthorization() -> Void {
        PHPhotoLibrary.requestAuthorization { [weak self] (status: PHAuthorizationStatus) in
            guard let `self` = self else { return }
            guard let callback = self.globalCallback else { return }
            switch status {
                case .authorized:
                    self.launchLibrary()
                    break
                default:
                    let error: [String: Any] = ["code": ErrorCode.libraryPermissionDenied, "message": ErrorMessage.libraryPermissionDenied]
                    callback([
                        error
                    ])
                    self.globalCallback = nil
                    break
            }
        }
    }
    
    func checkCameraPermission() -> Void {
        guard let callback = self.globalCallback else { return }
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        switch status {
            case .authorized:
                self.launchCamera()
                break
            case .notDetermined:
                self.requestCameraAuthorization()
                break
            default:
                let error: [String: Any] = ["code": ErrorCode.cameraPermissionDenied, "message": ErrorMessage.cameraPermissionDenied]
                callback([
                    error
                ])
                self.globalCallback = nil
                break
        }
    }
    
    func checkLibraryPermission() -> Void {
        guard let callback = self.globalCallback else { return }
        let status = PHPhotoLibrary.authorizationStatus()
        switch status {
            case .authorized:
                self.launchLibrary()
                break
            case .notDetermined:
                self.requestLibraryAuthorization()
                break
            default:
                let error: [String: Any] = ["code": ErrorCode.libraryPermissionDenied, "message": ErrorMessage.libraryPermissionDenied]
                callback([
                    error
                ])
                self.globalCallback = nil
                break
        }
    }
    
    func launchLibrary() -> Void {
        guard let callback = self.globalCallback else { return }
        let fetchOptions = PHFetchOptions()
        fetchOptions.includeAssetSourceTypes = .typeUserLibrary
        fetchOptions.sortDescriptors = [.init(key: "creationDate", ascending: false)]
        self.fetchedAssets = PHAsset.fetchAssets(with: .image, options: fetchOptions)
        self.imageShowerViewController = ImageShowerViewController(fetchedAssets: self.fetchedAssets, options: self.options, callback: callback)
        guard let imageShowerViewController = self.imageShowerViewController else { return }
        PHPhotoLibrary.shared().register(self)
        DispatchQueue.main.async {
            guard let rootViewController = RCTPresentedViewController() else { return }
            let navigationController = UINavigationController(rootViewController: imageShowerViewController)
            navigationController.modalPresentationStyle = .overFullScreen
            rootViewController.present(navigationController, animated: true, completion: nil)
        }
    }
    
    func launchCamera() -> Void {
        self.imagePickerController.delegate = self
        self.imagePickerController.sourceType = .camera
        DispatchQueue.main.async {
            guard let rootViewController = RCTPresentedViewController() else { return }
            rootViewController.present(self.imagePickerController, animated: true) {
                
            }
        }
    }
    
    func parseOptions(options: [String: Any]?) -> Void {
        if let options = options {
            if let title = options["title"] {
                self.options["title"] = title
            }
            if let cancelButtonTitle = options["cancelButtonTitle"] {
                self.options["cancelButtonTitle"] = cancelButtonTitle
            }
            if let takePhotoButtonTitle = options["takePhotoButtonTitle"] {
                self.options["takePhotoButtonTitle"] = takePhotoButtonTitle
            }
            if let chooseFromLibraryButtonTitle = options["chooseFromLibraryButtonTitle"] {
                self.options["chooseFromLibraryButtonTitle"] = chooseFromLibraryButtonTitle
            }
            if let storageOptions = options["storageOptions"] as? [String: Any] {
                self.options["storageOptions"] = storageOptions
            }
            if let permissionDenied = options["permissionDenied"] {
                self.options["permissionDenied"] = permissionDenied
            }
        }
    }
    
    @objc
    func launchPicker(_ options: [String: Any]?, responseCallback callback: @escaping RCTResponseSenderBlock) -> Void {
        self.globalCallback = callback
        self.parseOptions(options: options)
        
        let alert = UIAlertController(title: self.options["title"] as? String ?? "Pick Photos", message: nil, preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: self.options["takePhotoButtonTitle"] as? String ?? "Take Photos", style: .default, handler: { [weak self] (_: UIAlertAction) in
            guard let `self` = self else { return }
            #if targetEnvironment(simulator)
                callback([
                    ["code": ErrorCode.simulatorError, "message": ErrorMessage.simulatorError]
                ])
                self.globalCallback = nil
            #else
                self.checkCameraPermission()
            #endif
        }))
        alert.addAction(UIAlertAction(title: self.options["chooseFromLibraryButtonTitle"] as? String ?? "Open Photo Gallery", style: .default, handler: { [weak self] (_: UIAlertAction) in
            guard let `self` = self else { return }
            self.checkLibraryPermission()
        }))
        alert.addAction(UIAlertAction(title: self.options["cancelButtonTitle"] as? String ?? "Cancel", style: .cancel, handler: { (_: UIAlertAction) in
            let callbackResponse: [[String: Any]?] = [nil, ["didCancel": true]]
            callback(callbackResponse as [Any])
            self.globalCallback = nil
        }))
        DispatchQueue.main.async {
            guard let rootViewController = RCTPresentedViewController() else { return }
            rootViewController.present(alert, animated: true) {
                
            }
        }
    }
}


extension ImageSelector: UIImagePickerControllerDelegate {
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        guard let callback = self.globalCallback else { return }
        picker.dismiss(animated: true) {
            let callbackResponse: [[String: Any]?] = [nil, ["didCancel": true]]
            callback(callbackResponse as [Any])
            self.globalCallback = nil
        }
    }
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        guard let callback = self.globalCallback else { return }
        var response: [String: Any]? = nil
        switch picker.sourceType {
            case .camera:
                if let pickedImage = info[UIImagePickerController.InfoKey.originalImage] as? UIImage {
                    if let rotatedImage = ImageUtil.rotateImage(image: pickedImage) {
                        if let imageData = rotatedImage.pngData() {
                            var pathDirectory: String? = nil
                            if let storageOptions = self.options["storageOptions"] as? [String: Any] {
                                if let path = storageOptions["path"] as? String {
                                    pathDirectory = path
                                }
                            }
                            let fileCreateResult = ImageUtil.createCacheFile(imageData: imageData, pathDirectory: pathDirectory, callback: self.globalCallback)
                            response = fileCreateResult
                        }
                    }
                }
                break
            case .photoLibrary, .savedPhotosAlbum:
//                이부분은 collection view 로 띄워서 pick하는 걸로 대체하였습니다.
//                this part would be unnecessary, because we will show the collection view instead of native image picker view controller.
                break
            default:
                break
        }
        picker.dismiss(animated: true) {
            guard let response = response else {
                callback([
                    ["code": ErrorCode.sourceTypeMismatch, "message": ErrorMessage.sourceTypeMismatch]
                ])
                self.globalCallback = nil
                return
            }
            let callbackResponse: [[String: Any]?] = [nil, response]
            callback(callbackResponse as [Any])
            self.globalCallback = nil
        }
    }
}

extension ImageSelector: PHPhotoLibraryChangeObserver {
    func photoLibraryDidChange(_ changeInstance: PHChange) {
        if let changedDetails = changeInstance.changeDetails(for: self.fetchedAssets) {
            guard let imageShowerViewController = self.imageShowerViewController else { return }
            let newFetchedAssets = changedDetails.fetchResultAfterChanges
            imageShowerViewController.fetchedAssets = newFetchedAssets
            DispatchQueue.main.async {
                imageShowerViewController.collectionView.reloadData()
            }
        }
    }
}
