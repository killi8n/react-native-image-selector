# react-native-image-selector

This module is alternative version of https://github.com/react-native-image-picker/react-native-image-picker

The only change could be iOS (for iOS 14 limited selection issues).

So I created Image Viewer View Controller which only shows selected images (if user selected limited permission) only for iOS.

## Installation

`npm`

```sh
$ npm install react-native-image-selector
```

`yarn`

```sh
$ yarn add react-native-image-selector
```

`lerna`

```sh
$ lerna add react-native-image-selector
$ lerna add react-native-image-selector --scope="@some/package"
```

## Pre Usage

`iOS/info.plist`

```xml
    <key>PHPhotoLibraryPreventAutomaticLimitedAccessAlert</key>
    <false/>
    <key>NSCameraUsageDescription</key>
    <string>카메라 권한을 얻겠습니다.</string>
    <key>NSPhotoLibraryUsageDescription</key>
    <string>사진을 가져오겠습니다.</string>
```

`PHPhotoLibraryPreventAutomaticLimitedAccessAlert`

> // you can turn on this to <true /> if it is true, the permission alert will not be showed up every time when you request or check photo authorization.

`android/AndroidManifest.xml`

```xml
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.CAMERA" />
```

## Usage

```js
import ImageSelector, {
  ImageSelectorOptions,
} from 'react-native-image-selector';

// ...

const options: ImageSelectorOptions = {
  // import Options
  title: '사진 선택',
  cancelButtonTitle: '취소',
  takePhotoButtonTitle: '사진 촬영',
  chooseFromLibraryButtonTitle: '앨범에서 가져오기',
  storageOptions: {
    skipBackup: true,
    path: 'images',
  },
  permissionDenied: {
    title: '권한 설정',
    text: "이 기능을 이용하시려면 권한을 '허용'으로 변경해주세요.",
    reTryTitle: '변경하러가기',
    okTitle: '닫기',
  },
  // iOS Only
  iOSGridNumber: 4,
  // iOS Only, should be upper than iOS 15.0 for 'pageSheet'
  // default value is 'overFullScreen'
  iOSModalPresentationStyle: 'pageSheet',
};

ImageSelector.launchPicker(options, (error, response) => {
  if (error) {
    if (error.code === ImageSelectorErrorType.CAMERA_PERMISSION_DENIED) {
      console.error('camera permission denied');
    }
    return;
  }
  if (response) {
    if (response.didCancel) {
      console.log('USER CANCELED');
      return;
    }
    setResponse(response);
  }
});
```

## Example

```sh
$ yarn bootstrap
$ cd example
$ yarn start
$ yarn ios
$ yarn android
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

## TODO

1. remove Documentdirectory photo file (iOS only) when module or react component will be unmounted.
2. iOS status bar color (Dark Mode).
3. code refactoring (Android).
