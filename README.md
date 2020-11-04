# react-native-image-selector

image picker native module

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
    <false/> // you can turn on this to <true />
    <key>NSCameraUsageDescription</key>
    <string>카메라 권한을 얻겠습니다.</string>
    <key>NSLocationWhenInUseUsageDescription</key>
    <string></string>
    <key>NSPhotoLibraryUsageDescription</key>
    <string>사진을 가져오겠습니다.</string>
```

`android/AndroidManifest.xml`

```xml
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.CAMERA" />
```

## Usage

```js
import ImageSelector from 'react-native-image-selector';

// ...
ImageSelector.launchPicker((error, response) => {
  if (error) {
    // TODO: ERROR EXCEPTION
    return;
  }
  // TODO: handle response
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
