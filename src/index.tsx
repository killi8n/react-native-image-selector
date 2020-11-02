import { NativeModules } from 'react-native';

export interface Photo {
  fileSize: number
  fileName: string
  type: string
  uri: string
  data: string
}

type ImageSelectorType = {
  getPhotos: (limit: number) => Promise<Photo[]>
  initializePhotos: () => Promise<void>
};

const { ImageSelector } = NativeModules;

export default ImageSelector as ImageSelectorType;
