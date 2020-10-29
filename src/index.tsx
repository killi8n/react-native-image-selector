import { NativeModules } from 'react-native';

type ImageSelectorType = {
  multiply(a: number, b: number): Promise<number>;
};

const { ImageSelector } = NativeModules;

export default ImageSelector as ImageSelectorType;
