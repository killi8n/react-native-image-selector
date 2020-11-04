import AVKit
import Photos

class EventEmitter {

    /// Shared Instance.
    public static var sharedInstance = EventEmitter()

    // ReactNativeEventEmitter is instantiated by React Native with the bridge.
    private static var eventEmitter: RCTEventEmitter!

    private init() {}

    // When React Native instantiates the emitter it is registered here.
    func registerEventEmitter(eventEmitter: RCTEventEmitter) {
        EventEmitter.eventEmitter = eventEmitter
    }

    func dispatch(name: String, body: Any?) {
        EventEmitter.eventEmitter.sendEvent(withName: name, body: body)
    }

    /// All Events which must be support by React Native.
    lazy var allEvents: [String] = {
        var allEventNames: [String] = ["DidSelectItem"]

        // Append all events here
        
        return allEventNames
    }()

}

@objc(ImageSelector)
class ImageSelector: RCTEventEmitter, UINavigationControllerDelegate {
    
    let imagePickerController: UIImagePickerController = UIImagePickerController()
    
    private var fetchedAssets: PHFetchResult<PHAsset> = PHFetchResult<PHAsset>()
    private var globalCallback: RCTResponseSenderBlock?
    private var imageShowerViewController: ImageShowerViewController?
    
    override init() {
        super.init()
        EventEmitter.sharedInstance.registerEventEmitter(eventEmitter: self)
    }
    
    override func supportedEvents() -> [String]! {
        return EventEmitter.sharedInstance.allEvents
    }
    
    func requestCameraAuthorization() -> Void {
        AVCaptureDevice.requestAccess(for: .video) { [weak self] (isAuthorized: Bool) in
            guard let `self` = self else { return }
            guard let callback = self.globalCallback else { return }
            if isAuthorized {
                self.launchCamera()
            } else {
                let error = ["error": "CAMERA_PERMISSION_DENIED"]
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
                    let error = ["error": "LIBRARY_PERMISSION_DENIED"]
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
                let error = ["error": "CAMERA_PERMISSION_DENIED"]
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
                let error = ["error": "LIBRARY_PERMISSION_DENIED"]
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
        self.fetchedAssets = PHAsset.fetchAssets(with: .image, options: fetchOptions)
        self.imageShowerViewController = ImageShowerViewController(fetchedAssets: self.fetchedAssets, callback: callback)
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
    
    @objc
    func launchPicker(_ callback: @escaping RCTResponseSenderBlock) -> Void {
        self.globalCallback = callback
        let alert = UIAlertController(title: "사진 선택", message: nil, preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: "사진 촬영", style: .default, handler: { [weak self] (_: UIAlertAction) in
            guard let `self` = self else { return }
            #if targetEnvironment(simulator)
                callback([
                    ["error": "SIMULATOR_ERROR"]
                ])
                self.globalCallback = nil
            #else
                self.checkCameraPermission()
            #endif
        }))
        alert.addAction(UIAlertAction(title: "앨범에서 가져오기", style: .default, handler: { [weak self] (_: UIAlertAction) in
            guard let `self` = self else { return }
            self.checkLibraryPermission()
        }))
        alert.addAction(UIAlertAction(title: "취소", style: .cancel, handler: { (_: UIAlertAction) in
            callback([
                ["error": "USER_CANCEL"]
            ])
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
            callback([
                ["error": "USER_CANCEL"]
            ])
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
                            let fileCreateResult = ImageUtil.createCacheFile(imageData: imageData)
                            response = fileCreateResult
                        }
                    }
                }
                break
            case .photoLibrary, .savedPhotosAlbum:
                if let pickedImage = info[UIImagePickerController.InfoKey.originalImage] as? UIImage {
                    if let imageData = pickedImage.pngData() {
                        let fileCreateResult = ImageUtil.createCacheFile(imageData: imageData)
                        response = fileCreateResult
                    }
                }
                break
            default:
                break
        }
        picker.dismiss(animated: true) {
            guard let response = response else {
                callback([
                    ["error": "SOURCE_TYPE_MISMATCH"]
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
