import * as React from 'react';
import { StyleSheet, Button, SafeAreaView, Image, Text } from 'react-native';
import ImageSelector, {
  ImageSelectorCallbackResponse,
  ImageSelectorErrorType,
  ImageSelectorOptions,
} from 'react-native-image-selector';

const options: ImageSelectorOptions = {
  // import Options
  storageOptions: {
    skipBackup: true,
    path: 'hello',
  },
  permissionDenied: {
    title: '권한 설정',
    text: "이 기능을 이용하시려면 권한을 '허용'으로 변경해주세요.",
    reTryTitle: '변경하러가기',
    okTitle: '닫기',
  },
  title: '사진 선택',
  cancelButtonTitle: '취소',
  takePhotoButtonTitle: '사진 촬영',
  chooseFromLibraryButtonTitle: '앨범에서 가져오기',
};

export default function App() {
  const [
    response,
    setResponse,
  ] = React.useState<ImageSelectorCallbackResponse | null>(null);
  const handlePhotos = async () => {
    try {
      ImageSelector.launchPicker(options, (error, response) => {
        if (error) {
          console.log(error);
          if (error.code === ImageSelectorErrorType.CAMERA_PERMISSION_DENIED) {
            console.error('camera permission denied');
          }
          return;
        }
        if (response) {
          if (response.didCancel) {
            console.log('USER CANCELLED');
            return;
          }
          setResponse(response);
        }
      });
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <Button title="GET USER PHOTOS" onPress={handlePhotos} />
      {response !== null && (
        <>
          <Text style={styles.textStyle}>Base64 Image</Text>
          <Image
            style={styles.imageStyle}
            source={{
              uri: `data:image/png;base64,${response.data}`,
            }}
          />
          <Text style={styles.textStyle}>URI Image</Text>
          <Image
            style={styles.imageStyle}
            source={{
              uri: response.uri,
            }}
          />
          <Text style={styles.textStyle}>path: {response.path}</Text>
          <Text style={styles.textStyle}>fileName: {response.fileName}</Text>
          <Text style={styles.textStyle}>fileSize: {response.fileSize}</Text>
          <Text style={styles.textStyle}>fileType: {response.type}</Text>
        </>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  textStyle: {
    marginTop: 15,
  },
  imageStyle: {
    width: 150,
    height: 150,
    borderWidth: 1,
    borderColor: '#ababab',
  },
});
