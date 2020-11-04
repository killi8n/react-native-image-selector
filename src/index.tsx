import { NativeModules } from 'react-native';

export interface ImageSelectorCallbackResponse {
  fileSize: number;
  fileName: string;
  type: string;
  uri: string;
  data: string;
  path: string;
}

type ImageSelectorType = {
  launchPicker: (
    callback: (
      error: { error: string },
      response: ImageSelectorCallbackResponse
    ) => void
  ) => void;
};

const { ImageSelector } = NativeModules;

export default ImageSelector as ImageSelectorType;
