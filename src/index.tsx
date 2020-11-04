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
}

type ImageSelectorType = {
  launchPicker: (
    options: ImageSelectorOptions,
    callback: (
      error: { error: string },
      response?: ImageSelectorCallbackResponse
    ) => void
  ) => void;
};

const { ImageSelector } = NativeModules;

export default ImageSelector as ImageSelectorType;
