import { NativeModules } from 'react-native';

export interface ImageSelectorCallbackResponse {
  fileSize: number;
  fileName: string;
  type: string;
  uri: string;
  data: string;
  path: string;
  didCancel?: boolean;
}

export interface ImageSelectorOptions {
  title?: string;
  cancelButtonTitle?: string;
  takePhotoButtonTitle?: string;
  chooseFromLibraryButtonTitle?: string;
  storageOptions?: {
    skipBackup?: boolean;
    path?: string;
  };
  permissionDenied?: {
    title?: string;
    text?: string;
    reTryTitle?: string;
    okTitle?: string;
  };
  // iOS only
  iOSGridNumber?: 3 | 4;
  // iOS only, should be upper than iOS 15.0 for 'pageSheet'
  iOSModalPresentationStyle?: 'pageSheet' | 'overFullScreen';
}

interface ImageSelectorError {
  code: number;
  message: string;
}

export enum ImageSelectorErrorType {
  CAMERA_PERMISSION_DENIED = 100,
  LIBRARY_PERMISSION_DENIED = 101,
  SIMULATOR_ERROR = 102,
  SOURCE_TYPE_MISMATCH = 103,
  FILE_CREATE_ERROR = 104,
  NOT_VALID_PATH = 105,
  FAIL_TO_PICK_IMAGE = 106,
}

type ImageSelectorType = {
  launchPicker: (
    options: ImageSelectorOptions,
    callback: (
      error: ImageSelectorError,
      response?: ImageSelectorCallbackResponse
    ) => void
  ) => void;
};

const { ImageSelector } = NativeModules;

export default ImageSelector as ImageSelectorType;
