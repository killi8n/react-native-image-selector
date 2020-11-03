import { NativeModules } from 'react-native';

type ImageSelectorType = {
  launchPicker: (
    callback: (
      error: { error: string },
      response: {
        fileSize: number;
        fileName: string;
        type: string;
        uri: string;
      }
    ) => void
  ) => void;
};

const { ImageSelector } = NativeModules;

export default ImageSelector as ImageSelectorType;
