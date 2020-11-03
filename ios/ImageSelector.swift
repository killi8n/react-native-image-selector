import AVKit
import Photos
import PhotosUI

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
    
    static func createCacheFile(imageData: Data) -> [String: Any] {
        let fileName: String = "react-native-image-selector_\(UUID().uuidString).png"
        let paths = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true)
        let path = paths.first ?? ""
        let filePath = "\(path)/\(fileName)"
        if !FileManager.default.fileExists(atPath: filePath) {
            FileManager.default.createFile(atPath: filePath, contents: imageData, attributes: nil)
        }
        return ["uri": "file://\(filePath)", "fileName": fileName, "type": "image/png", "fileSize": imageData.count]
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
                    if let imageData = pickedImage.pngData() {
                        let fileCreateResult = ImageSelector.createCacheFile(imageData: imageData)
                        response = fileCreateResult
                    }
                }
                break
            case .photoLibrary, .savedPhotosAlbum:
                if let pickedImage = info[UIImagePickerController.InfoKey.originalImage] as? UIImage {
                    if let imageData = pickedImage.pngData() {
                        let fileCreateResult = ImageSelector.createCacheFile(imageData: imageData)
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
            callback([nil, response])
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

class ImageShowerViewController: UIViewController {
    public var fetchedAssets: PHFetchResult<PHAsset>?
    public lazy var collectionView: UICollectionView = {
        self.layout.scrollDirection = .vertical
        let cv = UICollectionView(frame: .zero, collectionViewLayout: self.layout)
        cv.register(ImageShowerCell.self, forCellWithReuseIdentifier: self.reusableIdentifier)
        cv.backgroundColor = .white
        cv.delegate = self
        cv.dataSource = self
        return cv
    }()
    private var globalCallback: RCTResponseSenderBlock?
    private let layout = UICollectionViewFlowLayout()
    private let reusableIdentifier: String = "cell"
    
    init(fetchedAssets: PHFetchResult<PHAsset>, callback: @escaping RCTResponseSenderBlock) {
        super.init(nibName: nil, bundle: nil)
        self.fetchedAssets = fetchedAssets
        self.globalCallback = callback
        self.title = "모든 사진"
        let cancelBarButtonItem: UIBarButtonItem = UIBarButtonItem(barButtonSystemItem: .done, target: self, action: #selector(self.dismissViewController(_:)))
        self.navigationItem.rightBarButtonItem = cancelBarButtonItem
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.view.backgroundColor = .white
        self.view.addSubview(self.collectionView)
        self.collectionView.translatesAutoresizingMaskIntoConstraints = false
        let topAnchor = self.collectionView.topAnchor.constraint(equalTo: self.view.topAnchor)
        let leftAnchor = self.collectionView.leftAnchor.constraint(equalTo: self.view.leftAnchor)
        let bottomAnchor = self.collectionView.bottomAnchor.constraint(equalTo: self.view.bottomAnchor)
        let rightAnchor = self.collectionView.rightAnchor.constraint(equalTo: self.view.rightAnchor)
        self.view.addConstraints([topAnchor, leftAnchor, bottomAnchor, rightAnchor])
    }
    
    @objc
    func dismissViewController(_ sender: UIBarButtonItem) {
        self.dismiss(animated: true, completion: nil)
    }
}

class ImageShowerCell: UICollectionViewCell {
    let cellImageView: UIImageView = {
        let imageView = UIImageView(frame: .zero)
        imageView.contentMode = .scaleAspectFill
        imageView.layer.masksToBounds = true
        return imageView
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.contentView.backgroundColor = .groupTableViewBackground
        self.contentView.addSubview(self.cellImageView)
        self.cellImageView.translatesAutoresizingMaskIntoConstraints = false
        let topAnchor = self.cellImageView.topAnchor.constraint(equalTo: self.contentView.topAnchor)
        let leftAnchor = self.cellImageView.leftAnchor.constraint(equalTo: self.contentView.leftAnchor)
        let bottomAnchor = self.cellImageView.bottomAnchor.constraint(equalTo: self.contentView.bottomAnchor)
        let rightAnchor = self.cellImageView.rightAnchor.constraint(equalTo: self.contentView.rightAnchor)
        self.contentView.addConstraints([topAnchor, leftAnchor, bottomAnchor, rightAnchor])
    }

    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func prepareForReuse() {
        self.cellImageView.image = nil
    }
    
    func fetchImage(asset: PHAsset) -> Void {
        let manager = PHImageManager.default()
        let options = PHImageRequestOptions()
        options.deliveryMode = .opportunistic
        options.isSynchronous = true
        options.resizeMode = .exact
        manager.requestImage(for: asset, targetSize: CGSize(width: self.contentView.frame.size.width, height: self.contentView.frame.size.height), contentMode: .aspectFill, options: options) { [weak self] (image: UIImage?, info: [AnyHashable : Any]?) in
            guard let `self` = self else { return }
            if let image = image {
                self.cellImageView.image = image
                return
            }
        }
    }
}

extension ImageShowerViewController: UICollectionViewDelegateFlowLayout, UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        guard let assets = self.fetchedAssets else { return 0 }
        return assets.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: self.reusableIdentifier, for: indexPath) as? ImageShowerCell else { return UICollectionViewCell() }
        guard let assets = self.fetchedAssets else { return cell }
        let asset = assets.object(at: indexPath.item)
        DispatchQueue.main.async {
            cell.fetchImage(asset: asset)
        }
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        return CGSize(width: collectionView.layer.frame.width / 3 - 2, height: collectionView.layer.frame.height / 6.5)
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumInteritemSpacingForSectionAt section: Int) -> CGFloat {
        return 2
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumLineSpacingForSectionAt section: Int) -> CGFloat {
        return 4
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, insetForSectionAt section: Int) -> UIEdgeInsets {
        return UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let assets = self.fetchedAssets else { return }
        let asset = assets.object(at: indexPath.item)
        let manager = PHImageManager.default()
        let options = PHImageRequestOptions()
        options.version = .original
        options.isSynchronous = true
        manager.requestImageData(for: asset, options: options) { [weak self] (imageData: Data?, _: String?, _: UIImage.Orientation, _: [AnyHashable : Any]?) in
            guard let `self` = self else { return }
            if let imageData = imageData {
                let fileCreateResult = ImageSelector.createCacheFile(imageData: imageData)
                self.dismiss(animated: true) {
                    guard let callback = self.globalCallback else { return }
                    callback([
                        nil,
                        fileCreateResult
                    ])
                    self.globalCallback = nil
                }
            }
        }
    }
}

